package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.models.VpsCreateUpdateRequestDto
import com.example.data.models.VpsDto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.AdminUiState

@Composable
fun AdminVpsManagementSection(
    adminState: AdminUiState,
    onRefreshCluster: () -> Unit,
    onAddOrUpdateVps: (Int?, VpsCreateUpdateRequestDto) -> Unit,
    onTestVps: (Int) -> Unit,
    onOpenVpsDetail: (Int) -> Unit,
) {
    var showVpsFormDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefreshCluster()
    }

    if (showVpsFormDialog) {
        VpsFormDialog(
            existing = null,
            isSaving = adminState.isMutating,
            onSave = { req ->
                onAddOrUpdateVps(null, req)
                showVpsFormDialog = false
            },
            onDismiss = { showVpsFormDialog = false },
        )
    }

    val sum = adminState.vpsSummary

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("VPS Management", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Provision real Linux servers & verify Docker health before hosting assignment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { showVpsFormDialog = true },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("add_vps_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add VPS")
            }
        }

        // Cluster Status Grid
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            KpiStatCard("Total VPS", "${sum.total_vps}", Icons.Outlined.Dns, ElectricBlue, Modifier.weight(1f))
            KpiStatCard("Online", "${sum.online_vps}", Icons.Outlined.CheckCircle, StatusRunningGreen, Modifier.weight(1f))
            KpiStatCard("Offline/Maint", "${sum.offline_vps}", Icons.Outlined.PauseCircle, StatusWarningAmber, Modifier.weight(1f))
            KpiStatCard("Unreachable", "${sum.unreachable_vps}", Icons.Outlined.ErrorOutline, StatusErrorRed, Modifier.weight(1f))
        }

        // Cluster Capacity Card
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Aggregate Cluster Capacity & Containers", style = MaterialTheme.typography.titleMedium)
                Text(
                    "CPU: ${sum.available_cpu_cores} / ${sum.total_cpu_cores} vCPU free • " +
                        "RAM: ${sum.available_ram_mb} / ${sum.total_ram_mb} MB free • " +
                        "Storage: ${FormatUtils.formatStorageMb(sum.available_storage_mb)} / ${FormatUtils.formatStorageMb(sum.total_storage_mb)} free",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Hosted Containers: ${sum.total_hosted_containers} • Active Running Hosting: ${sum.total_running_hosting}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (adminState.vpsList.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Dns,
                title = "No VPS Servers Registered Yet",
                subtitle = "Purchase/provision a Linux VPS separately, install Docker & the SmartHost VPS Agent (or SSH key), then add and verify it here.",
                actionLabel = "Add First VPS Server",
                onAction = { showVpsFormDialog = true },
            )
        } else {
            adminState.vpsList.forEach { vps ->
                VpsServerCard(
                    vps = vps,
                    onTestConnection = { onTestVps(vps.id) },
                    onOpenDetail = { onOpenVpsDetail(vps.id) },
                )
            }
        }
    }
}

@Composable
private fun VpsServerCard(
    vps: VpsDto,
    onTestConnection: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("vps_card_${vps.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${vps.name} (${vps.provider})", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${vps.ip_address} • ${vps.region} • ${vps.os_name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(vps.connection_status)
            }

            if (!vps.last_error.isNullOrBlank()) {
                Text(
                    text = "Error: ${vps.last_error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusErrorRed,
                )
            }

            ResourceUsageBar(
                label = "CPU (${vps.available_cpu_cores}/${vps.cpu_capacity_cores} vCPU free)",
                valueText = "${vps.cpu_usage_percent}%",
                progressFraction = (vps.cpu_usage_percent / 100.0).toFloat(),
            )
            ResourceUsageBar(
                label = "RAM (${vps.available_ram_mb}/${vps.ram_capacity_mb} MB free)",
                valueText = "${vps.ram_used_mb} MB used",
                progressFraction = vps.ram_used_mb.toFloat() / vps.ram_capacity_mb.coerceAtLeast(1),
                accentColor = CyberCyan,
            )
            ResourceUsageBar(
                label = "Disk (${FormatUtils.formatStorageMb(vps.available_storage_mb)} free)",
                valueText = "${FormatUtils.formatStorageMb(vps.disk_used_mb)} used",
                progressFraction = vps.disk_used_mb.toFloat() / vps.storage_capacity_mb.coerceAtLeast(1),
                accentColor = StatusRunningGreen,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hosting: ${vps.hosting_count}/${vps.max_hosting_count} • Check: ${FormatUtils.shortDate(vps.last_health_check_at)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTestConnection,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("test_vps_${vps.id}"),
                    ) { Text("Verify") }
                    Button(
                        onClick = onOpenDetail,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("manage_vps_${vps.id}"),
                    ) { Text("Manage") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVpsDetailScreen(
    vpsId: Int,
    adminState: AdminUiState,
    onLoadDetail: () -> Unit,
    onRunAction: (String, Boolean) -> Unit,
    onUpdateVps: (VpsCreateUpdateRequestDto) -> Unit,
    onRemoveVps: () -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var showEditDialog by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(vpsId) { onLoadDetail() }

    val vps = adminState.selectedVps

    if (showEditDialog && vps != null) {
        VpsFormDialog(
            existing = vps,
            isSaving = adminState.isMutating,
            onSave = { req ->
                onUpdateVps(req)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
        )
    }

    if (showRemoveConfirm && vps != null) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove VPS '${vps.name}'?") },
            text = {
                if (vps.hosting_count > 0) {
                    Text(
                        "Removal Blocked: ${vps.hosting_count} hosting instance(s) are currently assigned to this server. Migrate or delete assigned hosting before removing this VPS.",
                        color = StatusErrorRed,
                    )
                } else {
                    Text("Zero hosting instances are assigned to '${vps.name}'. Confirm permanent removal of this VPS server?")
                }
            },
            confirmButton = {
                if (vps.hosting_count == 0) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = StatusErrorRed),
                        onClick = {
                            showRemoveConfirm = false
                            onRemoveVps()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_remove_vps_button"),
                    ) { Text("Remove VPS") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Close") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vps?.name ?: "VPS Details") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (vps != null) {
                        IconButton(onClick = { showEditDialog = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit VPS")
                        }
                        IconButton(onClick = { showRemoveConfirm = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove VPS", tint = StatusErrorRed)
                        }
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
            if (adminState.isLoading || adminState.isMutating) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!adminState.actionMessage.isNullOrBlank()) {
                item { FeedbackBanner(adminState.actionMessage, onDismiss = onDismissMessage) }
            }
            if (!adminState.errorMessage.isNullOrBlank()) {
                item { ErrorBanner(adminState.errorMessage, onRetry = onLoadDetail) }
            }

            if (vps != null) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${vps.provider} • ${vps.region}", style = MaterialTheme.typography.titleMedium)
                                StatusBadge(vps.connection_status)
                            }
                            Text("IP: ${vps.ip_address}:${vps.ssh_port} • Auth: ${vps.auth_method} • Docker Verified: ${if (vps.docker_available) "Yes" else "No"}", style = MaterialTheme.typography.labelSmall)
                            Text("OS: ${vps.os_name} • Uptime: ${FormatUtils.formatUptime(vps.uptime_seconds)} • Encrypted Credentials Stored: ${if (vps.has_credentials_configured) "Yes" else "Missing"}", style = MaterialTheme.typography.bodySmall)
                            if (!vps.last_error.isNullOrBlank()) {
                                Text("Connection Diagnostic: ${vps.last_error}", style = MaterialTheme.typography.bodySmall, color = StatusErrorRed)
                            }
                        }
                    }
                }

                // Connection & State Controls
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onRunAction("test", false) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("vps_detail_test_button"),
                            ) { Text("Test Health") }
                            FilledTonalButton(
                                onClick = { onRunAction("connect", false) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text("Connect") }
                            OutlinedButton(
                                onClick = { onRunAction("disconnect", false) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text("Disconnect") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onRunAction(if (vps.is_enabled) "disable" else "enable", false) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(if (vps.is_enabled) "Disable VPS" else "Enable VPS") }
                            OutlinedButton(
                                onClick = { onRunAction("maintenance", !vps.maintenance_mode) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(if (vps.maintenance_mode) "Exit Maintenance" else "Set Maintenance") }
                        }
                    }
                }

                // Assigned Hosting on this VPS
                item {
                    Text("Assigned Hosting Instances (${adminState.vpsHosting.size})", style = MaterialTheme.typography.titleMedium)
                }
                if (adminState.vpsHosting.isEmpty()) {
                    item {
                        Text("No user hosting instances are currently assigned to this VPS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(adminState.vpsHosting, key = { it.id }) { h ->
                        Card(shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("#${h.id} ${h.name} (${h.hosting_type})", style = MaterialTheme.typography.titleSmall)
                                    Text("User #${h.user_id} • ${h.cpu_limit_cores}vCPU / ${h.ram_limit_mb}MB", style = MaterialTheme.typography.labelSmall)
                                }
                                StatusBadge(h.status)
                            }
                        }
                    }
                }

                // Operation Audit Logs (secrets never logged!)
                item {
                    Text("VPS Administrative Operation Logs", style = MaterialTheme.typography.titleMedium)
                }
                items(adminState.vpsLogs, key = { it.id }) { lg ->
                    Surface(shape = MaterialTheme.shapes.extraSmall, color = DarkNavy950, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "[${FormatUtils.shortDate(lg.created_at)}] [${lg.level}] ${lg.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lg.level == "ERROR") StatusErrorRed else TextLightPrimary,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VpsFormDialog(
    existing: VpsDto?,
    isSaving: Boolean,
    onSave: (VpsCreateUpdateRequestDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var provider by remember { mutableStateOf(existing?.provider ?: "DigitalOcean") }
    var ipAddress by remember { mutableStateOf(existing?.ip_address.orEmpty()) }
    var sshPort by remember { mutableStateOf((existing?.ssh_port ?: 22).toString()) }
    var sshUsername by remember { mutableStateOf(existing?.ssh_username ?: "root") }
    var authMethod by remember { mutableStateOf(existing?.auth_method ?: "vps_agent") }
    var agentUrl by remember { mutableStateOf(existing?.agent_url.orEmpty()) }
    var secretCredential by remember { mutableStateOf("") }
    var hostFingerprint by remember { mutableStateOf("") }
    var osName by remember { mutableStateOf(existing?.os_name ?: "Ubuntu 24.04 LTS") }
    var region by remember { mutableStateOf(existing?.region ?: "Singapore") }
    var cpuCores by remember { mutableStateOf((existing?.cpu_capacity_cores ?: 4.0).toString()) }
    var ramMb by remember { mutableStateOf((existing?.ram_capacity_mb ?: 8192).toString()) }
    var storageMb by remember { mutableStateOf((existing?.storage_capacity_mb ?: 81920).toString()) }
    var maxHosting by remember { mutableStateOf((existing?.max_hosting_count ?: 30).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Register New Linux VPS" else "Edit VPS Server") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("VPS Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("vps_name_input"))
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider (e.g., Hetzner, Vultr)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ipAddress, onValueChange = { ipAddress = it }, label = { Text("Public IP Address") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("vps_ip_input"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sshPort, onValueChange = { sshPort = it }, label = { Text("SSH Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sshUsername, onValueChange = { sshUsername = it }, label = { Text("SSH User") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Text("Authentication Method", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("vps_agent" to "Agent", "ssh_key" to "SSH Key", "ssh_password" to "SSH Pass").forEach { (key, lbl) ->
                        FilterChip(selected = authMethod == key, onClick = { authMethod = key }, label = { Text(lbl) })
                    }
                }

                if (authMethod == "vps_agent") {
                    OutlinedTextField(value = agentUrl, onValueChange = { agentUrl = it }, label = { Text("Agent HTTPS URL (Optional)") }, placeholder = { Text("https://ip:9443") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = secretCredential, onValueChange = { secretCredential = it }, label = { Text("VPS Agent Secret Token (Write-Only)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(value = secretCredential, onValueChange = { secretCredential = it }, label = { Text("SSH Private Key / Password (Encrypted at Rest)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hostFingerprint, onValueChange = { hostFingerprint = it }, label = { Text("Pinned SSH Host Key SHA256 Fingerprint") }, placeholder = { Text("SHA256:...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = region, onValueChange = { region = it }, label = { Text("Region") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = osName, onValueChange = { osName = it }, label = { Text("OS") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cpuCores, onValueChange = { cpuCores = it }, label = { Text("CPU Cores") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ramMb, onValueChange = { ramMb = it }, label = { Text("RAM (MB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = storageMb, onValueChange = { storageMb = it }, label = { Text("Disk (MB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxHosting, onValueChange = { maxHosting = it }, label = { Text("Max Hosts") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VpsCreateUpdateRequestDto(
                            name = name.trim(),
                            provider = provider.trim(),
                            ip_address = ipAddress.trim(),
                            ssh_port = sshPort.toIntOrNull() ?: 22,
                            ssh_username = sshUsername.trim().ifBlank { "root" },
                            auth_method = authMethod,
                            agent_url = agentUrl.trim().ifBlank { null },
                            secret_credential = secretCredential.ifBlank { null },
                            agent_token = if (authMethod == "vps_agent") secretCredential.ifBlank { null } else null,
                            host_key_fingerprint = hostFingerprint.trim().ifBlank { null },
                            os_name = osName.trim(),
                            region = region.trim(),
                            cpu_capacity_cores = cpuCores.toDoubleOrNull() ?: 2.0,
                            ram_capacity_mb = ramMb.toIntOrNull() ?: 4096,
                            storage_capacity_mb = storageMb.toIntOrNull() ?: 40960,
                            max_hosting_count = maxHosting.toIntOrNull() ?: 25,
                        )
                    )
                },
                enabled = !isSaving && name.length >= 2 && ipAddress.length >= 3,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("submit_vps_form_button"),
            ) {
                Text("Save VPS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
