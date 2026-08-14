package com.example.homestorage.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * 语音输入输入框：OutlinedTextField + 麦克风按钮
 *
 * 点击麦克风弹出自定义录音对话框（SpeechDialog），识别结果填入输入框；
 * 识别失败时对话框内提示原因，手动输入兜底。
 *
 * @param value 输入框值
 * @param onValueChange 值变化回调
 * @param label 标签文本
 * @param placeholder 占位文本
 * @param modifier 修饰符
 */
@Composable
fun VoiceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    var showSpeech by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { androidx.compose.material3.Text(label) },
        placeholder = { androidx.compose.material3.Text(placeholder) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showSpeech = true }) {
                Icon(
                    Icons.Default.MicNone,
                    contentDescription = "语音输入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (showSpeech) {
        SpeechDialog(
            onResult = { text -> onValueChange(text) },
            onDismiss = { showSpeech = false }
        )
    }
}
