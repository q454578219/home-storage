package com.example.homestorage.ui.floorplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homestorage.data.db.CabinetOnFloorPlan
import com.example.homestorage.data.image.ImageStore
import com.example.homestorage.ui.common.SheetAction
import kotlin.math.roundToInt

/**
 * 户型图详情页：户型图照片 + 柜子标记层
 *
 * 点击空白处 → 弹窗选择"新建柜子并挂载"或"挂载已有柜子"；
 * 点击柜子标记 → 跳转柜子详情；长按拖动移动标记位置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorPlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    onOpenCabinet: (Long) -> Unit,
    onCreateCabinet: (Float, Float) -> Unit,
    viewModel: FloorPlanDetailViewModel = viewModel()
) {
    val floorPlan by viewModel.floorPlan.collectAsState()
    val cabinets by viewModel.cabinets.collectAsState()
    val unattached by viewModel.unattachedCabinets.collectAsState()

    LaunchedEffect(planId) { viewModel.load(planId) }

    val context = LocalContext.current
    val imageStore = remember { ImageStore(context) }

    // 交互状态：点击位置（归一化坐标）/ 挂载选择 / 拖动标记
    var tapPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var attachPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var dragCabinet by remember { mutableStateOf<CabinetOnFloorPlan?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // pointerInput 内部引用最新值（避免 key 变化重启手势）
    val cabinetsState = rememberUpdatedState(cabinets)
    val containerState = rememberUpdatedState(containerSize)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(floorPlan?.name ?: "户型图") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (floorPlan == null) {
                // 加载中/不存在
                Text(
                    "户型图不存在或已删除",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val imageFile = imageStore.getFile(floorPlan!!.imagePath)
                val ratio = imageStore.getAspectRatio(floorPlan!!.imagePath) ?: 1f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .align(Alignment.TopCenter)
                        .onSizeChanged { containerSize = it }
                ) {
                    // 户型图
                    AsyncImage(
                        model = imageFile,
                        contentDescription = floorPlan?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    // 柜子标记层
                    cabinets.forEach { cabinet ->
                        val x = containerSize.width * cabinet.x
                        val y = containerSize.height * cabinet.y
                        CabinetMarker(
                            cabinet = cabinet,
                            coverFile = cabinet.coverImagePath?.let { imageStore.getFile(it) },
                            modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        )
                    }

                    // 手势覆盖层：点击（点位/空白）+ 长按拖动移动
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                val radiusPx = 18.dp.toPx()
                                detectTapGestures { offset ->
                                    val cabinets = cabinetsState.value
                                    val size = containerState.value
                                    if (size.width <= 0 || size.height <= 0) return@detectTapGestures
                                    val hit = cabinets.find { cabinet ->
                                        val dx = offset.x - size.width * cabinet.x
                                        val dy = offset.y - size.height * cabinet.y
                                        dx * dx + dy * dy <= radiusPx * radiusPx
                                    }
                                    if (hit != null) {
                                        onOpenCabinet(hit.id)
                                    } else {
                                        tapPos = Pair(
                                            offset.x / size.width,
                                            offset.y / size.height
                                        )
                                        attachPos = tapPos
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                val radiusPx = 18.dp.toPx()
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        val cabinets = cabinetsState.value
                                        val size = containerState.value
                                        dragCabinet = cabinets.find { cabinet ->
                                            val dx = offset.x - size.width * cabinet.x
                                            val dy = offset.y - size.height * cabinet.y
                                            dx * dx + dy * dy <= radiusPx * radiusPx
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragCabinet?.let { cabinet ->
                                            val size = containerState.value
                                            if (size.width > 0 && size.height > 0) {
                                                viewModel.moveCabinet(
                                                    cabinet.id,
                                                    cabinet.x + dragAmount.x / size.width,
                                                    cabinet.y + dragAmount.y / size.height
                                                )
                                            }
                                        }
                                    },
                                    onDragEnd = { dragCabinet = null },
                                    onDragCancel = { dragCabinet = null }
                                )
                            }
                    )
                }
            }
        }
    }

    // ---- 点击空白操作弹窗 ----
    tapPos?.let { (x, y) ->
        ModalBottomSheet(onDismissRequest = { tapPos = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "在户型图上添加柜子",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = "新建柜子并挂载到此处",
                    onClick = {
                        tapPos = null
                        onCreateCabinet(x, y)
                    }
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    text = "挂载已有柜子",
                    onClick = {
                        tapPos = null
                        showAttachSheet = true
                    }
                )
            }
        }
    }

    // ---- 挂载已有柜子选择 ----
    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "选择要挂载的柜子",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                if (unattached.isEmpty()) {
                    Text(
                        "没有可挂载的柜子（所有柜子都已挂载）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
                unattached.forEach { cabinet ->
                    SheetAction(
                        icon = {
                            if (cabinet.coverImagePath != null) {
                                AsyncImage(
                                    model = imageStore.getFile(cabinet.coverImagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Inventory2, contentDescription = null)
                            }
                        },
                        text = cabinet.name,
                        onClick = {
                            showAttachSheet = false
                            val pos = attachPos ?: return@SheetAction
                            viewModel.attachCabinet(cabinet.id, pos.first, pos.second)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 柜子标记：圆形缩略图（有封面显示封面，否则显示名称首字）
 *
 * @param cabinet 柜子信息
 * @param coverFile 封面图片文件（可空）
 * @param modifier 外层修饰符
 */
@Composable
private fun CabinetMarker(
    cabinet: CabinetOnFloorPlan,
    coverFile: java.io.File?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset { IntOffset((-18).dp.toPx().roundToInt(), (-18).dp.toPx().roundToInt()) }
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (coverFile != null) {
            AsyncImage(
                model = coverFile,
                contentDescription = cabinet.name,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                cabinet.name.take(1),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}