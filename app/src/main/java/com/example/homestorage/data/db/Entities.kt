package com.example.homestorage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分类实体：柜子所属的房间/区域分类 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 分类名称，如"客厅" */
    val name: String,
    /** 是否预置分类（预置不可删除） */
    val isPreset: Boolean = false,
)

/** 户型图实体：一张户型图 = 一个楼层/区域的空间地图，点击位置挂载柜子 */
@Entity(tableName = "floor_plans")
data class FloorPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 户型图名称，如"一层" */
    val name: String,
    /** 户型图图片相对路径（ImageStore 内） */
    val imagePath: String,
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
)

/** 柜子实体：一个储物柜/储物空间 */
@Entity(tableName = "cabinets")
data class CabinetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 柜子名称，如"厨房吊柜" */
    val name: String,
    /** 封面照片相对路径（ImageStore 内），为空表示无封面 */
    val coverImagePath: String? = null,
    /** 所属分类 id */
    val categoryId: Long? = null,
    /** 是否已移入回收站（软删除） */
    val isDeleted: Boolean = false,
    /** 所属户型图 id（null = 未挂载到户型图） */
    val floorPlanId: Long? = null,
    /** 柜子在户型图上的归一化横坐标 0~1 */
    val x: Float = 0.5f,
    /** 柜子在户型图上的归一化纵坐标 0~1 */
    val y: Float = 0.5f,
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
)

/** 点位实体：柜子照片上的一个格子/位置标记 */
@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 所属柜子 id */
    val cabinetId: Long,
    /** 归一化横坐标 0~1（相对柜子照片宽度） */
    val x: Float,
    /** 归一化纵坐标 0~1（相对柜子照片高度） */
    val y: Float,
    /** 点位标题（可选，如"上层左边"） */
    val title: String? = null,
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
)

/** 物品实体：点位下的一个物品 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 所属点位 id */
    val spotId: Long,
    /** 物品照片相对路径（ImageStore 内） */
    val imagePath: String? = null,
    /** 物品名称（搜索索引主字段） */
    val name: String,
    /** AI 识别关键词，逗号分隔（辅助搜索，如"酱油"识别出"瓶子,玻璃瓶"） */
    val tags: String? = null,
    /** 数量（同种物品多件时计数） */
    val quantity: Int = 1,
    /** 备注（可选） */
    val note: String? = null,
    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis(),
)

/** 搜索结果-物品命中：跨表投影（物品+点位+柜子+分类） */
data class SearchItemHit(
    val itemId: Long,
    val itemName: String,
    val itemImagePath: String?,
    val itemNote: String?,
    val spotId: Long,
    val spotX: Float,
    val spotY: Float,
    val cabinetId: Long,
    val cabinetName: String,
    val categoryName: String?,
)

/** 搜索结果-柜子命中：跨表投影（柜子+分类） */
data class SearchCabinetHit(
    val cabinetId: Long,
    val cabinetName: String,
    val coverImagePath: String?,
    val categoryName: String?,
)
