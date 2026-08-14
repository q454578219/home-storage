package com.example.homestorage.ui.spot

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.ai.ImageRecognizer
import com.example.homestorage.data.db.ItemEntity
import com.example.homestorage.data.db.SpotEntity
import com.example.homestorage.data.repo.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 点位详情 ViewModel：物品增删/编辑、图片智能识别
 */
class SpotDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 是否正在智能识别图片 */
    private val _recognizing = MutableStateFlow(false)
    val recognizing: StateFlow<Boolean> = _recognizing

    /** 最近一次识别结果（物品关键词，逗号分隔） */
    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText: StateFlow<String?> = _recognizedText

    /** 拆分后的标签（识别结果去掉主名称后的其余关键词，逗号分隔） */
    private val _recognizedTags = MutableStateFlow<String?>(null)
    val recognizedTags: StateFlow<String?> = _recognizedTags

    /** 是否识别失败（用于 UI 提示） */
    private val _recognizeFailed = MutableStateFlow(false)
    val recognizeFailed: StateFlow<Boolean> = _recognizeFailed

    /** 批量录入进度（当前第几张, 总数；null = 未在批量录入） */
    private val _batchProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val batchProgress: StateFlow<Pair<Int, Int>?> = _batchProgress

    /** 点位 id（导航参数注入） */
    private val spotIdFlow = MutableStateFlow<Long?>(null)

    /** 点位所属柜子 id（用于同柜其他点位查询） */
    private val cabinetIdFlow = MutableStateFlow<Long?>(null)

    /** 物品列表 */
    val items: StateFlow<List<ItemEntity>> =
        spotIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeItems(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 同柜其他点位（物品"移动到"用，排除自身） */
    val otherSpots: StateFlow<List<SpotEntity>> =
        cabinetIdFlow.flatMapLatest { cabinetId ->
            if (cabinetId == null) flowOf(emptyList())
            else repo.observeSpots(cabinetId).map { spots ->
                spots.filter { it.id != spotIdFlow.value }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 加载点位
     *
     * @param spotId 点位 id
     */
    fun load(spotId: Long) {
        if (spotIdFlow.value != spotId) {
            spotIdFlow.value = spotId
            viewModelScope.launch {
                cabinetIdFlow.value = repo.getSpot(spotId)?.cabinetId
            }
        }
    }

    /**
     * 将物品移动到同柜其他点位
     *
     * @param itemId 物品 id
     * @param newSpotId 目标点位 id
     */
    fun moveItem(itemId: Long, newSpotId: Long) {
        viewModelScope.launch {
            val item = repo.getItem(itemId) ?: return@launch
            repo.updateItem(item.copy(spotId = newSpotId))
        }
    }

    /**
     * 智能识别图片中的物品关键词（GLM-4V-Flash）
     *
     * 识别结果按逗号/顿号拆分：第一个词作为主名称，其余作为标签（tags）。
     *
     * @param imageUri 图片 Uri
     */
    fun recognizeImage(imageUri: Uri) {
        if (_recognizing.value) return
        viewModelScope.launch {
            _recognizing.value = true
            _recognizedText.value = null
            _recognizedTags.value = null
            _recognizeFailed.value = false
            val text = ImageRecognizer.recognize(getApplication(), imageUri)
            if (text != null) {
                val parts = splitKeywords(text)
                if (parts.isEmpty()) {
                    _recognizeFailed.value = true
                } else {
                    _recognizedText.value = text
                    _recognizedTags.value = parts.drop(1).take(5).distinct().joinToString(",")
                }
            } else {
                _recognizeFailed.value = true
            }
            _recognizing.value = false
        }
    }

    /**
     * 批量录入：多张照片依次识别并保存，不弹确认框（识别失败命名为"未命名"）
     *
     * @param imageUris 待处理照片 Uri 列表
     */
    fun addItemsFromImages(imageUris: List<Uri>) {
        val spotId = spotIdFlow.value ?: return
        if (imageUris.isEmpty() || _batchProgress.value != null) return
        viewModelScope.launch {
            _batchProgress.value = 1 to imageUris.size
            imageUris.forEachIndexed { index, uri ->
                try {
                    val parts = ImageRecognizer.recognize(getApplication(), uri)
                        ?.let { splitKeywords(it) } ?: emptyList()
                    val name = parts.firstOrNull() ?: "未命名"
                    val tags = parts.drop(1).take(5).distinct().joinToString(",")
                    repo.addItem(spotId, name, tags, 1, null, uri)
                } catch (e: Exception) {
                    // 单张失败跳过，继续下一张
                }
                _batchProgress.value = (index + 2).coerceAtMost(imageUris.size) to imageUris.size
            }
            _batchProgress.value = null
        }
    }

    /** 识别文本按逗号/顿号/分号拆分 */
    private fun splitKeywords(text: String): List<String> =
        text.split(",", "，", "、", "。", "；", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * 清除识别结果状态（弹窗关闭/切换图片时调用）
     */
    fun clearRecognition() {
        _recognizing.value = false
        _recognizedText.value = null
        _recognizedTags.value = null
        _recognizeFailed.value = false
    }

    /**
     * 添加物品
     *
     * @param imageUri 照片 Uri
     * @param name 物品名称（必填）
     * @param tags AI 识别关键词（逗号分隔，辅助搜索，可选）
     * @param quantity 数量（默认 1）
     * @param note 备注（可选）
     */
    fun addItem(imageUri: Uri, name: String, tags: String?, quantity: Int, note: String?) {
        val spotId = spotIdFlow.value ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.addItem(
                spotId, trimmed, tags, quantity.coerceAtLeast(1), note?.trim()?.ifBlank { null }, imageUri
            )
        }
    }

    /**
     * 更新物品（名称/数量/备注）
     *
     * @param item 已修改字段的实体
     */
    fun updateItem(item: ItemEntity) {
        viewModelScope.launch {
            repo.updateItem(
                item.copy(
                    name = item.name.trim(),
                    quantity = item.quantity.coerceAtLeast(1),
                    note = item.note?.trim()?.ifBlank { null }
                )
            )
        }
    }

    /**
     * 删除物品（连带删除照片文件）
     *
     * @param itemId 物品 id
     */
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repo.deleteItem(itemId)
        }
    }
}
