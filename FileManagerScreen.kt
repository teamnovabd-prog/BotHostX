package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.FileItemDto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.UserPortalUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    hostingId: Int,
    portalState: UserPortalUiState,
    onLoadFiles: (String, String?) -> Unit,
    onUploadUri: (android.net.Uri, Boolean) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFile: (String, String) -> Unit,
    onExtractZip: (String) -> Unit,
    onOpenEditor: (String) -> Unit,
    onCloseEditor: () -> Unit,
    onSaveEditor: (String, String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    BackHandler {
        if (portalState.editingFile != null) {
            onCloseEditor()
        } else if (portalState.currentDirectory.isNotBlank()) {
            val parent = portalState.currentDirectory.substringBeforeLast('/', "")
            onLoadFiles(parent, null)
        } else {
            onBack()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var autoExtractZipOnUpload by remember { mutableStateOf(true) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItemDto?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUploadUri(uri, autoExtractZipOnUpload)
        }
    }

    LaunchedEffect(hostingId) {
        onLoadFiles("", null)
    }

    // Full-screen in-app text editor for .py, .php, .html, .css, .js, .json, .txt, .env
    val editing = portalState.editingFile
    if (editing != null) {
        var draft by remember(editing.relative_path) { mutableStateOf(editing.content) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(editing.relative_path, style = MaterialTheme.typography.titleSmall) },
                    navigationIcon = {
                        IconButton(onClick = onCloseEditor, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Editor")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { onSaveEditor(editing.relative_path, draft) },
                            enabled = !portalState.isMutating,
                            modifier = Modifier.padding(end = 8.dp).minimumInteractiveComponentSize().testTag("save_file_editor_button"),
                        ) {
                            Text("Save")
                        }
                    },
                )
            },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { pad ->
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, lineHeight = 19.sp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(12.dp)
                    .testTag("code_editor_input"),
            )
        }
        return
    }

    if (showFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFolder(folderName)
                        showFolderDialog = false
                    },
                    enabled = folderName.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (renameTarget != null) {
        var newName by remember(renameTarget) { mutableStateOf(renameTarget!!.file_name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Item") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New File/Folder Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameFile(renameTarget!!.relative_path, newName)
                        renameTarget = null
                    },
                    enabled = newName.isNotBlank(),
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("File Manager")
                        Text(
                            text = "/${portalState.currentDirectory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFolderDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("create_folder_button"),
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = "New Folder")
                    }
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("upload_file_button"),
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "Upload File or ZIP")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Storage Bar + Auto-Extract Toggle
            item {
                val h = portalState.selectedHosting
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (h != null) {
                            ResourceUsageBar(
                                label = "Storage Quota",
                                valueText = "${h.storage_used_mb} / ${h.storage_limit_mb} MB",
                                progressFraction = (h.storage_used_mb / h.storage_limit_mb.coerceAtLeast(1)).toFloat(),
                                accentColor = ElectricBlue,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Supported: .py .php .html .css .js .json .txt .zip .env",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = autoExtractZipOnUpload, onCheckedChange = { autoExtractZipOnUpload = it })
                                Text("Unzip", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onLoadFiles(portalState.currentDirectory, it.ifBlank { null })
                    },
                    placeholder = { Text("Search files...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("file_search_input"),
                )
            }

            if (portalState.currentDirectory.isNotBlank()) {
                item {
                    OutlinedButton(
                        onClick = {
                            val parent = portalState.currentDirectory.substringBeforeLast('/', "")
                            onLoadFiles(parent, null)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                        Icon(Icons.Outlined.SubdirectoryArrowLeft, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Up to parent directory")
                    }
                }
            }

            if (portalState.isLoading || portalState.isMutating) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!portalState.actionMessage.isNullOrBlank()) {
                item { FeedbackBanner(portalState.actionMessage, onDismiss = onDismissMessage) }
            }
            if (!portalState.errorMessage.isNullOrBlank()) {
                item { ErrorBanner(portalState.errorMessage, onRetry = { onLoadFiles(portalState.currentDirectory, null) }) }
            }

            if (!portalState.isLoading && portalState.files.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.FolderOff,
                        title = "Directory Is Empty",
                        subtitle = "Upload Python, PHP, HTML, requirements.txt, or a project .zip archive from your phone.",
                        actionLabel = "Upload File / ZIP",
                        onAction = { filePickerLauncher.launch("*/*") },
                    )
                }
            } else {
                items(portalState.files, key = { it.id }) { file ->
                    FileRowItem(
                        item = file,
                        onOpen = {
                            if (file.is_directory) {
                                onLoadFiles(file.relative_path, null)
                            } else if (file.file_type != ".zip") {
                                onOpenEditor(file.relative_path)
                            }
                        },
                        onExtract = { onExtractZip(file.relative_path) },
                        onRename = { renameTarget = file },
                        onDelete = { onDeleteItem(file.relative_path) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileRowItem(
    item: FileItemDto,
    onOpen: () -> Unit,
    onExtract: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("file_item_${item.id}"),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = when {
                    item.is_directory -> Icons.Filled.Folder
                    item.file_type == ".zip" -> Icons.Outlined.FolderZip
                    else -> Icons.Outlined.Description
                },
                contentDescription = item.file_type,
                tint = if (item.is_directory) StatusWarningAmber else MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(item.file_name, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (item.is_directory) "Folder" else "${item.file_type} • ${FormatUtils.formatBytes(item.size_bytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.file_type == ".zip") {
                IconButton(onClick = onExtract, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Outlined.Unarchive, contentDescription = "Extract ZIP")
                }
            } else if (!item.is_directory) {
                IconButton(onClick = onOpen, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit File")
                }
            }
            IconButton(onClick = onRename, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = "Rename")
            }
            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = StatusErrorRed)
            }
        }
    }
}
