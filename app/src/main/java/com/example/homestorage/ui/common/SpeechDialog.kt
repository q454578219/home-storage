package com.example.homestorage.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.speech.SpeechRecognizer
import kotlinx.coroutines.delay

/**
 * 语音识别对话框：麦克风波纹动画 + 状态提示
 *
 * 打开即自动开始识别，识别完成自动回填并关闭；
 * 取消/失败时提示原因后自动关闭。
 *
 * @param onResult 识别成功回调（文本）
 * @param onDismiss 关闭回调
 */
@Composable
fun SpeechDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("正在聆听…") }

    // 对话框关闭（composable 移除）时协程取消，SpeechInput 自动销毁识别器
    LaunchedEffect(Unit) {
        val result = SpeechInput.recognize(context)
        if (result.error == null && !result.text.isNullOrBlank()) {
            onResult(result.text)
            onDismiss()
        } else {
            status = when (result.error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请重试"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有听到声音"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少语音权限，请在系统设置中授权"
                SpeechInput.ERROR_NOT_AVAILABLE -> "当前设备不支持语音识别"
                SpeechInput.ERROR_CREATE_FAILED -> "语音识别启动失败"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音服务繁忙，请稍后再试"
                else -> "识别失败，请重试"
            }
            delay(1200)
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 36.dp)
            ) {
                // 麦克风 + 波纹动画
                val infinite = rememberInfiniteTransition(label = "ripple")
                val rippleScale by infinite.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.45f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rippleScale"
                )
                val rippleAlpha by infinite.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 0.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rippleAlpha"
                )
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(rippleScale)
                            .alpha(rippleAlpha)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "语音识别中",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    status,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}
