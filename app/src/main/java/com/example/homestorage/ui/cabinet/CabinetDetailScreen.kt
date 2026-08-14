package com.example.homestorage.ui.cabinet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homestorage.data.db.SpotWithPreview
import com.example.homestorage.data.image.ImageStore
import com.example.homestorage.ui.common.SheetAction
import java.io.File
import kotlin.math.roundToInt

/** 柜子详情页：照片 + 点位叠加层（点击加点、长按移动、点击弹窗） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CabinetDetailScreen(
    cabinetId: Long,
    highlightSpotId: Long? = null,
    onBack: () -> Unit,
    onOpenSpot: (Long) -> Unit,
    viewModel: CabinetDetailViewModel = viewModel()
) {
    LaunchedEffect(cabinetId) { viewModel.load(cabinetId) }

    val cabinet by viewModel.cabinet.collectAsState()
    val spots by viewModel.spots.collectAsState()
    val navigateToSpot by viewModel.navigateToSpot.collectAsState()

    // 新增点位成功后直接进入点位详情
    LaunchedEffect(navigateToSpot) {
        navigateToSpot?.let {
            viewModel.consumeNavigate()
            onOpenSpot(it)
        }
    }

    val context = LocalContext.current
    val imageStore = remember { ImageStore(context) }

    var addSpotPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var spotTitle by remember { mutableStateOf("") }
    var tappedSpot by remember { mutableStateOf<SpotWithPreview?>(null) }
    var editSpotTitle by remember { mutableStateOf(false) }
    var deleteSpot by remember { mutableStateOf<SpotWithPreview?>(null) }
    var dragSpot by remember { mutableStateOf<SpotWithPreview?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // pointerInput 内部引用最新值（避免 key 变化重启手势）
    val spotsState = rememberUpdatedState(spots)
    val containerState = rememberUpdatedState(containerSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cabinet?.name ?: "柜子") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (cabinet?.coverImagePath != null) {
                        IconButton(onClick = { showFullScreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "全屏查看")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val coverFile = cabinet?.coverImagePath?.let { imageStore.getFile(it) }
            if (coverFile == null) {
                // 无封面：提示
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.padding(top = 80.dp))
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.padding(8.dp))
                    Text(
                        "该柜子还没有照片\n请在首页长按柜子选择「更换封面」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 照片 + 点位层
                val ratio = remember(coverFile) { imageStore.getAspectRatio(cabinet?.coverImagePath) ?: 1f }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .onSizeChanged { containerSize = it }
                ) {
                    AsyncImage(
                        model = coverFile,
                        contentDescription = cabinet?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // 点位层（纯视觉，手势统一由覆盖层处理）
                    spots.forEach { spot ->
                        val x = containerSize.width * spot.x
                        val y = containerSize.height * spot.y
                        val highlight = highlightSpotId != null && highlightSpotId == spot.id
                        SpotMarker(
                            spot = spot,
                            highlight = highlight,
                            previewFile = spot.firstItemImage?.let { imageStore.getFile(it) },
                            modifier = Modifier
                                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        )
                    }

                    // 手势覆盖层：点击（点位/空白）+ 长按拖动移动点位
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                val radiusPx = 14.dp.toPx()
                                detectTapGestures { offset ->
                                    val spots = spotsState.value
                                    val size = containerState.value
                                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                    val hit = spots.find { spot ->
                                        val dx = offset.x - size.width * spot.x
                                        val dy = offset.y - size.height * spot.y
                                        dx * dx + dy * dy <= radiusPx * radiusPx
                                    }
                                    if (hit != null) {
                                        tappedSpot = hit
                                    } else {
                                        addSpotPos = Pair(
                                            offset.x / size.width,
                                            offset.y / size.height
                                        )
                                        spotTitle = ""
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                val radiusPx = 14.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val spots = spotsState.value
                                        val size = containerState.value
                                        dragSpot = spots.find { spot ->
                                            val dx = offset.x - size.width * spot.x
                                            val dy = offset.y - size.height * spot.y
                                            dx * dx + dy * dy <= radiusPx * radiusPx
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragSpot?.let { spot ->
                                            val size = containerState.value
                                            if (size.width > 0 && size.height > 0) {
                                                viewModel.moveSpot(
                                                    spot,
                                                    spot.x + dragAmount.x / size.width,
                                                    spot.y + dragAmount.y / size.height
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = { dragSpot = null },
                                    onDragCancel = { dragSpot = null }
                                )
                            }
                    )
                }
            }

            // 使用提示
            if (coverFile != null && spots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "点击照片空白处添加点位",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    // ---- 添加点位对话框 ----
    addSpotPos?.let { (x, y) ->
        AlertDialog(
            onDismissRequest = { addSpotPos = null },
            title = { Text("添加点位") },
            text = {
                Column {
                    Text("坐标 (${(x * 100).roundToInt()}%, ${(y * 100).roundToInt()}%)")
                    OutlinedTextField(
                        value = spotTitle,
                        onValueChange = { spotTitle = it },
                        label = { Text("点位标题（可选）") },
                        placeholder = { Text("如：上层左边") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addSpot(x, y, spotTitle.trim().ifBlank { null })
                    addSpotPos = null
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { addSpotPos = null }) { Text("取消") }
            }
        )
    }

    // ---- 点位操作弹窗（底部抽屉） ----
    tappedSpot?.let { spot ->
        ModalBottomSheet(onDismissRequest = { tappedSpot = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    spot.title ?: "点位",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    text = "查看 / 添加物品",
                    onClick = {
                        tappedSpot = null
                        onOpenSpot(spot.id)
                    }
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = "编辑标题",
                    onClick = {
                        editSpotTitle = true
                        spotTitle = spot.title ?: ""
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    text = "删除点位",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        deleteSpot = spot
                        tappedSpot = null
                    }
                )
            }
        }
    }

    // ---- 编辑点位标题 ----
    if (editSpotTitle) {
        AlertDialog(
            onDismissRequest = { editSpotTitle = false },
            title = { Text("编辑点位标题") },
            text = {
                OutlinedTextField(
                    value = spotTitle,
                    onValueChange = { spotTitle = it },
                    label = { Text("点位标题") },
                    placeholder = { Text("如：上层左边") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tappedSpot?.let { viewModel.renameSpot(it, spotTitle.trim().ifBlank { null }) }
                    editSpotTitle = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editSpotTitle = false }) { Text("取消") }
            }
        )
    }

    // ---- 删除点位确认 ----
    deleteSpot?.let { spot ->
        AlertDialog(
            onDismissRequest = { deleteSpot = null },
            title = { Text("删除点位") },
            text = { Text("确定删除该点位？其物品照片将一并删除，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSpot(spot.id)
                    deleteSpot = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSpot = null }) { Text("取消") }
            }
        )
    }

    // ---- 全屏查看封面 ----
    val coverFile = cabinet?.coverImagePath?.let { imageStore.getFile(it) }
    if (showFullScreen && coverFile != null) {
        FullScreenImageDialog(coverFile = coverFile, onDismiss = { showFullScreen = false })
    }
}

/** 全屏查看封面图：捏合缩放 + 拖动平移，点右上角关闭 */
@Composable
private fun FullScreenImageDialog(
    coverFile: File,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        scale = (scale * gestureZoom).coerceIn(1f, 5f)
                        pan += gesturePan
                    }
                }
        ) {
            AsyncImage(
                model = coverFile,
                contentDescription = "柜子封面",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                    },
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}

/** 点位标记（纯视觉）：圆形 + 白心（高亮时脉冲放大 3 次），手势由覆盖层统一处理 */
@Composable
private fun SpotMarker(
    spot: SpotWithPreview,
    highlight: Boolean,
    previewFile: File?,
    modifier: Modifier = Modifier
) {
    // 高亮脉冲动画
    val scale = remember { Animatable(1f) }
    LaunchedEffect(highlight) {
        if (highlight) {
            repeat(3) {
                scale.animateTo(1.6f, tween(280))
                scale.animateTo(1f, tween(280))
            }
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset((-18).dp.toPx().roundToInt(), (-18).dp.toPx().roundToInt()) }
            .size(36.dp)
            .graphicsLayer { this.scaleX = scale.value; this.scaleY = scale.value }
            .clip(CircleShape)
            .background(
                if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                CircleShape
            )
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // 有物品时显示首件物品缩略图，否则显示白心
        if (previewFile != null) {
            AsyncImage(
                model = previewFile,
                contentDescription = spot.title ?: "点位",
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
