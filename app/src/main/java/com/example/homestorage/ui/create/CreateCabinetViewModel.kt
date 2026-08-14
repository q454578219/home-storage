package com.example.homestorage.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.db.CategoryEntity
import com.example.homestorage.data.repo.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 新建柜子 ViewModel：名称/分类/封面输入，保存入库
 */
class CreateCabinetViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 柜子名称 */
    val name = MutableStateFlow("")

    /** 选中分类 id（null = 未分类） */
    val categoryId = MutableStateFlow<Long?>(null)

    /** 可选分类列表 */
    val categories: StateFlow<List<CategoryEntity>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 保存柜子
     *
     * @param coverUri 封面图 Uri（可为 null 不设封面）
     * @param onSaved 保存成功回调（参数为新柜子 id）
     */
    fun save(coverUri: Uri?, onSaved: (Long) -> Unit) {
        val trimmed = name.value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = repo.addCabinet(trimmed, categoryId.value, coverUri)
            onSaved(id)
        }
    }
}
