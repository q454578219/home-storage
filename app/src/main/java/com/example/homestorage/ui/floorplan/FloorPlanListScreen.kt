package com.example.homestorage.ui.floorplan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.homestorage.data.db.FloorPlanEntity
import com.example.homestorage.data.image.ImageStore
import com.example.homestorage.ui.common.ImagePicker
import com.example.homestorage.ui.common.PrimaryButton
import com.example.homestorage.ui.common.SheetAction

/**
 * 户型图列表页：管理多张户型图（新增/重命名/删除），点击进入详情挂载柜子
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FloorPlanListScreen(
    onBack: () -> Unit,
    onOpenFloorPlan: (Long) -> Unit,
    viewModel: FloorPlanListViewModel = viewModel()
) {
    val floorPlans by viewModel.floorPlans.collectAsState()

    val context = LocalContext.current
    val imageStore = remember { ImageStore(context) }

    // 新增流程状态：来源选择 → 命名
    var showSourceDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var planName by remember { mutableStateOf("") }

    // 管理操作状态
    var menuPlan by remember { mutableStateOf<FloorPlanEntity?>(null) }
    var renamePlan by remember { mutableStateOf<FloorPlanEntity?>(null) }
    var deletePlan by remember { mutableStateOf<FloorPlanEntity?>(null) }

    // 相册选择
    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        showSourceDialog = false
        if (uri != null) {
            pendingImageUri = uri
            planName = ""
            showNameDialog = true
        }
    }

    // 拍照
    val launchCamera = ImagePicker.rememberCameraLauncher { uri ->
        showSourceDialog = false
        if (uri != null) {
            pendingImageUri = uri
            planName = ""
            showNameDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("户型图") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSourceDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加户型图") },
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 3.dp
                )
            )
        }
    ) { innerPadding ->
        if (floorPlans.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(120.dp))
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.padding(8.dp))
                Text(
                    "还没有户型图\n上传一张户型图，点击位置即可挂载柜子\n让「哪里放了什么」一目了然",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                items(floorPlans, key = { it.id }) { plan ->
                    FloorPlanCard(
                        plan = plan,
                        imageStore = imageStore,
                        onClick = { onOpenFloorPlan(plan.id) },
                        onLongClick = { menuPlan = plan }
                    )
                }
            }
        }
    }

    // ---- 图片来源选择（拍照/相册） ----
    if (showSourceDialog) {
        ModalBottomSheet(onDismissRequest = { showSourceDialog = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    "选择图片来源",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                SheetAction(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    text = "从相册选择",
                    onClick = {
                        pickGallery.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                SheetAction(
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    text = "拍照",
                    onClick = { launchCamera(ImagePicker.createCameraFile(context)) }
                )
            }
        }
    }

    // ---- 新增命名 ----
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("户型图名称") },
            text = {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    placeholder = { Text("如：一层 / 客厅区域") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImageUri
                        if (uri != null) {
                            viewModel.addFloorPlan(planName, uri) { id ->
                                onOpenFloorPlan(id)
                            }
                        }
                        showNameDialog = false
                    },
                    enabled = planName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("取消") }
            }
        )
    }

    // ---- 操作菜单 ----
    menuPlan?.let { plan ->
        ModalBottomSheet(onDismissRequest = { menuPlan = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = "重命名",
                    onClick = {
                        renamePlan = plan
                        planName = plan.name
                        menuPlan = null
                    }
                )
                SheetAction(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    text = "删除户型图",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        deletePlan = plan
                        menuPlan = null
                    }
                )
            }
        }
    }

    // ---- 重命名对话框 ----
    renamePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { renamePlan = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameFloorPlan(plan.id, planName)
                        renamePlan = null
                    },
                    enabled = planName.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renamePlan = null }) { Text("取消") }
            }
        )
    }

    // ---- 删除确认 ----
    deletePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { deletePlan = null },
            title = { Text("删除户型图？") },
            text = { Text("已挂载的柜子不会删除，仅解除挂载位置。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFloorPlan(plan.id)
                        deletePlan = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deletePlan = null }) { Text("取消") }
            }
        )
    }
}

/** 户型图卡片：图片缩略图 + 名称，长按弹操作菜单 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloorPlanCard(
    plan: FloorPlanEntity,
    imageStore: ImageStore,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AsyncImage(
                    model = imageStore.getFile(plan.imagePath),
                    contentDescription = plan.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}