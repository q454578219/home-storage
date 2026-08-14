package com.example.homestorage.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homestorage.ui.common.ImagePicker
import com.example.homestorage.ui.common.PrimaryButton
import com.example.homestorage.ui.common.SheetAction
import com.example.homestorage.ui.common.VoiceInputField

/** 无涟漪点击扩展：图片选择区点击不显示波纹 */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )

/** 新建柜子页：封面选择（拍照/相册）+ 名称 + 分类 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCabinetScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: CreateCabinetViewModel = viewModel()
) {
    val name by viewModel.name.collectAsState()
    val categoryId by viewModel.categoryId.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val context = LocalContext.current
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // 相册选择
    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) coverUri = uri
        showSourceDialog = false
    }

    // 拍照
    val launchCamera = ImagePicker.rememberCameraLauncher { uri ->
        if (uri != null) coverUri = uri
        showSourceDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建柜子") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 封面选择区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clickableNoRipple { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = "柜子封面",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "添加柜子照片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 名称输入（支持语音）
            VoiceInputField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = "柜子名称（必填）",
                placeholder = "如：厨房吊柜",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // 分类选择
            Text(
                "选择分类",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { viewModel.categoryId.value = null },
                        label = { Text("未分类") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { viewModel.categoryId.value = category.id },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // 保存（统一主按钮：白底阴影 + 对勾图标）
            PrimaryButton(
                text = "保存柜子",
                icon = Icons.Default.Check,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.save(coverUri, onSaved) }
            )
        }
    }

    // 图片来源选择（底部抽屉）
    if (showSourceDialog) {
        ModalBottomSheet(onDismissRequest = { showSourceDialog = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "选择封面来源",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SheetAction(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    text = "从相册选择",
                    onClick = {
                        showSourceDialog = false
                        pickGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                SheetAction(
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    text = "拍照",
                    onClick = {
                        showSourceDialog = false
                        launchCamera(ImagePicker.createCameraFile(context))
                    }
                )
            }
        }
    }
}
