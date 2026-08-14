package com.example.homestorage.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.homestorage.data.db.CabinetEntity
import com.example.homestorage.data.db.CabinetWithCategory
import com.example.homestorage.data.db.CategoryEntity
import com.example.homestorage.data.db.CategoryWithCount
import com.example.homestorage.data.db.HomeStorageDatabase
import com.example.homestorage.data.db.ItemEntity
import com.example.homestorage.data.db.SearchCabinetHit
import com.example.homestorage.data.db.SearchItemHit
import com.example.homestorage.data.db.SpotEntity
import com.example.homestorage.data.db.SpotWithPreview
import com.example.homestorage.data.image.ImageStore
import kotlinx.coroutines.flow.Flow

/** 搜索结果（物品命中 + 柜子命中） */
data class SearchResult(
    val items: List<SearchItemHit>,
    val cabinets: List<SearchCabinetHit>,
)

/**
 * 数据仓库：统一封装数据库与图片存储，对外提供业务方法（含级联删除逻辑）
 */
class HomeRepository private constructor(private val db: HomeStorageDatabase, private val imageStore: ImageStore) {

    /** 柜子封面最大尺寸 */
    private val MAX_CABINET_SIZE = 1600

    // ---------- 分类 ----------

    /** 观察全部分类（预置在前） */
    fun observeCategories(): Flow<List<CategoryEntity>> = db.categoryDao().observeAll()

    /** 观察全部分类（含物品数，首页筛选 chips 用） */
    fun observeCategoriesWithCount(): Flow<List<CategoryWithCount>> = db.categoryDao().observeAllWithCount()

    /** 观察最近添加的物品（首页横向栏用） */
    fun observeRecentItems(limit: Int): Flow<List<SearchItemHit>> = db.itemDao().observeRecentItems(limit)

    /**
     * 新增自定义分类
     *
     * @return 新分类 id
     */
    suspend fun addCategory(name: String): Long = db.categoryDao().insert(CategoryEntity(name = name, isPreset = false))

    /**
     * 删除自定义分类（预置分类不可删）
     *
     * @return 是否删除成功（预置返回 false）
     */
    suspend fun deleteCategory(category: CategoryEntity): Boolean {
        if (category.isPreset) return false
        db.categoryDao().insert(category)
        return true
    }

    // ---------- 柜子 ----------

    /** 观察全部柜子（含分类名） */
    fun observeCabinets(): Flow<List<CabinetWithCategory>> = db.cabinetDao().observeAll()

    /** 按分类观察柜子（null 表示全部分类） */
    fun observeCabinetsByCategory(categoryId: Long?): Flow<List<CabinetWithCategory>> =
        db.cabinetDao().observeByCategory(categoryId)

    /**
     * 新增柜子（可带封面图）
     *
     * @return 新柜子 id
     */
    suspend fun addCabinet(name: String, categoryId: Long?, coverUri: Uri?): Long {
        val coverPath = coverUri?.let { imageStore.saveFromUri(it, MAX_CABINET_SIZE) }
        return db.cabinetDao().insert(
            CabinetEntity(name = name, coverImagePath = coverPath, categoryId = categoryId)
        )
    }

    /**
     * 更新柜子（名称/分类）
     *
     * @param cabinet 已更新字段的实体（id 必须存在）
     */
    suspend fun updateCabinet(cabinet: CabinetEntity) = db.cabinetDao().update(cabinet)

    /**
     * 更换柜子封面图（旧图自动删除）
     *
     * @param cabinetId 柜子 id
     * @param newUri 新图片 Uri
     */
    suspend fun replaceCabinetCover(cabinetId: Long, newUri: Uri) {
        val cabinet = db.cabinetDao().getById(cabinetId) ?: return
        val newPath = imageStore.saveFromUri(newUri, MAX_CABINET_SIZE) ?: return
        val oldPath = cabinet.coverImagePath
        db.cabinetDao().update(cabinet.copy(coverImagePath = newPath))
        imageStore.delete(oldPath)
    }

    /**
     * 删除柜子：移入回收站（软删除，可恢复）
     *
     * @param cabinetId 柜子 id
     */
    suspend fun deleteCabinet(cabinetId: Long) {
        val cabinet = db.cabinetDao().getById(cabinetId) ?: return
        db.cabinetDao().update(cabinet.copy(isDeleted = true))
    }

    /** 观察回收站柜子 */
    fun observeDeletedCabinets(): Flow<List<CabinetWithCategory>> = db.cabinetDao().observeDeleted()

    /**
     * 从回收站恢复柜子
     *
     * @param cabinetId 柜子 id
     */
    suspend fun restoreCabinet(cabinetId: Long) {
        val cabinet = db.cabinetDao().getById(cabinetId) ?: return
        db.cabinetDao().update(cabinet.copy(isDeleted = false))
    }

    /**
     * 彻底删除柜子：级联删除点位、物品及全部图片文件（不可恢复）
     *
     * @param cabinetId 柜子 id
     */
    suspend fun purgeCabinet(cabinetId: Long) {
        val cabinet = db.cabinetDao().getById(cabinetId) ?: return
        db.withTransaction {
            val items = db.itemDao().getByCabinet(cabinetId)
            db.itemDao().deleteSpotsByCabinet(cabinetId)
            db.cabinetDao().delete(cabinet)
            items.forEach { imageStore.delete(it.imagePath) }
        }
        imageStore.delete(cabinet.coverImagePath)
    }

    // ---------- 点位 ----------

    /** 观察某柜子的全部点位 */
    fun observeSpots(cabinetId: Long): Flow<List<SpotEntity>> = db.spotDao().observeByCabinet(cabinetId)

    /** 按 id 查询点位 */
    suspend fun getSpot(id: Long): SpotEntity? = db.spotDao().getById(id)

    /** 观察柜子点位（含首件物品缩略图，柜子详情角标用） */
    fun observeSpotsWithPreview(cabinetId: Long): Flow<List<SpotWithPreview>> =
        db.spotDao().observeByCabinetWithPreview(cabinetId)

    /**
     * 新增点位
     *
     * @param cabinetId 柜子 id
     * @param x 归一化横坐标 0~1
     * @param y 归一化纵坐标 0~1
     * @param title 标题（可选）
     * @return 新点位 id
     */
    suspend fun addSpot(cabinetId: Long, x: Float, y: Float, title: String?): Long =
        db.spotDao().insert(SpotEntity(cabinetId = cabinetId, x = x, y = y, title = title))

    /** 更新点位（移动坐标/改标题） */
    suspend fun updateSpot(spot: SpotEntity) = db.spotDao().update(spot)

    /**
     * 删除点位：级联删除其物品及图片
     *
     * @param spotId 点位 id
     */
    suspend fun deleteSpot(spotId: Long) {
        val spot = db.spotDao().getById(spotId) ?: return
        db.withTransaction {
            val items = db.itemDao().getBySpot(spotId)
            db.itemDao().deleteBySpot(spotId)
            db.spotDao().delete(spot)
            items.forEach { imageStore.delete(it.imagePath) }
        }
    }

    // ---------- 物品 ----------

    /** 观察某点位的全部物品 */
    fun observeItems(spotId: Long): Flow<List<ItemEntity>> = db.itemDao().observeBySpot(spotId)

    /** 按 id 查询物品 */
    suspend fun getItem(id: Long): ItemEntity? = db.itemDao().getById(id)

    /**
     * 新增物品（带照片 + 名称）
     *
     * @param spotId 点位 id
     * @param name 物品名称（搜索索引）
     * @param tags AI 识别关键词（逗号分隔，辅助搜索，可选）
     * @param quantity 数量（默认 1）
     * @param note 备注（可选）
     * @param imageUri 照片 Uri（可为空，但空图不建议）
     * @return 新物品 id
     */
    suspend fun addItem(
        spotId: Long, name: String, tags: String?, quantity: Int, note: String?, imageUri: Uri
    ): Long {
        val imagePath = imageStore.saveFromUri(imageUri)
        return db.itemDao().insert(
            ItemEntity(
                spotId = spotId, imagePath = imagePath, name = name, tags = tags,
                quantity = quantity, note = note
            )
        )
    }

    /** 更新物品（名称/备注） */
    suspend fun updateItem(item: ItemEntity) = db.itemDao().update(item)

    /**
     * 删除物品（连带删除照片文件）
     *
     * @param itemId 物品 id
     */
    suspend fun deleteItem(itemId: Long) {
        val item = db.itemDao().getById(itemId) ?: return
        db.itemDao().delete(item)
        imageStore.delete(item.imagePath)
    }

    // ---------- 搜索 ----------

    /**
     * 搜索：物品名/柜子名/分类名模糊匹配
     *
     * @param keyword 关键词（空串返回空结果）
     */
    fun search(keyword: String): Flow<SearchResult> {
        if (keyword.isBlank()) return kotlinx.coroutines.flow.flowOf(SearchResult(emptyList(), emptyList()))
        val kw = keyword.trim()
        return kotlinx.coroutines.flow.combine(
            db.itemDao().searchItems(kw),
            db.itemDao().searchCabinets(kw)
        ) { items, cabinets -> SearchResult(items, cabinets) }
    }

    companion object {
        /**
         * 获取仓库单例
         *
         * @param context 应用上下文
         */
        fun get(context: Context): HomeRepository {
            val app = context.applicationContext
            return InstanceHolder.instance ?: synchronized(this) {
                InstanceHolder.instance ?: HomeRepository(
                    HomeStorageDatabase.get(app),
                    ImageStore(app)
                ).also { InstanceHolder.instance = it }
            }
        }

        /** 实例持有者 */
        private object InstanceHolder {
            @Volatile
            var instance: HomeRepository? = null
        }

        /**
         * 关闭并重置仓库单例（备份恢复后调用，旧实例引用已关闭的数据库）
         */
        fun resetForRestore() {
            synchronized(this) {
                InstanceHolder.instance?.let { it.db.close() }
                InstanceHolder.instance = null
            }
        }
    }
}
