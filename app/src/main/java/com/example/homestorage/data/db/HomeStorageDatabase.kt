package com.example.homestorage.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/** 应用数据库：分类/柜子/点位/物品/户型图 */
@Database(
    entities = [
        CategoryEntity::class,
        CabinetEntity::class,
        SpotEntity::class,
        ItemEntity::class,
        FloorPlanEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class HomeStorageDatabase : RoomDatabase() {

    /** 分类表 DAO */
    abstract fun categoryDao(): CategoryDao

    /** 柜子表 DAO */
    abstract fun cabinetDao(): CabinetDao

    /** 点位表 DAO */
    abstract fun spotDao(): SpotDao

    /** 物品表 DAO */
    abstract fun itemDao(): ItemDao

    /** 户型图表 DAO */
    abstract fun floorPlanDao(): FloorPlanDao

    companion object {
        /** 数据库文件名 */
        private const val DB_NAME = "home_storage.db"

        /** 预置分类列表（按家庭常见房间预设） */
        private val PRESET_CATEGORIES = listOf("客厅", "卧室", "书房", "厨房", "卫生间", "阳台", "其他")

        /**
         * 单例获取数据库实例
         *
         * @param context 应用上下文
         */
        fun get(context: Context): HomeStorageDatabase =
            InstanceHolder.instance ?: synchronized(this) {
                InstanceHolder.instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HomeStorageDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(SeedCallback())
                    .build()
                    .also { InstanceHolder.instance = it }
            }

        /** v1 → v2：物品表新增 tags 列（AI 识别关键词，辅助搜索） */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN tags TEXT")
            }
        }

        /** v2 → v3：物品表新增 quantity 列（数量管理，默认 1） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** v3 → v4：柜子表新增 isDeleted 列（回收站软删除，默认 0） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cabinets ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v4 → v5：新增户型图表 + 柜子表挂载字段（空间定位） */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS floor_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        imagePath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE cabinets ADD COLUMN floorPlanId INTEGER")
                db.execSQL("ALTER TABLE cabinets ADD COLUMN x REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE cabinets ADD COLUMN y REAL NOT NULL DEFAULT 0.5")
            }
        }

        /** 首次创建数据库时写入预置分类 */
        private class SeedCallback : Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                PRESET_CATEGORIES.forEach { name ->
                    db.execSQL(
                        "INSERT INTO categories (name, isPreset) VALUES ('$name', 1)"
                    )
                }
            }
        }

        /** 实例持有者（双重检查锁） */
        private object InstanceHolder {
            @Volatile
            var instance: HomeStorageDatabase? = null
        }
    }
}
