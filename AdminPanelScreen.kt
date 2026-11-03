package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.models.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.AdminUiState
import com.example.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    onOpenVpsDetail: (Int) -> Unit,
    onOpenTicketDetail: (Int) -> Unit,
    onOpenHostingLogs: (Int) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val adminState by adminViewModel.uiState.collectAsState()
    var activeSection by remember { mutableIntStateOf(0) }

    val sections = listOf(
        "Dashboard",
        "VPS Management",
        "Users",
        "Hosting",
        "Plans",
        "Deposits",
        "Support",
        "Announcements",
        "System Settings",
    )

    LaunchedEffect(activeSection) {
        when (activeSection) {
            0 -> adminViewModel.loadAdminDashboard()
            1 -> adminViewModel.loadVpsCluster()
            2 -> adminViewModel.loadUsers()
            3 -> adminViewModel.loadAllHosting()
            4 -> adminViewModel.loadAdminPlans()
            5 -> adminViewModel.loadAdminDeposits()
            6 -> adminViewModel.loadAdminTickets()
            7 -> adminViewModel.loadAnnouncements()
            8 -> adminViewModel.loadSystemSettings()
        }
    }

    var assignMigrateTarget by remember { mutableStateOf<Pair<HostingDto, Boolean>?>(null) }
    var showPlanDialog by remember { mutableStateOf<PlanDto?>(null) }
    var creatingNewPlan by remember { mutableStateOf(false) }

    if (assignMigrateTarget != null) {
        val (hosting, isMigration) = assignMigrateTarget!!
        var chosenVpsId by remember { mutableStateOf(adminState.vpsList.firstOrNull { it.connection_status == "Online" }?.id) }
        AlertDialog(
            onDismissRequest = { assignMigrateTarget = null },
            title = { Text(if (isMigration) "Safe Migrate '${hosting.name}'" else "Assign VPS to '${hosting.name}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isMigration) "Runs 10-step verified container migration with automatic rollback. Only Online verified VPS nodes are selectable."
                        else "Select an Online verified VPS node with sufficient CPU/RAM/Storage capacity.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val eligibleNodes = adminState.vpsList.filter { it.connection_status == "Online" && it.is_enabled && !it.maintenance_mode && it.id != hosting.assigned_vps_id }
                    if (eligibleNodes.isEmpty()) {
                        Text("No eligible Online target VPS nodes available.", color = StatusErrorRed, style = MaterialTheme.typography.bodySmall)
                    } else {
                        eligibleNodes.forEach { node ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = chosenVpsId == node.id, onClick = { chosenVpsId = node.id })
                                Text("${node.name} (${node.available_ram_mb}MB free)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val vId = chosenVpsId
                        if (vId != null) {
                            if (isMigration) adminViewModel.migrateHostingBetweenVps(hosting.id, vId)
                            else adminViewModel.assignHostingToVps(hosting.id, vId)
                        }
                        assignMigrateTarget = null
                    },
                    enabled = chosenVpsId != null,
                ) { Text(if (isMigration) "Start Safe Migration" else "Assign VPS") }
            },
            dismissButton = { TextButton(onClick = { assignMigrateTarget = null }) { Text("Cancel") } },
        )
    }

    if (creatingNewPlan || showPlanDialog != null) {
        val editing = showPlanDialog
        var pName by remember { mutableStateOf(editing?.name.orEmpty()) }
        var pPrice by remember { mutableStateOf((editing?.price_bdt ?: 299.0).toString()) }
        var pDays by remember { mutableStateOf((editing?.duration_days ?: 30).toString()) }
        var pCpu by remember { mutableStateOf((editing?.cpu_limit_cores ?: 1.0).toString()) }
        var pRam by remember { mutableStateOf((editing?.ram_limit_mb ?: 1024).toString()) }
        var pDisk by remember { mutableStateOf((editing?.storage_limit_mb ?: 5120).toString()) }
        var pMax by remember { mutableStateOf((editing?.max_hosting_count ?: 5).toString()) }
        var pStatus by remember { mutableStateOf(editing?.status ?: "Active") }

        AlertDialog(
            onDismissRequest = { creatingNewPlan = false; showPlanDialog = null },
            title = { Text(if (editing == null) "Create Hosting Plan" else "Edit ${editing.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Plan Name") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("Price (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pDays, onValueChange = { pDays = it }, label = { Text("Days") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pCpu, onValueChange = { pCpu = it }, label = { Text("vCPU") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pRam, onValueChange = { pRam = it }, label = { Text("RAM MB") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = pDisk, onValueChange = { pDisk = it }, label = { Text("Disk MB") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pMax, onValueChange = { pMax = it }, label = { Text("Max Hosts") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = pStatus == "Active", onClick = { pStatus = "Active" }, label = { Text("Active") })
                        FilterChip(selected = pStatus == "Disabled", onClick = { pStatus = "Disabled" }, label = { Text("Disabled") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.saveAdminPlan(
                            editing?.id,
                            PlanWriteRequestDto(
                                name = pName,
                                price_bdt = pPrice.toDoubleOrNull() ?: 0.0,
                                duration_days = pDays.toIntOrNull() ?: 30,
                                cpu_limit_cores = pCpu.toDoubleOrNull() ?: 0.5,
                                ram_limit_mb = pRam.toIntOrNull() ?: 512,
                                storage_limit_mb = pDisk.toIntOrNull() ?: 1024,
                                max_hosting_count = pMax.toIntOrNull() ?: 1,
                                status = pStatus,
                            ),
                        )
                        creatingNewPlan = false
                        showPlanDialog = null
                    },
                    enabled = pName.isNotBlank(),
                ) { Text("Save Plan") }
            },
            dismissButton = { TextButton(onClick = { creatingNewPlan = false; showPlanDialog = null }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SmartHost Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Section Navigation Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sections.forEachIndexed { idx, title ->
                        FilterChip(
                            selected = activeSection == idx,
                            onClick = { activeSection = idx },
                            label = { Text(title) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_tab_$idx"),
                        )
                    }
                }
            }

            if (adminState.isLoading || adminState.isMutating) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!adminState.actionMessage.isNullOrBlank()) {
                item { FeedbackBanner(adminState.actionMessage.orEmpty(), onDismiss = { adminViewModel.clearMessages() }) }
            }
            if (!adminState.errorMessage.isNullOrBlank()) {
                item { ErrorBanner(adminState.errorMessage.orEmpty()) }
            }

            when (activeSection) {
                // 0: Admin Dashboard
                0 -> {
                    val d = adminState.dashboard
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                KpiStatCard("Users (Active)", "${d?.active_users ?: 0}/${d?.total_users ?: 0}", Icons.Outlined.People, ElectricBlue, Modifier.weight(1f))
                                KpiStatCard("Revenue (BDT)", FormatUtils.formatBdt(d?.total_revenue_bdt ?: 0.0), Icons.Outlined.Payments, StatusRunningGreen, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                KpiStatCard("Running / Total", "${d?.running_hosting ?: 0}/${d?.total_hosting ?: 0}", Icons.Outlined.CloudDone, CyberCyan, Modifier.weight(1f))
                                KpiStatCard("Pending Deposits", "${d?.pending_deposits ?: 0}", Icons.Outlined.PendingActions, StatusWarningAmber, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                KpiStatCard("Online VPS", "${d?.vps_summary?.online_vps ?: 0}/${d?.vps_summary?.total_vps ?: 0}", Icons.Outlined.Dns, StatusRunningGreen, Modifier.weight(1f))
                                KpiStatCard("Offline/Error VPS", "${(d?.vps_summary?.offline_vps ?: 0) + (d?.vps_summary?.unreachable_vps ?: 0)}", Icons.Outlined.WarningAmber, StatusErrorRed, Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 1: Multi-VPS Management
                1 -> {
                    item {
                        AdminVpsManagementSection(
                            adminState = adminState,
                            onRefreshCluster = { adminViewModel.loadVpsCluster() },
                            onAddOrUpdateVps = { id, req -> adminViewModel.addOrUpdateVps(id, req) {} },
                            onTestVps = { vpsId -> adminViewModel.runVpsAction(vpsId, "test") },
                            onOpenVpsDetail = onOpenVpsDetail,
                        )
                    }
                }

                // 2: User Management
                2 -> {
                    item {
                        var userSearch by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = userSearch,
                            onValueChange = { userSearch = it; adminViewModel.loadUsers(it.ifBlank { null }) },
                            placeholder = { Text("Search users by email...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(adminState.users, key = { it.id }) { u ->
                        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${u.full_name} (#${u.id})", style = MaterialTheme.typography.titleSmall)
                                        Text("${u.email} • Plan: ${u.current_plan_name ?: "None"} • Wallet: ${FormatUtils.formatBdt(u.wallet_balance_bdt)} • Hosts: ${u.hosting_count}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusBadge(if (u.is_active) "Active" else "Suspended")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { adminViewModel.toggleUserSuspension(u) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Text(if (u.is_active) "Suspend" else "Activate")
                                    }
                                    OutlinedButton(onClick = { adminViewModel.deleteUser(u.id) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed), modifier = Modifier.minimumInteractiveComponentSize()) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }

                // 3: All Hosting Management + Assign / Migrate VPS
                3 -> {
                    items(adminState.allHosting, key = { it.id }) { h ->
                        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("#${h.id} ${h.name} (${h.hosting_type})", style = MaterialTheme.typography.titleSmall)
                                        Text("User #${h.user_id} • VPS: ${h.assigned_vps_name ?: "Unassigned"} • ${h.cpu_usage_percent}% CPU / ${h.ram_used_mb}MB RAM", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusBadge(h.status)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    OutlinedButton(onClick = { adminViewModel.adminControlHosting(h.id, "start") }) { Text("Start") }
                                    OutlinedButton(onClick = { adminViewModel.adminControlHosting(h.id, "stop") }) { Text("Stop") }
                                    OutlinedButton(onClick = { adminViewModel.adminControlHosting(h.id, "restart") }) { Text("Restart") }
                                    OutlinedButton(onClick = { onOpenHostingLogs(h.id) }) { Text("Logs") }
                                    FilledTonalButton(onClick = { assignMigrateTarget = h to false }) { Text("Assign VPS") }
                                    if (h.assigned_vps_id != null) {
                                        Button(onClick = { assignMigrateTarget = h to true }) { Text("Migrate VPS") }
                                    }
                                    OutlinedButton(onClick = { adminViewModel.adminControlHosting(h.id, "delete") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed)) { Text("Delete") }
                                }
                            }
                        }
                    }
                }

                // 4: Plan Management
                4 -> {
                    item {
                        Button(onClick = { creatingNewPlan = true }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_plan_button")) {
                            Text("Create New Hosting Plan")
                        }
                    }
                    items(adminState.plans, key = { it.id }) { p ->
                        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${p.name} • ${FormatUtils.formatBdt(p.price_bdt)} (${p.duration_days}d)", style = MaterialTheme.typography.titleSmall)
                                    Text("${p.cpu_limit_cores}vCPU • ${p.ram_limit_mb}MB • ${p.storage_limit_mb}MB • Max ${p.max_hosting_count} • ${p.status}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { showPlanDialog = p }) { Icon(Icons.Outlined.Edit, contentDescription = "Edit Plan") }
                                    IconButton(onClick = { adminViewModel.deleteAdminPlan(p.id) }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Plan", tint = StatusErrorRed) }
                                }
                            }
                        }
                    }
                }

                // 5: Deposit Management
                5 -> {
                    items(adminState.deposits, key = { it.id }) { dep ->
                        var note by remember { mutableStateOf("") }
                        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${dep.payment_method} • ${FormatUtils.formatBdt(dep.amount_bdt)}", style = MaterialTheme.typography.titleSmall)
                                        Text("User: ${dep.user_email ?: "#${dep.user_id}"} • Sender: ${dep.sender_number ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("TrxID: ${dep.transaction_reference}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    StatusBadge(dep.status)
                                }
                                if (dep.status == "Pending") {
                                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Admin Verification Note (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusRunningGreen),
                                            onClick = { adminViewModel.reviewDeposit(dep.id, true, note) },
                                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                        ) { Text("Approve & Credit ৳") }
                                        OutlinedButton(
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                                            onClick = { adminViewModel.reviewDeposit(dep.id, false, note) },
                                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                                        ) { Text("Reject") }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6: Support Management
                6 -> {
                    items(adminState.tickets, key = { it.id }) { t ->
                        Card(
                            onClick = { onOpenTicketDetail(t.id) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("#${t.id} ${t.subject}", style = MaterialTheme.typography.titleSmall)
                                    Text("${t.user_email ?: "User #${t.user_id}"} • ${FormatUtils.shortDate(t.updated_at)}", style = MaterialTheme.typography.bodySmall)
                                }
                                StatusBadge(t.status)
                            }
                        }
                    }
                }

                // 7: Announcements
                7 -> {
                    item {
                        var title by remember { mutableStateOf("") }
                        var content by remember { mutableStateOf("") }
                        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Publish Platform Announcement", style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Announcement Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = { adminViewModel.createAnnouncement(title, content); title = ""; content = "" },
                                    enabled = title.length >= 3 && content.length >= 5,
                                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                                ) { Text("Publish to All Users") }
                            }
                        }
                    }
                    items(adminState.announcements, key = { it.id }) { a ->
                        Card(shape = MaterialTheme.shapes.small, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(a.title, style = MaterialTheme.typography.titleSmall)
                                    Text(a.content, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { adminViewModel.deleteAnnouncement(a.id) }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = StatusErrorRed)
                                }
                            }
                        }
                    }
                }

                // 8: System & BDT Payment Settings
                8 -> {
                    val cfg = adminState.systemSettings
                    if (cfg != null) {
                        item {
                            var appName by remember(cfg) { mutableStateOf(cfg.app_name) }
                            var domain by remember(cfg) { mutableStateOf(cfg.hosting_domain) }
                            var apiUrl by remember(cfg) { mutableStateOf(cfg.api_base_url) }
                            var maint by remember(cfg) { mutableStateOf(cfg.maintenance_mode) }
                            var maxMb by remember(cfg) { mutableStateOf(cfg.max_upload_size_mb.toString()) }
                            var allowedTypes by remember(cfg) { mutableStateOf(cfg.allowed_file_types) }
                            var vpsPolicy by remember(cfg) { mutableStateOf(cfg.default_vps_selection_policy) }
                            var assignPolicy by remember(cfg) { mutableStateOf(cfg.hosting_assignment_policy) }
                            var bkash by remember(cfg) { mutableStateOf(cfg.bkash_number) }
                            var bkashOn by remember(cfg) { mutableStateOf(cfg.bkash_enabled) }
                            var nagad by remember(cfg) { mutableStateOf(cfg.nagad_number) }
                            var nagadOn by remember(cfg) { mutableStateOf(cfg.nagad_enabled) }
                            var rocket by remember(cfg) { mutableStateOf(cfg.rocket_number) }
                            var rocketOn by remember(cfg) { mutableStateOf(cfg.rocket_enabled) }
                            var manualInstr by remember(cfg) { mutableStateOf(cfg.manual_payment_instructions) }

                            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("System, Domain & VPS Assignment Policies", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(value = appName, onValueChange = { appName = it }, label = { Text("Platform Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = domain, onValueChange = { domain = it }, label = { Text("Wildcard Hosting Domain (e.g. cloud.yourdomain.com)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = apiUrl, onValueChange = { apiUrl = it }, label = { Text("Public API Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = maxMb, onValueChange = { maxMb = it }, label = { Text("Max Upload Size (MB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = allowedTypes, onValueChange = { allowedTypes = it }, label = { Text("Allowed File Extensions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = vpsPolicy, onValueChange = { vpsPolicy = it }, label = { Text("VPS Selection Policy (most_available_resources / least_instances)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = assignPolicy, onValueChange = { assignPolicy = it }, label = { Text("Hosting Assignment Policy (auto / manual)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Maintenance Mode")
                                        Switch(checked = maint, onCheckedChange = { maint = it })
                                    }

                                    HorizontalDivider()
                                    Text("BDT Payment Gateway Settings", style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = bkash, onValueChange = { bkash = it }, label = { Text("bKash Number") }, singleLine = true, modifier = Modifier.weight(1f))
                                        Switch(checked = bkashOn, onCheckedChange = { bkashOn = it })
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = nagad, onValueChange = { nagad = it }, label = { Text("Nagad Number") }, singleLine = true, modifier = Modifier.weight(1f))
                                        Switch(checked = nagadOn, onCheckedChange = { nagadOn = it })
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = rocket, onValueChange = { rocket = it }, label = { Text("Rocket Number") }, singleLine = true, modifier = Modifier.weight(1f))
                                        Switch(checked = rocketOn, onCheckedChange = { rocketOn = it })
                                    }
                                    OutlinedTextField(value = manualInstr, onValueChange = { manualInstr = it }, label = { Text("Manual Payment Instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                                    Button(
                                        onClick = {
                                            adminViewModel.saveSystemSettings(
                                                SystemSettingsDto(
                                                    app_name = appName,
                                                    hosting_domain = domain,
                                                    api_base_url = apiUrl,
                                                    maintenance_mode = maint,
                                                    max_upload_size_mb = maxMb.toIntOrNull() ?: 50,
                                                    allowed_file_types = allowedTypes,
                                                    default_vps_selection_policy = vpsPolicy,
                                                    hosting_assignment_policy = assignPolicy,
                                                    bkash_number = bkash,
                                                    bkash_enabled = bkashOn,
                                                    nagad_number = nagad,
                                                    nagad_enabled = nagadOn,
                                                    rocket_number = rocket,
                                                    rocket_enabled = rocketOn,
                                                    manual_payment_instructions = manualInstr,
                                                    manual_enabled = true,
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_system_settings_button"),
                                    ) {
                                        Text("Save System & Payment Settings")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
