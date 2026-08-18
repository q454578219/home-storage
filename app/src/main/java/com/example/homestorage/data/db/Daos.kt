package com.example.homestorage.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 分类表 DAO */
@Dao
interface CategoryDao {
    /** 插入分类，返回自增 id */
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    /** 查询全部分类（预置在前） */
    @Query("SELECT * FROM categories ORDER BY isPreset DESC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** 查询全部分类（含物品数统计，首页筛选 chips 用） */
    @Query(
        """
        SELECT categories.*,
               (SELECT COUNT(*) FROM items
                JOIN spots ON items.spotId = spots.id
                JOIN cabinets ON spots.cabinetId = cabinets.id
                WHERE cabinets.categoryId = categories.id
                  AND cabinets.isDeleted = 0) AS itemCount
        FROM categories ORDER BY isPreset DESC, id ASC
        """
    )
    fun observeAllWithCount(): Flow<List<CategoryWithCount>>

    /** 按 id 查询分类 */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}

/** 柜子表 DAO */
@Dao
interface CabinetDao {
    /** 插入柜子，返回自增 id */
    @Insert
    suspend fun insert(cabinet: CabinetEntity): Long

    /** 更新柜子 */
    @Update
    suspend fun update(cabinet: CabinetEntity)

    /** 删除柜子 */
    @Delete
    suspend fun delete(cabinet: CabinetEntity)

    /** 按 id 查询柜子 */
    @Query("SELECT * FROM cabinets WHERE id = :id")
    suspend fun getById(id: Long): CabinetEntity?

    /** 查询全部柜子（含分类名、物品数），用于首页列表 */
    @Query(
        """
        SELECT cabinets.*, categories.name AS categoryName,
               (SELECT COUNT(*) FROM items
                JOIN spots ON items.spotId = spots.id
                WHERE spots.cabinetId = cabinets.id) AS itemCount
        FROM cabinets LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE cabinets.isDeleted = 0
        ORDER BY cabinets.createdAt DESC
        """
    )
    fun observeAll(): Flow<List<CabinetWithCategory>>

    /** 按分类筛选柜子（含分类名、物品数） */
    @Query(
        """
        SELECT cabinets.*, categories.name AS categoryName,
               (SELECT COUNT(*) FROM items
                JOIN spots ON items.spotId = spots.id
                WHERE spots.cabinetId = cabinets.id) AS itemCount
        FROM cabinets LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE (categories.id = :categoryId OR (:categoryId IS NULL))
          AND cabinets.isDeleted = 0
        ORDER BY cabinets.createdAt DESC
        """
    )
    fun observeByCategory(categoryId: Long?): Flow<List<CabinetWithCategory>>

    /** 查询回收站中的柜子（软删除） */
    @Query(
        """
        SELECT cabinets.*, categories.name AS categoryName,
               (SELECT COUNT(*) FROM items
                JOIN spots ON items.spotId = spots.id
                WHERE spots.cabinetId = cabinets.id) AS itemCount
        FROM cabinets LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE cabinets.isDeleted = 1
        ORDER BY cabinets.createdAt DESC
        """
    )
    fun observeDeleted(): Flow<List<CabinetWithCategory>>

    /** 查询某户型图上的全部柜子（含封面缩略图，户型图详情标记用） */
    @Query(
        """
        SELECT cabinets.id AS id, cabinets.name AS name,
               cabinets.coverImagePath AS coverImagePath,
               cabinets.x AS x, cabinets.y AS y
        FROM cabinets
        WHERE cabinets.floorPlanId = :planId
          AND cabinets.isDeleted = 0
        ORDER BY cabinets.createdAt ASC
        """
    )
    fun observeByFloorPlan(planId: Long): Flow<List<CabinetOnFloorPlan>>

    /**
     * 更新柜子的户型图挂载位置（挂载/移动标记）
     *
     * @param cabinetId 柜子 id
     * @param planId 户型图 id
     * @param x 归一化横坐标 0~1
     * @param y 归一化纵坐标 0~1
     */
    @Query("UPDATE cabinets SET floorPlanId = :planId, x = :x, y = :y WHERE id = :cabinetId")
    suspend fun attachToFloorPlan(cabinetId: Long, planId: Long, x: Float, y: Float)

    /** 解除柜子的户型图挂载（删除户型图时调用，柜子本身保留） */
    @Query("UPDATE cabinets SET floorPlanId = NULL WHERE floorPlanId = :planId")
    suspend fun clearFloorPlan(planId: Long)

    /** 查询未挂载户型图且未删除的柜子（挂载弹窗选择列表用） */
    @Query(
        """
        SELECT cabinets.*, categories.name AS categoryName,
               (SELECT COUNT(*) FROM items
                JOIN spots ON items.spotId = spots.id
                WHERE spots.cabinetId = cabinets.id) AS itemCount
        FROM cabinets LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE cabinets.floorPlanId IS NULL
          AND cabinets.isDeleted = 0
        ORDER BY cabinets.createdAt DESC
        """
    )
    fun observeUnattached(): Flow<List<CabinetWithCategory>>
}

/** 户型图上的柜子投影（标记渲染用） */
data class CabinetOnFloorPlan(
    val id: Long,
    val name: String,
    val coverImagePath: String?,
    val x: Float,
    val y: Float,
)

/** 户型图表 DAO */
@Dao
interface FloorPlanDao {
    /** 插入户型图，返回自增 id */
    @Insert
    suspend fun insert(floorPlan: FloorPlanEntity): Long

    /** 更新户型图（重命名） */
    @Update
    suspend fun update(floorPlan: FloorPlanEntity)

    /** 删除户型图 */
    @Delete
    suspend fun delete(floorPlan: FloorPlanEntity)

    /** 观察全部户型图（列表页用） */
    @Query("SELECT * FROM floor_plans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FloorPlanEntity>>

    /** 观察单个户型图 */
    @Query("SELECT * FROM floor_plans WHERE id = :id")
    fun observeById(id: Long): Flow<FloorPlanEntity?>

    /** 按 id 查询户型图 */
    @Query("SELECT * FROM floor_plans WHERE id = :id")
    suspend fun getById(id: Long): FloorPlanEntity?
}

/** 柜子 + 分类名 + 物品数投影 */
data class CabinetWithCategory(
    val id: Long,
    val name: String,
    val coverImagePath: String?,
    val categoryId: Long?,
    val createdAt: Long,
    val categoryName: String?,
    val itemCount: Int = 0,
)

/** 分类 + 物品数投影（首页筛选 chips 统计用） */
data class CategoryWithCount(
    val id: Long,
    val name: String,
    val isPreset: Boolean,
    val itemCount: Int = 0,
)

/** 点位表 DAO */
@Dao
interface SpotDao {
    /** 插入点位，返回自增 id */
    @Insert
    suspend fun insert(spot: SpotEntity): Long

    /** 更新点位 */
    @Update
    suspend fun update(spot: SpotEntity)

    /** 删除点位 */
    @Delete
    suspend fun delete(spot: SpotEntity)

    /** 查询某柜子的全部点位 */
    @Query("SELECT * FROM spots WHERE cabinetId = :cabinetId ORDER BY createdAt ASC")
    fun observeByCabinet(cabinetId: Long): Flow<List<SpotEntity>>

    /** 查询某柜子的全部点位（含每点位首件物品缩略图，用于柜子详情角标） */
    @Query(
        """
        SELECT spots.*,
               (SELECT items.imagePath FROM items
                WHERE items.spotId = spots.id
                ORDER BY items.createdAt ASC LIMIT 1) AS firstItemImage
        FROM spots WHERE spots.cabinetId = :cabinetId
        ORDER BY spots.createdAt ASC
        """
    )
    fun observeByCabinetWithPreview(cabinetId: Long): Flow<List<SpotWithPreview>>

    /** 按 id 查询点位 */
    @Query("SELECT * FROM spots WHERE id = :id")
    suspend fun getById(id: Long): SpotEntity?
}

/** 点位 + 首件物品图片投影（柜子详情缩略图角标用） */
data class SpotWithPreview(
    val id: Long,
    val cabinetId: Long,
    val x: Float,
    val y: Float,
    val title: String?,
    val createdAt: Long,
    val firstItemImage: String?,
)

/** 物品表 DAO */
@Dao
interface ItemDao {
    /** 插入物品，返回自增 id */
    @Insert
    suspend fun insert(item: ItemEntity): Long

    /** 更新物品 */
    @Update
    suspend fun update(item: ItemEntity)

    /** 删除物品 */
    @Delete
    suspend fun delete(item: ItemEntity)

    /** 查询某点位的全部物品 */
    @Query("SELECT * FROM items WHERE spotId = :spotId ORDER BY createdAt ASC")
    fun observeBySpot(spotId: Long): Flow<List<ItemEntity>>

    /** 按 id 查询物品 */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    /** 查询某点位的全部物品（级联删除时取图片用） */
    @Query("SELECT * FROM items WHERE spotId = :spotId")
    suspend fun getBySpot(spotId: Long): List<ItemEntity>

    /** 按柜子查全部物品（级联删除时取图片用） */
    @Query(
        """
        SELECT items.* FROM items
        JOIN spots ON items.spotId = spots.id
        WHERE spots.cabinetId = :cabinetId
        """
    )
    suspend fun getByCabinet(cabinetId: Long): List<ItemEntity>

    /** 按点位删除物品 */
    @Query("DELETE FROM items WHERE spotId = :spotId")
    suspend fun deleteBySpot(spotId: Long)

    /** 按柜子删除点位 */
    @Query("DELETE FROM spots WHERE cabinetId = :cabinetId")
    suspend fun deleteSpotsByCabinet(cabinetId: Long)

    /** 搜索物品：物品名/柜子名/分类名 LIKE 匹配 */
    @Query(
        """
        SELECT items.id AS itemId, items.name AS itemName, items.imagePath AS itemImagePath,
               items.note AS itemNote,
               spots.id AS spotId, spots.x AS spotX, spots.y AS spotY,
               cabinets.id AS cabinetId, cabinets.name AS cabinetName,
               categories.name AS categoryName
        FROM items
        JOIN spots ON items.spotId = spots.id
        JOIN cabinets ON spots.cabinetId = cabinets.id
        LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE (items.name LIKE '%' || :kw || '%'
           OR items.tags LIKE '%' || :kw || '%'
           OR cabinets.name LIKE '%' || :kw || '%'
           OR categories.name LIKE '%' || :kw || '%')
          AND cabinets.isDeleted = 0
        ORDER BY items.createdAt DESC
        """
    )
    fun searchItems(kw: String): Flow<List<SearchItemHit>>

    /** 最近添加的物品（首页"最近添加"栏，跨柜子按时间倒序） */
    @Query(
        """
        SELECT items.id AS itemId, items.name AS itemName, items.imagePath AS itemImagePath,
               items.note AS itemNote,
               spots.id AS spotId, spots.x AS spotX, spots.y AS spotY,
               cabinets.id AS cabinetId, cabinets.name AS cabinetName,
               categories.name AS categoryName
        FROM items
        JOIN spots ON items.spotId = spots.id
        JOIN cabinets ON spots.cabinetId = cabinets.id
        LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE cabinets.isDeleted = 0
        ORDER BY items.createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentItems(limit: Int): Flow<List<SearchItemHit>>

    /** 搜索柜子：柜子名/分类名 LIKE 匹配 */
    @Query(
        """
        SELECT cabinets.id AS cabinetId, cabinets.name AS cabinetName,
               cabinets.coverImagePath AS coverImagePath,
               categories.name AS categoryName
        FROM cabinets
        LEFT JOIN categories ON cabinets.categoryId = categories.id
        WHERE (cabinets.name LIKE '%' || :kw || '%'
           OR categories.name LIKE '%' || :kw || '%')
          AND cabinets.isDeleted = 0
        ORDER BY cabinets.createdAt DESC
        """
    )
    fun searchCabinets(kw: String): Flow<List<SearchCabinetHit>>
}
