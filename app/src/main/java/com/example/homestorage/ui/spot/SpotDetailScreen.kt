package com.example.homestorage.ui.spot

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homestorage.data.db.ItemEntity
import com.example.homestorage.data.image.ImageStore
import com.example.homestorage.ui.common.ImagePicker
import com.example.homestorage.ui.common.PrimaryButton
import com.example.homestorage.ui.common.SheetAction
import com.example.homestorage.ui.common.VoiceInputField

/** 点位详情页：物品照片列表 + 添加/编辑/删除 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SpotDetailScreen(
    spotId: Long,
    onBack: () -> Unit,
    viewModel: SpotDetailViewModel = viewModel()
) {
    LaunchedEffect(spotId) { viewModel.load(spotId) }

    val items by viewModel.items.collectAsState()

    val context = LocalContext.current
    val imageStore = remember { ImageStore(context) }

    // 录入弹窗状态
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var editingItem by remember { mutableStateOf<ItemEntity?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemNote by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf(1) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // 相册选择
    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImageUri = uri
            itemName = ""
            itemNote = ""
            viewModel.clearRecognition()
            viewModel.recognizeImage(uri)
        }
    }

    // 拍照
    val launchCamera = ImagePicker.rememberCameraLauncher { uri ->
        if (uri != null) {
            pendingImageUri = uri
            itemName = ""
            itemNote = ""
            viewModel.clearRecognition()
            viewModel.recognizeImage(uri)
        }
    }

    // 批量选择（多张照片，逐个识别录入）
    val pickBatch = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addItemsFromImages(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("格子物品") },
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
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "这个格子还没有物品\n点击下方按钮添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            imageStore = imageStore,
                            onClick = {
                            editingItem = item
                            itemName = item.name
                            itemNote = item.note ?: ""
                            itemQuantity = item.quantity
                            }
                        )
                    }
                }
            }

            // 底部添加按钮（统一主按钮：白底阴影 + 图片图标）
            PrimaryButton(
                text = "添加物品照片",
                icon = Icons.Default.AddPhotoAlternate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { showSourceDialog = true }
            )
        }
    }

    // 图片来源选择（底部抽屉）
    if (showSourceDialog) {
        ModalBottomSheet(onDismissRequest = { showSourceDialog = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "添加物品照片",
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
                SheetAction(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    text = "批量选择（多张）",
                    onClick = {
                        showSourceDialog = false
                        pickBatch.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }

    // 批量录入进度弹窗
    val batchProgress by viewModel.batchProgress.collectAsState()
    batchProgress?.let { (current, total) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("批量录入中…") },
            text = {
                Column {
                    Text("正在识别并保存第 $current / $total 张照片")
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { current.toFloat() / total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // ---- 新物品录入弹窗 ----
    pendingImageUri?.let { uri ->
        // 识别状态订阅
        val recognizing by viewModel.recognizing.collectAsState()
        val recognizedText by viewModel.recognizedText.collectAsState()
        val recognizedTags by viewModel.recognizedTags.collectAsState()
        val recognizeFailed by viewModel.recognizeFailed.collectAsState()

        // 识别完成自动回填名称框（用户可修改）
        LaunchedEffect(recognizedText) {
            recognizedText?.let { if (itemName.isBlank()) itemName = it }
        }

        AlertDialog(
            onDismissRequest = { pendingImageUri = null },
            title = { Text("录入物品名称") },
            text = {
                Column {
                    // 图片预览 + 识别状态
                    AsyncImage(
                        model = uri,
                        contentDescription = "物品照片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        recognizing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "智能识别图片中…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        recognizeFailed -> {
                            Text(
                                "智能识别失败，请手动输入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        recognizedText != null -> {
                            Text(
                                "识别关键词：$recognizedText（可修改）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    VoiceInputField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = "物品名称（必填）",
                        placeholder = "识别结果或手动输入",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemNote,
                        onValueChange = { itemNote = it },
                        label = { Text("备注（可选）") },
                        placeholder = { Text("如：还剩半瓶") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    QuantitySelector(
                        quantity = itemQuantity,
                        onQuantityChange = { itemQuantity = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addItem(uri, itemName, recognizedTags, itemQuantity, itemNote)
                        pendingImageUri = null
                    },
                    enabled = itemName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImageUri = null }) { Text("取消") }
            }
        )
    }

    // ---- 编辑/删除物品弹窗 ----
    editingItem?.let { item ->
        val otherSpots by viewModel.otherSpots.collectAsState()
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("编辑物品") },
            text = {
                Column {
                    VoiceInputField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = "物品名称",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemNote,
                        onValueChange = { itemNote = it },
                        label = { Text("备注") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    QuantitySelector(
                        quantity = itemQuantity,
                        onQuantityChange = { itemQuantity = it }
                    )
                    if (otherSpots.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "移动到其他格子",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        otherSpots.forEach { spot ->
                            TextButton(
                                onClick = {
                                    viewModel.moveItem(item.id, spot.id)
                                    editingItem = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    spot.title ?: "格子 ${(spot.x * 100).toInt()}% ${(spot.y * 100).toInt()}%"
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.deleteItem(item.id)
                            editingItem = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除物品", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateItem(
                            item.copy(name = itemName, note = itemNote, quantity = itemQuantity)
                        )
                        editingItem = null
                    },
                    enabled = itemName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("取消") }
            }
        )
    }
}

/** 物品卡片：照片 + 名称 + 备注（主流列表卡片：圆角容器 + 圆角图） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    item: ItemEntity,
    imageStore: ImageStore,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val file = item.imagePath?.let { imageStore.getFile(it) }
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.note?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // 数量角标（多件时显示）
            if (item.quantity > 1) {
                Text(
                    "x ${item.quantity}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

/** 数量选择器：- 数字 +（最少 1） */
@Composable
private fun QuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "数量",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        OutlinedIconButton(
            onClick = { onQuantityChange((quantity - 1).coerceAtLeast(1)) },
            enabled = quantity > 1,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "减", modifier = Modifier.size(18.dp))
        }
        Text(
            "$quantity",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        OutlinedIconButton(
            onClick = { onQuantityChange(quantity + 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "加", modifier = Modifier.size(18.dp))
        }
    }
}
