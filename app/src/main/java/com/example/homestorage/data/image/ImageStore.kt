package com.example.homestorage.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片存储：统一存到 filesDir/imagestore/，保存时压缩降采样，
 * 数据库只存相对路径（如 images/xxx.jpg），换目录迁移时数据不受影响
 */
class ImageStore(private val context: Context) {

    /** 图片根目录（相对路径的根） */
    private val rootDir: File
        get() = File(context.filesDir, "imagestore").apply { mkdirs() }

    /** 柜子封面最大边长（px） */
    private val MAX_CABINET_SIZE = 1600

    /** 物品照片最大边长（px） */
    private val MAX_ITEM_SIZE = 1000

    /** JPEG 压缩质量 */
    private val JPEG_QUALITY = 85

    /**
     * 保存图片（从 Uri），自动压缩降采样
     *
     * @param sourceUri 源图片 Uri（相册/拍照）
     * @param maxSize 最大边长，超过则等比缩小
     * @return 相对路径（如 images/xxx.jpg），失败返回 null
     */
    fun saveFromUri(sourceUri: Uri, maxSize: Int = MAX_ITEM_SIZE): String? {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: return null
        return saveBytes(bytes, maxSize)
    }

    /**
     * 保存图片（从字节数组），自动压缩降采样
     *
     * @param bytes 图片字节
     * @param maxSize 最大边长，超过则等比缩小
     * @return 相对路径（如 images/xxx.jpg），解码失败返回 null
     */
    fun saveBytes(bytes: ByteArray, maxSize: Int = MAX_ITEM_SIZE): String? {
        val bitmap = decodeSampled(bytes, maxSize) ?: return null
        return saveBitmap(bitmap)
    }

    /**
     * 读取图片文件
     *
     * @param relPath 相对路径（images/xxx.jpg）
     * @return 图片文件，不存在返回 null
     */
    fun getFile(relPath: String?): File? {
        if (relPath.isNullOrBlank()) return null
        val file = File(rootDir, relPath.removePrefix("images/"))
        return if (file.exists()) file else null
    }

    /**
     * 删除图片文件
     *
     * @param relPath 相对路径
     */
    fun delete(relPath: String?) {
        getFile(relPath)?.delete()
    }

    /**
     * 读取图片宽高比（宽/高），用于布局等比显示
     *
     * @param relPath 相对路径
     * @return 宽高比，读取失败返回 null
     */
    fun getAspectRatio(relPath: String?): Float? {
        val file = getFile(relPath) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return bounds.outWidth.toFloat() / bounds.outHeight.toFloat()
    }

    /** 采样解码：先读尺寸，再按需降采样 */
    private fun decodeSampled(bytes: ByteArray, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxSize || bounds.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** 写入磁盘（JPEG） */
    private fun saveBitmap(bitmap: Bitmap): String {
        val relPath = "images/${UUID.randomUUID()}.jpg"
        val file = File(rootDir, relPath.removePrefix("images/"))
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        return relPath
    }
}
