package com.example.homestorage.ui.floorplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.db.CabinetOnFloorPlan
import com.example.homestorage.data.db.CabinetWithCategory
import com.example.homestorage.data.db.FloorPlanEntity
import com.example.homestorage.data.repo.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 户型图详情 ViewModel：图片 + 柜子标记层 + 挂载/移动交互
 */
class FloorPlanDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 当前户型图 id（加载入口） */
    private val planId = MutableStateFlow<Long?>(null)

    /** 当前户型图（一次性加载） */
    val floorPlan: StateFlow<FloorPlanEntity?> = planId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repo.getFloorPlanFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 户型图上的柜子标记列表 */
    val cabinets: StateFlow<List<CabinetOnFloorPlan>> = planId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeCabinetsByFloorPlan(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 未挂载户型图的柜子（挂载选择列表用） */
    val unattachedCabinets: StateFlow<List<CabinetWithCategory>> =
        repo.observeUnattachedCabinets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 加载户型图数据
     *
     * @param id 户型图 id
     */
    fun load(id: Long) {
        planId.value = id
    }

    /**
     * 挂载已有柜子到户型图位置
     *
     * @param cabinetId 柜子 id
     * @param x 归一化横坐标 0~1
     * @param y 归一化纵坐标 0~1
     */
    fun attachCabinet(cabinetId: Long, x: Float, y: Float) {
        val id = planId.value ?: return
        viewModelScope.launch { repo.attachCabinet(cabinetId, id, x, y) }
    }

    /**
     * 拖动更新柜子标记位置
     *
     * @param cabinetId 柜子 id
     * @param x 归一化横坐标 0~1
     * @param y 归一化纵坐标 0~1
     */
    fun moveCabinet(cabinetId: Long, x: Float, y: Float) {
        val id = planId.value ?: return
        viewModelScope.launch { repo.attachCabinet(cabinetId, id, x, y) }
    }
}