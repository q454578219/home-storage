package com.example.homestorage.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份/恢复管理器：将数据库与物品照片打包为 ZIP 导出到"下载"目录，或从 ZIP 恢复
 */
object BackupManager {

    /** 数据库文件名 */
    private const val DB_NAME = "home_storage.db"

    /** 图片目录名 */
    private const val IMAGE_DIR = "imagestore"

    /** 导出 ZIP 中允许的路径（防路径穿越） */
    private fun isSafePath(name: String): Boolean {
        if (name.contains("..")) return false
        return name == DB_NAME || name.startsWith("$IMAGE_DIR/")
    }

    /**
     * 导出备份：打包数据库 + 图片目录为 ZIP，写入系统"下载"目录
     *
     * @param context 应用上下文
     * @return 生成的 ZIP Uri（失败返回 null）
     */
    fun exportBackup(context: Context): Uri? {
        val filesDir = context.filesDir
        val dbFile = File(filesDir, DB_NAME)
        if (!dbFile.exists()) return null

        val displayName = "HomeStorage备份_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".zip"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(DB_NAME))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()

                val imageDir = File(filesDir, IMAGE_DIR)
                imageDir.listFiles()?.filter { it.isFile }?.forEach { image ->
                    zip.putNextEntry(ZipEntry("$IMAGE_DIR/${image.name}"))
                    image.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        return uri
    }

    /**
     * 从 ZIP 恢复备份：解压数据库与图片到应用私有目录（覆盖旧数据）
     *
     * @param context 应用上下文
     * @param zipUri 备份 ZIP 的 Uri
     * @return 是否成功（含数据库文件）
     */
    fun restoreBackup(context: Context, zipUri: Uri): Boolean {
        val filesDir = context.filesDir
        var hasDb = false
        val input = context.contentResolver.openInputStream(zipUri) ?: return false
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (isSafePath(name)) {
                    val target = File(filesDir, name)
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read = zip.read(buffer)
                        while (read > 0) {
                            out.write(buffer, 0, read)
                            read = zip.read(buffer)
                        }
                    }
                    if (name == DB_NAME) hasDb = true
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return hasDb
    }
}
