package com.example.homestorage.ui.cabinet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.db.CabinetWithCategory
import com.example.homestorage.data.db.SpotEntity
import com.example.homestorage.data.db.SpotWithPreview
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
 * 柜子详情 ViewModel：点位增删/移动
 */
class CabinetDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 柜子 id（导航参数注入） */
    private val cabinetIdFlow = MutableStateFlow<Long?>(null)

    /** 柜子信息（含分类名） */
    val cabinet: StateFlow<CabinetWithCategory?> =
        cabinetIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repo.observeCabinets().map { list -> list.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 点位列表（含首件物品缩略图） */
    val spots: StateFlow<List<SpotWithPreview>> =
        cabinetIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeSpotsWithPreview(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 新增点位成功后待跳转的 spotId（一次性事件） */
    private val _navigateToSpot = MutableStateFlow<Long?>(null)
    val navigateToSpot: StateFlow<Long?> = _navigateToSpot

    /**
     * 加载柜子
     *
     * @param cabinetId 柜子 id
     */
    fun load(cabinetId: Long) {
        if (cabinetIdFlow.value != cabinetId) {
            cabinetIdFlow.value = cabinetId
        }
    }

    /**
     * 添加点位（成功后发出跳转事件，直接进入点位详情）
     *
     * @param x 归一化横坐标 0~1
     * @param y 归一化纵坐标 0~1
     * @param title 标题（可选）
     */
    fun addSpot(x: Float, y: Float, title: String?) {
        val id = cabinetIdFlow.value ?: return
        viewModelScope.launch {
            val spotId = repo.addSpot(id, x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), title)
            _navigateToSpot.value = spotId
        }
    }

    /**
     * 消费跳转事件（导航完成后调用）
     */
    fun consumeNavigate() {
        _navigateToSpot.value = null
    }

    /**
     * 移动点位
     *
     * @param spot 目标点位
     * @param x 新归一化横坐标
     * @param y 新归一化纵坐标
     */
    fun moveSpot(spot: SpotWithPreview, x: Float, y: Float) {
        viewModelScope.launch {
            repo.updateSpot(
                SpotEntity(
                    id = spot.id,
                    cabinetId = spot.cabinetId,
                    x = x.coerceIn(0f, 1f),
                    y = y.coerceIn(0f, 1f),
                    title = spot.title
                )
            )
        }
    }

    /**
     * 重命名点位标题（标题为空则清除）
     *
     * @param spot 目标点位
     * @param title 新标题（可为 null 表示清除）
     */
    fun renameSpot(spot: SpotWithPreview, title: String?) {
        viewModelScope.launch {
            val current = spots.value.find { it.id == spot.id } ?: return@launch
            repo.updateSpot(
                SpotEntity(
                    id = current.id,
                    cabinetId = current.cabinetId,
                    x = current.x,
                    y = current.y,
                    title = title
                )
            )
        }
    }

    /**
     * 删除点位（级联删除物品与图片）
     *
     * @param spotId 点位 id
     */
    fun deleteSpot(spotId: Long) {
        viewModelScope.launch {
            repo.deleteSpot(spotId)
        }
    }
}
