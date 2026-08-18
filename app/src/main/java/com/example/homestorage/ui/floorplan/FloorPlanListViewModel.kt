package com.example.homestorage.ui.floorplan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.db.FloorPlanEntity
import com.example.homestorage.data.repo.HomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 户型图列表 ViewModel：列表/新增/重命名/删除
 */
class FloorPlanListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 户型图列表 */
    val floorPlans: StateFlow<List<FloorPlanEntity>> = repo.observeFloorPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 新增户型图
     *
     * @param name 名称
     * @param imageUri 图片 Uri
     * @param onSaved 保存成功回调（参数为新户型图 id）
     */
    fun addFloorPlan(name: String, imageUri: Uri, onSaved: (Long) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = repo.addFloorPlan(trimmed, imageUri)
            if (id > 0) onSaved(id)
        }
    }

    /**
     * 重命名户型图
     *
     * @param planId 户型图 id
     * @param newName 新名称
     */
    fun renameFloorPlan(planId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repo.renameFloorPlan(planId, trimmed) }
    }

    /**
     * 删除户型图（柜子保留，仅解除挂载）
     *
     * @param planId 户型图 id
     */
    fun deleteFloorPlan(planId: Long) {
        viewModelScope.launch { repo.deleteFloorPlan(planId) }
    }
}