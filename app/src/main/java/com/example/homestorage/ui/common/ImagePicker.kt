package com.example.homestorage.ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片获取工具：统一封装"拍照"与"相册选择"两种来源
 *
 * 拍照走系统相机 App（ACTION_IMAGE_CAPTURE），照片先存 App 缓存目录（FileProvider 授权），
 * 返回 Uri 后由调用方保存到 ImageStore
 */
object ImagePicker {

    /**
     * 创建拍照输出文件（缓存目录 camera/ 下，文件名带时间戳）
     *
     * @param context 上下文
     * @return 目标文件
     */
    fun createCameraFile(context: Context): File {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "IMG_$name.jpg")
    }

    /**
     * 生成系统相机 Intent（含 FileProvider 授权）
     *
     * @param context 上下文
     * @param outputFile 拍照输出文件
     * @return 相机 Intent
     */
    fun cameraIntent(context: Context, outputFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    /**
     * 记忆拍照启动器
     *
     * @param onPhotoTaken 拍照完成回调（成功返回 Uri，取消返回 null）
     * @return 启动函数：传入拍照输出文件后拉起系统相机
     */
    @Composable
    fun rememberCameraLauncher(onPhotoTaken: (Uri?) -> Unit): (File) -> Unit {
        val context = LocalContext.current
        var pendingUri by remember { mutableStateOf<Uri?>(null) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                pendingUri?.let(onPhotoTaken)
            } else {
                onPhotoTaken(null)
            }
        }

        return { file ->
            pendingUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            launcher.launch(cameraIntent(context, file))
        }
    }
}
