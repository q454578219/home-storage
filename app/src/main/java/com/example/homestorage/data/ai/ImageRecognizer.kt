package com.example.homestorage.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.homestorage.BuildConfig
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 图片智能识别：调用智谱 GLM-4V-Flash（永久免费视觉模型）
 *
 * 上传图片 → 返回图中物品关键词（中文），失败返回 null。
 * API Key 通过 build.gradle.kts 从 local.properties 的 zhipuApiKey 注入，
 * 未配置时识别功能自动禁用（返回 null）。
 */
object ImageRecognizer {

    /** 智谱 API 地址（OpenAI 兼容） */
    private const val API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"

    /** 模型名：GLM-4V-Flash 永久免费 */
    private const val MODEL = "glm-4v-flash"

    /** 图片最长边压缩尺寸（px），模型限制单图 5MB / 6000px */
    private const val MAX_DIMENSION = 1024

    /** 识别超时（秒） */
    private const val TIMEOUT_SECONDS = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 识别图片中的物品关键词
     *
     * @param context 上下文（读取 Uri 内容用）
     * @param imageUri 图片 Uri（相册/拍照均可）
     * @return 物品关键词（中文，多个词用逗号分隔）；识别失败返回 null
     */
    suspend fun recognize(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        // 未配置 API Key 时识别功能禁用
        if (BuildConfig.ZHIPU_API_KEY.isBlank()) return@withContext null
        try {
            val base64 = readAndCompress(context, imageUri) ?: return@withContext null
            val json = buildRequestJson(base64)
            val response = client.newCall(buildHttpRequest(json)).execute()
            if (!response.isSuccessful) return@withContext null
            val content = response.body?.string() ?: return@withContext null
            parseContent(content)
        } catch (e: Exception) {
            null
        }
    }

    /** 读取 Uri 图片 → 压缩 → base64 */
    private fun readAndCompress(context: Context, imageUri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        val max = maxOf(bounds.outWidth, bounds.outHeight)
        while (max / sampleSize > MAX_DIMENSION) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        bitmap.recycle()
        return android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)
    }

    /** 构造请求体 JSON */
    private fun buildRequestJson(base64: String): String {
        val content = JSONArray()
        content.put(
            JSONObject().put("type", "text").put(
                "text",
                "识别图片中的所有物品，用中文名词输出，只输出物品名，多个用逗号分隔，不要输出其他任何内容"
            )
        )
        content.put(
            JSONObject()
                .put("type", "image_url")
                .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64"))
        )
        val message = JSONObject().put("role", "user").put("content", content)
        return JSONObject()
            .put("model", MODEL)
            .put("messages", JSONArray().put(message))
            .toString()
    }

    /** 构造 HTTP 请求 */
    private fun buildHttpRequest(json: String): Request =
        Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer ${BuildConfig.ZHIPU_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

    /** 解析响应中的文本内容 */
    private fun parseContent(response: String): String? {
        val json = JSONObject(response)
        val content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
        return content.ifBlank { null }
    }
}
