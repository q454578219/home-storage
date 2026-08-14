package com.example.homestorage.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.homestorage.data.db.CabinetEntity
import com.example.homestorage.data.db.CabinetWithCategory
import com.example.homestorage.data.db.CategoryWithCount
import com.example.homestorage.data.repo.HomeRepository
import com.example.homestorage.data.repo.SearchResult
import com.example.homestorage.data.db.SearchItemHit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel：柜子列表、分类筛选、柜子管理操作（重命名/改分类/换封面/删除）
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = HomeRepository.get(application)

    /** 当前选中的分类 id（null = 全部） */
    val selectedCategoryId = MutableStateFlow<Long?>(null)

    /** 全部分类（含物品数，UI 显示"客厅 (12)"） */
    val categories: StateFlow<List<CategoryWithCount>> = repo.observeCategoriesWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 柜子列表（随选中分类变化） */
    val cabinets: StateFlow<List<CabinetWithCategory>> =
        combine(repo.observeCabinets(), selectedCategoryId) { all, categoryId ->
            if (categoryId == null) all else all.filter { it.categoryId == categoryId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 切换分类筛选
     *
     * @param categoryId 分类 id，null 表示全部
     */
    fun selectCategory(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    /** 搜索关键词（空 = 未搜索） */
    val searchKeyword = MutableStateFlow("")

    /** 最近添加的物品（首页顶部横向栏） */
    val recentItems: StateFlow<List<SearchItemHit>> =
        repo.observeRecentItems(10)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 搜索结果（随关键词变化） */
    val searchResult: StateFlow<SearchResult> =
        searchKeyword
            .flatMapLatest { repo.search(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResult(emptyList(), emptyList()))

    /** 更新搜索关键词 */
    fun onSearchChange(keyword: String) {
        searchKeyword.value = keyword
    }

    /**
     * 重命名柜子
     *
     * @param cabinet 目标柜子
     * @param newName 新名称
     */
    fun renameCabinet(cabinet: CabinetWithCategory, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repo.updateCabinet(cabinet.toEntity().copy(name = newName.trim()))
        }
    }

    /**
     * 修改柜子分类
     *
     * @param cabinet 目标柜子
     * @param newCategoryId 新分类 id（null = 未分类）
     */
    fun changeCabinetCategory(cabinet: CabinetWithCategory, newCategoryId: Long?) {
        viewModelScope.launch {
            repo.updateCabinet(cabinet.toEntity().copy(categoryId = newCategoryId))
        }
    }

    /**
     * 更换柜子封面
     *
     * @param cabinet 目标柜子
     * @param newUri 新封面图片 Uri
     */
    fun replaceCabinetCover(cabinet: CabinetWithCategory, newUri: Uri) {
        viewModelScope.launch {
            repo.replaceCabinetCover(cabinet.id, newUri)
        }
    }

    /**
     * 删除柜子（移入回收站，可恢复）
     *
     * @param cabinet 目标柜子
     */
    fun deleteCabinet(cabinet: CabinetWithCategory) {
        viewModelScope.launch {
            repo.deleteCabinet(cabinet.id)
        }
    }

    /** 回收站柜子列表（软删除的柜子） */
    val recycleBin: StateFlow<List<CabinetWithCategory>> =
        repo.observeDeletedCabinets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 从回收站恢复柜子
     *
     * @param cabinetId 柜子 id
     */
    fun restoreCabinet(cabinetId: Long) {
        viewModelScope.launch {
            repo.restoreCabinet(cabinetId)
        }
    }

    /**
     * 彻底删除柜子（连带点位物品图片，不可恢复）
     *
     * @param cabinetId 柜子 id
     */
    fun purgeCabinet(cabinetId: Long) {
        viewModelScope.launch {
            repo.purgeCabinet(cabinetId)
        }
    }

    /** 将投影对象转回实体（更新用） */
    private fun CabinetWithCategory.toEntity() = CabinetEntity(
        id = id, name = name, coverImagePath = coverImagePath,
        categoryId = categoryId, createdAt = createdAt
    )
}
