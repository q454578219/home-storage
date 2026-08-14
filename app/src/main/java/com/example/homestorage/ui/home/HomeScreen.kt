package com.example.homestorage.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import android.content.Intent
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.homestorage.data.backup.BackupManager
import com.example.homestorage.data.db.CabinetWithCategory
import com.example.homestorage.data.db.CategoryWithCount
import com.example.homestorage.data.db.SearchItemHit
import com.example.homestorage.data.image.ImageStore
import com.example.homestorage.data.repo.HomeRepository
import com.example.homestorage.data.repo.SearchResult
import com.example.homestorage.ui.common.PrimaryButton
import com.example.homestorage.ui.common.SheetAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

/** 首页：柜子网格 + 分类筛选 + 搜索 + 管理操作 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenCabinet: (Long) -> Unit,
    onOpenItem: (Long, Long) -> Unit,
    onOpenCreateCabinet: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val cabinets by viewModel.cabinets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val recentItems by viewModel.recentItems.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()

    val context = LocalContext.current
    val imageStore = remember { ImageStore(context) }

    // 管理操作对话框状态
    var menuCabinet by remember { mutableStateOf<CabinetWithCategory?>(null) }
    var renameCabinet by remember { mutableStateOf<CabinetWithCategory?>(null) }
    var categoryCabinet by remember { mutableStateOf<CabinetWithCategory?>(null) }
    var deleteCabinet by remember { mutableStateOf<CabinetWithCategory?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showRecycleBin by remember { mutableStateOf(false) }
    var purgeTarget by remember { mutableStateOf<CabinetWithCategory?>(null) }
    var showBackupMenu by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 换封面：系统相册选择
    val pickCover = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            menuCabinet?.let { viewModel.replaceCabinetCover(it, uri) }
        }
        menuCabinet = null
    }

    // 导入备份：选择 ZIP 文件
    val pickBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            restoring = true
            scope.launch(Dispatchers.IO) {
                val ok = BackupManager.restoreBackup(context, uri)
                HomeRepository.resetForRestore()
                withContext(Dispatchers.Main) {
                    restoring = false
                    Toast.makeText(
                        context,
                        if (ok) "恢复成功，正在重启…" else "恢复失败：备份文件无效",
                        Toast.LENGTH_LONG
                    ).show()
                    if (ok) {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreateCabinet,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建柜子") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 大标题区 + 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "我的储物",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (cabinets.isEmpty()) "还没有柜子，点击下方按钮开始"
                        else "共 ${cabinets.size} 个柜子",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showBackupMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                IconButton(onClick = { showRecycleBin = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "回收站")
                }
            }

            // 搜索框（胶囊形）
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("搜索物品 / 柜子…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchKeyword.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (searchKeyword.isBlank()) {
                // 分类筛选
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("全部") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = {
                                Text(
                                    if (category.itemCount > 0) "${category.name} (${category.itemCount})"
                                    else category.name
                                )
                            }
                        )
                    }
                }

                // 最近添加（仅"全部"时展示，点击直达柜子点位）
                if (selectedCategoryId == null && recentItems.isNotEmpty()) {
                    RecentItemsRow(
                        items = recentItems,
                        imageStore = imageStore,
                        onOpenItem = { hit -> onOpenItem(hit.cabinetId, hit.spotId) }
                    )
                }

                // 柜子网格
                if (cabinets.isEmpty()) {
                    EmptyCabinetHint(onOpenCreateCabinet)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cabinets, key = { it.id }) { cabinet ->
                            CabinetCard(
                                cabinet = cabinet,
                                imageStore = imageStore,
                                onClick = { onOpenCabinet(cabinet.id) },
                                onLongPress = { menuCabinet = cabinet }
                            )
                        }
                    }
                }
            } else {
                // 搜索结果
                SearchResultList(
                    result = searchResult,
                    imageStore = imageStore,
                    onOpenItem = { hit -> onOpenItem(hit.cabinetId, hit.spotId) },
                    onOpenCabinet = onOpenCabinet
                )
            }
        }
    }

    // ---- 管理操作菜单 ----
    menuCabinet?.let { cabinet ->
        CabinetMenuDialog(
            cabinetName = cabinet.name,
            onDismiss = { menuCabinet = null },
            onRename = { renameText = cabinet.name; renameCabinet = cabinet; menuCabinet = null },
            onChangeCategory = { categoryCabinet = cabinet; menuCabinet = null },
            onChangeCover = {
                pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDelete = { deleteCabinet = cabinet; menuCabinet = null }
        )
    }

    // ---- 更多菜单（备份/恢复） ----
    Box {
        DropdownMenu(
            expanded = showBackupMenu,
            onDismissRequest = { showBackupMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("导出备份到下载目录") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    showBackupMenu = false
                    scope.launch(Dispatchers.IO) {
                        val uri = BackupManager.exportBackup(context)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (uri != null) "备份已导出到「下载」目录"
                                else "导出失败：没有可备份的数据",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("从备份文件恢复") },
                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                onClick = {
                    showBackupMenu = false
                    pickBackup.launch(arrayOf("application/zip", "application/octet-stream"))
                }
            )
        }
    }

    // 恢复中提示
    if (restoring) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("恢复备份") },
            text = { Text("正在恢复，请稍候…") },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // ---- 回收站弹窗 ----
    if (showRecycleBin) {
        RecycleBinDialog(
            cabinets = viewModel.recycleBin.collectAsState().value,
            onDismiss = { showRecycleBin = false },
            onRestore = { viewModel.restoreCabinet(it) },
            onPurge = { purgeTarget = it }
        )
    }

    // ---- 彻底删除确认 ----
    purgeTarget?.let { cabinet ->
        AlertDialog(
            onDismissRequest = { purgeTarget = null },
            title = { Text("彻底删除") },
            text = { Text("将永久删除「${cabinet.name}」及其全部点位和物品照片，此操作不可恢复。确定？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.purgeCabinet(cabinet.id)
                    purgeTarget = null
                }) { Text("彻底删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { purgeTarget = null }) { Text("取消") }
            }
        )
    }

    // ---- 重命名对话框 ----
    renameCabinet?.let { cabinet ->
        AlertDialog(
            onDismissRequest = { renameCabinet = null },
            title = { Text("重命名柜子") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameCabinet(cabinet, renameText)
                    renameCabinet = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameCabinet = null }) { Text("取消") }
            }
        )
    }

    // ---- 修改分类对话框 ----
    categoryCabinet?.let { cabinet ->
        CategoryChoiceDialog(
            categories = categories,
            onDismiss = { categoryCabinet = null },
            onPick = { categoryId ->
                viewModel.changeCabinetCategory(cabinet, categoryId)
                categoryCabinet = null
            }
        )
    }

    // ---- 删除确认对话框 ----
    deleteCabinet?.let { cabinet ->
        AlertDialog(
            onDismissRequest = { deleteCabinet = null },
            title = { Text("删除柜子") },
            text = { Text("确定删除「${cabinet.name}」？其全部点位和物品照片将一并删除，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCabinet(cabinet)
                    deleteCabinet = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCabinet = null }) { Text("取消") }
            }
        )
    }
}

/** 空状态提示 */
@Composable
private fun EmptyCabinetHint(onOpenCreateCabinet: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Kitchen,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("还没有柜子", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "拍一张柜子照片，就能开始收纳啦",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "新建柜子",
                icon = Icons.Default.Add,
                onClick = onOpenCreateCabinet
            )
        }
    }
}

/** 柜子卡片（封面 + 名称 + 分类标签，主流图片卡片风格：轻阴影、大圆角、图片无裁切边） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CabinetCard(
    cabinet: CabinetWithCategory,
    imageStore: ImageStore,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                val file = cabinet.coverImagePath?.let { imageStore.getFile(it) }
                if (file != null) {
                    AsyncImage(
                        model = file,
                        contentDescription = cabinet.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Kitchen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
                }
                // 物品数徽标（右下角半透明胶囊）
                if (cabinet.itemCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.45f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${cabinet.itemCount} 件物品",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            // 文字区固定高度，保证网格中所有卡片高度一致
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    cabinet.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cabinet.categoryName != null) {
                    Text(
                        cabinet.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** 柜子管理操作菜单（底部抽屉：主流 App 长按菜单样式） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabinetMenuDialog(
    cabinetName: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onChangeCategory: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                cabinetName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            SheetAction(
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                text = "重命名",
                onClick = { onDismiss(); onRename() }
            )
            SheetAction(
                icon = { Icon(Icons.Default.Label, contentDescription = null) },
                text = "修改分类",
                onClick = { onDismiss(); onChangeCategory() }
            )
            SheetAction(
                icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                text = "更换封面",
                onClick = { onDismiss(); onChangeCover() }
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
            SheetAction(
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                text = "删除柜子",
                color = MaterialTheme.colorScheme.error,
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

/** 最近添加栏：横向滚动的图片卡片，点击直达对应柜子点位 */
@Composable
private fun RecentItemsRow(
    items: List<SearchItemHit>,
    imageStore: ImageStore,
    onOpenItem: (SearchItemHit) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "最近添加",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.itemId }) { hit ->
                Card(
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { onOpenItem(hit) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                        ) {
                            if (hit.itemImagePath != null) {
                                AsyncImage(
                                    model = imageStore.getFile(hit.itemImagePath),
                                    contentDescription = hit.itemName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                        Text(
                            text = hit.itemName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)
                        )
                        Text(
                            text = hit.cabinetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 回收站弹窗：列出已删除柜子，支持恢复/彻底删除 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecycleBinDialog(
    cabinets: List<CabinetWithCategory>,
    onDismiss: () -> Unit,
    onRestore: (Long) -> Unit,
    onPurge: (CabinetWithCategory) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "回收站",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (cabinets.isEmpty()) {
                Text(
                    "回收站是空的",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            } else {
                cabinets.forEach { cabinet ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cabinet.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onRestore(cabinet.id) }) {
                            Text("恢复", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { onPurge(cabinet) }) {
                            Text("彻底删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/** 分类选择（底部抽屉） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChoiceDialog(
    categories: List<CategoryWithCount>,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "选择分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            SheetAction(
                icon = { Icon(Icons.Default.Label, contentDescription = null) },
                text = "未分类",
                onClick = { onDismiss(); onPick(null) }
            )
            categories.forEach { category ->
                SheetAction(
                    icon = { Icon(Icons.Default.Label, contentDescription = null) },
                    text = category.name,
                    onClick = { onDismiss(); onPick(category.id) }
                )
            }
        }
    }
}

/** 搜索结果列表（物品命中 + 柜子命中） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultList(
    result: SearchResult,
    imageStore: ImageStore,
    onOpenItem: (SearchItemHit) -> Unit,
    onOpenCabinet: (Long) -> Unit
) {
    if (result.items.isEmpty() && result.cabinets.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有找到匹配的内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (result.cabinets.isNotEmpty()) {
            item {
                Text(
                    "柜子",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(result.cabinets, key = { "c${it.cabinetId}" }) { hit ->
                ListItem(
                    headlineContent = { Text(hit.cabinetName) },
                    supportingContent = {
                        hit.categoryName?.let { Text(it) }
                    },
                    leadingContent = {
                        val file = hit.coverImagePath?.let { imageStore.getFile(it) }
                        if (file != null) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Kitchen,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onOpenCabinet(hit.cabinetId) })
                )
            }
        }

        if (result.items.isNotEmpty()) {
            item {
                Text(
                    "物品",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(result.items, key = { "i${it.itemId}" }) { hit ->
                ListItem(
                    headlineContent = { Text(hit.itemName) },
                    supportingContent = {
                        Text("${hit.cabinetName}${hit.categoryName?.let { " · $it" } ?: ""}")
                    },
                    leadingContent = {
                        val file = hit.itemImagePath?.let { imageStore.getFile(it) }
                        if (file != null) {
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onOpenItem(hit) })
                )
            }
        }
    }
}
