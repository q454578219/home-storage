package com.example.homestorage.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * 语音识别结果
 *
 * @property text 识别文本（失败为 null）
 * @property error 错误码（成功为 null；-1 设备不支持；-2 创建识别器失败；其余为 SpeechRecognizer 错误码）
 */
data class RecognizeResult(val text: String?, val error: Int?)

/**
 * 语音输入工具：封装系统 SpeechRecognizer（后台识别，无系统界面）
 *
 * 录音由系统语音引擎（如小米小爱语音）完成，App 无需 RECORD_AUDIO 权限；
 * 引擎自身的麦克风权限需在系统设置中授权。
 * 必须在主线程调用（SpeechRecognizer 要求），本封装内部自动切主线程。
 */
object SpeechInput {

    /** 设备不支持语音识别 */
    const val ERROR_NOT_AVAILABLE = -1

    /** 创建识别器失败 */
    const val ERROR_CREATE_FAILED = -2

    /**
     * 开始语音识别，返回识别文本与错误码
     *
     * @param context 上下文
     * @return 识别结果；协程取消时自动销毁识别器
     */
    suspend fun recognize(context: Context): RecognizeResult {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return RecognizeResult(null, ERROR_NOT_AVAILABLE)
        }
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val sr = try {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } catch (e: Exception) {
                    if (!cont.isCancelled) cont.resume(RecognizeResult(null, ERROR_CREATE_FAILED))
                    return@suspendCancellableCoroutine
                }
                val listener = object : RecognitionListener {
                    /** 是否已结束（onResults/onError 可能重复回调，只响应第一次） */
                    private var finished = false

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}

                    override fun onResults(results: Bundle?) {
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        finish(RecognizeResult(text?.takeIf { it.isNotBlank() }, null))
                    }

                    override fun onError(error: Int) {
                        finish(RecognizeResult(null, error))
                    }

                    private fun finish(result: RecognizeResult) {
                        if (finished) return
                        finished = true
                        sr.destroy()
                        if (!cont.isCancelled) cont.resume(result)
                    }
                }
                sr.setRecognitionListener(listener)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.SIMPLIFIED_CHINESE.toLanguageTag()
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                try {
                    sr.startListening(intent)
                } catch (e: Exception) {
                    sr.destroy()
                    if (!cont.isCancelled) cont.resume(RecognizeResult(null, ERROR_CREATE_FAILED))
                }
                cont.invokeOnCancellation { sr.destroy() }
            }
        }
    }
}
