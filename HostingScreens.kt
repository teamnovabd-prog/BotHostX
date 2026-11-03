package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.models.CreateHostingRequestDto
import com.example.data.models.HostingDto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.UserPortalUiState

private fun hostingTypeLabel(raw: String): String = when (raw) {
    "python_bot" -> "Python Bot"
    "telegram_bot" -> "Telegram Bot"
    "php_script" -> "PHP 8.3 Hosting"
    "html_web" -> "HTML Website"
    else -> raw
}

@Composable
fun HostingListScreen(
    portalState: UserPortalUiState,
    onLoadHosting: (String?) -> Unit,
    onCreateHosting: () -> Unit,
    onSelectHosting: (Int) -> Unit,
    onConfigureServer: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedFilter) {
        onLoadHosting(selectedFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("hosting_list_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("My Cloud Hosting", style = MaterialTheme.typography.headlineSmall)
                FilledTonalButton(
                    onClick = onCreateHosting,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("hosting_top_create_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Instance")
                }
            }
        }

        // Hosting Type Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val filters = listOf(
                    null to "All",
                    "python_bot" to "Python Bot",
                    "telegram_bot" to "Telegram Bot",
                    "php_script" to "PHP Script",
                    "html_web" to "HTML Site",
                )
                filters.forEach { (typeKey, label) ->
                    FilterChip(
                        selected = selectedFilter == typeKey,
                        onClick = { selectedFilter = typeKey },
                        label = { Text(label) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("filter_${typeKey ?: "all"}"),
                    )
                }
            }
        }

        if (portalState.isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        if (!portalState.errorMessage.isNullOrBlank()) {
            item {
                ErrorBanner(
                    message = portalState.errorMessage,
                    onRetry = { onLoadHosting(selectedFilter) },
                    onConfigureServer = onConfigureServer,
                )
            }
        }

        if (!portalState.isLoading && portalState.hostingList.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.CloudQueue,
                    title = "No Hosting Instances Found",
                    subtitle = "Create an isolated Python Bot, Telegram Bot, PHP 8.3 Script, or HTML Website instance.",
                    actionLabel = "Create Hosting",
                    onAction = onCreateHosting,
                )
            }
        } else {
            items(portalState.hostingList, key = { it.id }) { item ->
                HostingItemCard(item = item, onClick = { onSelectHosting(item.id) })
            }
        }

        item { Spacer(Modifier.height(76.dp)) }
    }
}

@Composable
private fun HostingItemCard(item: HostingDto, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("hosting_card_${item.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${hostingTypeLabel(item.hosting_type)} • ${item.assigned_vps_name ?: "Pending VPS"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(status = item.status)
            }

            if (!item.public_url.isNullOrBlank()) {
                Text(
                    text = item.public_url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "CPU: ${item.cpu_usage_percent}% / ${item.cpu_limit_cores}vCPU",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "RAM: ${item.ram_used_mb}/${item.ram_limit_mb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Exp: ${FormatUtils.shortDate(item.expires_at).take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHostingScreen(
    portalState: UserPortalUiState,
    onSubmit: (CreateHostingRequestDto) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var name by remember { mutableStateOf("") }
    var hostingType by remember { mutableStateOf("telegram_bot") }
    var entryFile by remember { mutableStateOf("main.py") }
    var subdomainSlug by remember { mutableStateOf("") }
    var botToken by remember { mutableStateOf("") }
    var envText by remember { mutableStateOf("") }
    var webhookEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Cloud Hosting") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("1. Choose Isolated Runtime Type", style = MaterialTheme.typography.titleMedium)

            val types = listOf(
                "telegram_bot" to "Telegram Bot (Python)",
                "python_bot" to "Python Bot / Worker",
                "php_script" to "PHP 8.3 Script Hosting",
                "html_web" to "HTML Static Website",
            )
            types.forEach { (key, title) ->
                val selected = hostingType == key
                Card(
                    onClick = {
                        hostingType = key
                        entryFile = when (key) {
                            "php_script" -> "index.php"
                            "html_web" -> "index.html"
                            else -> "main.py"
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("select_type_$key"),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        RadioButton(selected = selected, onClick = null)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hosting Instance Name") },
                placeholder = { Text("e.g., SupportAssistantBot or PortfolioSite") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("create_hosting_name_input"),
            )

            OutlinedTextField(
                value = entryFile,
                onValueChange = { entryFile = it },
                label = { Text("Entry File") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("create_hosting_entry_input"),
            )

            if (hostingType in listOf("php_script", "html_web")) {
                OutlinedTextField(
                    value = subdomainSlug,
                    onValueChange = { subdomainSlug = it },
                    label = { Text("Custom Subdomain Prefix (Optional)") },
                    placeholder = { Text("my-app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_hosting_slug_input"),
                )
            }

            if (hostingType == "telegram_bot") {
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    label = { Text("Telegram Bot Token (Encrypted at Rest)") },
                    placeholder = { Text("123456789:AAH...") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_hosting_bot_token_input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unique Secure Webhook Endpoint", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Leave off for standard Telegram polling bots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = webhookEnabled, onCheckedChange = { webhookEnabled = it })
                }
            }

            OutlinedTextField(
                value = envText,
                onValueChange = { envText = it },
                label = { Text("Environment Variables (KEY=VALUE per line)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("create_hosting_env_input"),
            )

            if (!portalState.errorMessage.isNullOrBlank()) {
                ErrorBanner(message = portalState.errorMessage)
            }

            Button(
                onClick = {
                    val parsedEnv = envText.lines()
                        .mapNotNull { line ->
                            val idx = line.indexOf('=')
                            if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
                        }
                        .toMap()
                    onSubmit(
                        CreateHostingRequestDto(
                            name = name.trim(),
                            hosting_type = hostingType,
                            subdomain_slug = subdomainSlug.trim().ifBlank { null },
                            entry_file = entryFile.trim().ifBlank { "main.py" },
                            bot_token = botToken.trim().ifBlank { null },
                            env_vars = parsedEnv,
                            webhook_enabled = webhookEnabled,
                        )
                    )
                },
                enabled = !portalState.isMutating && name.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_create_hosting_button"),
            ) {
                if (portalState.isMutating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text("Provision Isolated Container")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostingDetailScreen(
    hostingId: Int,
    portalState: UserPortalUiState,
    onRefresh: () -> Unit,
    onAction: (String) -> Unit,
    onSaveBotConfig: (String, String?, Map<String, String>, Boolean) -> Unit,
    onOpenFiles: () -> Unit,
    onOpenLogs: () -> Unit,
    onUpgradePlan: () -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBotConfigDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hostingId) {
        onRefresh()
    }

    val hosting = portalState.selectedHosting

    if (showDeleteConfirm && hosting != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete '${hosting.name}'?") },
            text = { Text("This stops and permanently deletes the isolated Docker container and all hosted files.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
                    onClick = {
                        showDeleteConfirm = false
                        onAction("delete")
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_delete_hosting_button"),
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showBotConfigDialog && hosting != null) {
        var entry by remember { mutableStateOf(hosting.entry_file) }
        var newBotToken by remember { mutableStateOf("") }
        var envInput by remember { mutableStateOf("") }
        var webhook by remember { mutableStateOf(hosting.webhook_enabled) }

        AlertDialog(
            onDismissRequest = { showBotConfigDialog = false },
            title = { Text("Runtime & Secret Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!hosting.bot_token_masked.isNullOrBlank()) {
                        Text(
                            "Saved Token (Masked): ${hosting.bot_token_masked}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    OutlinedTextField(
                        value = entry,
                        onValueChange = { entry = it },
                        label = { Text("Entry Script") },
                        singleLine = true,
                    )
                    if (hosting.hosting_type in listOf("telegram_bot", "python_bot")) {
                        OutlinedTextField(
                            value = newBotToken,
                            onValueChange = { newBotToken = it },
                            label = { Text("Replace Bot Token (Leave blank to keep)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = envInput,
                        onValueChange = { envInput = it },
                        label = { Text("KEY=VALUE Env Vars (one per line)") },
                        minLines = 3,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = webhook, onCheckedChange = { webhook = it })
                        Text("Enable Secure Webhook Endpoint", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val envMap = envInput.lines().mapNotNull { ln ->
                            val i = ln.indexOf('=')
                            if (i > 0) ln.substring(0, i).trim() to ln.substring(i + 1).trim() else null
                        }.toMap()
                        onSaveBotConfig(entry, newBotToken.ifBlank { null }, envMap, webhook)
                        showBotConfigDialog = false
                    },
                ) {
                    Text("Save Securely")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBotConfigDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hosting?.name ?: "Hosting Control Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (portalState.isLoading || portalState.isMutating) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (!portalState.actionMessage.isNullOrBlank()) {
                item { FeedbackBanner(portalState.actionMessage, onDismiss = onDismissMessage) }
            }
            if (!portalState.errorMessage.isNullOrBlank()) {
                item { ErrorBanner(portalState.errorMessage, onRetry = onRefresh) }
            }

            if (hosting != null) {
                // Status & Metadata Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(hostingTypeLabel(hosting.hosting_type), style = MaterialTheme.typography.titleLarge)
                                StatusBadge(hosting.status)
                            }
                            Text("Plan: ${hosting.plan_name ?: "Standard"} • Assigned VPS: ${hosting.assigned_vps_name ?: "Unassigned"} (${hosting.assigned_vps_region ?: "N/A"})", style = MaterialTheme.typography.bodySmall)
                            Text("Uptime: ${FormatUtils.formatUptime(hosting.uptime_seconds)} • Expires: ${FormatUtils.shortDate(hosting.expires_at)}", style = MaterialTheme.typography.bodySmall)
                            if (!hosting.php_version.isNullOrBlank()) {
                                Text("PHP Runtime Version: ${hosting.php_version}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!hosting.bot_token_masked.isNullOrBlank()) {
                                Text("Protected Bot Token: ${hosting.bot_token_masked}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (!hosting.public_url.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(hosting.public_url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("preview_website_button"),
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Preview: ${hosting.public_url}")
                                }
                            }
                        }
                    }
                }

                // Lifecycle Controls: Start, Stop, Restart
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onAction("start") },
                            enabled = !portalState.isMutating && hosting.status != "Running",
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRunningGreen),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("hosting_start_button"),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                        Button(
                            onClick = { onAction("stop") },
                            enabled = !portalState.isMutating && hosting.status == "Running",
                            colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("hosting_stop_button"),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                        FilledTonalButton(
                            onClick = { onAction("restart") },
                            enabled = !portalState.isMutating,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("hosting_restart_button"),
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Restart")
                        }
                    }
                }

                // Resource Gauges
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Isolated Container Telemetry", style = MaterialTheme.typography.titleMedium)
                            ResourceUsageBar(
                                label = "CPU Limit (${hosting.cpu_limit_cores} vCPU)",
                                valueText = "${hosting.cpu_usage_percent}%",
                                progressFraction = (hosting.cpu_usage_percent / 100.0).toFloat(),
                            )
                            ResourceUsageBar(
                                label = "Memory",
                                valueText = "${hosting.ram_used_mb} / ${hosting.ram_limit_mb} MB",
                                progressFraction = hosting.ram_used_mb.toFloat() / hosting.ram_limit_mb.coerceAtLeast(1),
                                accentColor = CyberCyan,
                            )
                            ResourceUsageBar(
                                label = "Isolated Disk",
                                valueText = "${hosting.storage_used_mb} / ${hosting.storage_limit_mb} MB",
                                progressFraction = (hosting.storage_used_mb / hosting.storage_limit_mb.coerceAtLeast(1)).toFloat(),
                                accentColor = StatusRunningGreen,
                            )
                        }
                    }
                }

                // Management Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenFiles,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("manage_files_button"),
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open File Manager & Upload Code")
                        }

                        OutlinedButton(
                            onClick = onOpenLogs,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("view_logs_button"),
                        ) {
                            Icon(Icons.Outlined.Terminal, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Live Console & Error Logs")
                        }

                        if (hosting.hosting_type in listOf("python_bot", "telegram_bot")) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onAction("install_deps") },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("install_deps_button"),
                                ) {
                                    Text("pip requirements.txt")
                                }
                                OutlinedButton(
                                    onClick = { showBotConfigDialog = true },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("bot_config_button"),
                                ) {
                                    Text("Bot Token / .env")
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = onUpgradePlan,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("upgrade_plan_button"),
                            ) {
                                Text("Upgrade Plan")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("delete_hosting_button"),
                            ) {
                                Text("Delete Hosting")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostingLogsScreen(
    hostingId: Int,
    portalState: UserPortalUiState,
    onLoadLogs: (String?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var logFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hostingId, logFilter) {
        onLoadLogs(logFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Container Logs (#$hostingId)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onLoadLogs(logFilter) },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_logs_button"),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh Logs")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "All", "console" to "Console", "error" to "Errors", "system" to "System").forEach { (key, label) ->
                    FilterChip(
                        selected = logFilter == key,
                        onClick = { logFilter = key },
                        label = { Text(label) },
                    )
                }
            }

            if (portalState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = DarkNavy950,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (portalState.logs.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("No log lines recorded yet.", color = TextLightSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(portalState.logs, key = { it.id }) { entry ->
                            val lineColor = when (entry.log_type) {
                                "error" -> StatusErrorRed
                                "system" -> CyberCyan
                                else -> TextLightPrimary
                            }
                            Text(
                                text = "[${FormatUtils.shortDate(entry.created_at)}] [${entry.log_type.uppercase()}] ${entry.message}",
                                style = MaterialTheme.typography.labelSmall,
                                color = lineColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
