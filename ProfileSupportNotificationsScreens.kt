package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.models.UserDto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.UserPortalUiState

@Composable
fun ProfileScreen(
    user: UserDto?,
    isAdminFallback: Boolean,
    onOpenSettings: () -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenHostingHistory: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit,
) {
    val isAdmin = user?.is_admin ?: isAdminFallback

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("profile_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp),
                        ) {
                            if (!user?.avatar_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = user?.avatar_url,
                                    contentDescription = "Profile Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user?.full_name ?: "Cloud User", style = MaterialTheme.typography.titleLarge)
                            Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("User ID: #${user?.id ?: "--"} • Joined: ${FormatUtils.shortDate(user?.created_at).take(10)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(user?.current_plan_name ?: "Free / None", style = MaterialTheme.typography.titleSmall)
                        }
                        Column {
                            Text("Wallet Balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FormatUtils.formatBdt(user?.wallet_balance_bdt ?: 0.0), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("Hosting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${user?.hosting_count ?: 0}", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdminPanel,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("profile_admin_panel_button"),
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open SmartHost Admin & Multi-VPS Panel")
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            ) {
                Column {
                    ProfileMenuItem(Icons.Outlined.ManageAccounts, "Edit Profile, Password & Server Settings", "profile_settings_item", onOpenSettings)
                    HorizontalDivider()
                    ProfileMenuItem(Icons.Outlined.ReceiptLong, "Transaction & Deposit History", "profile_tx_item", onOpenTransactions)
                    HorizontalDivider()
                    ProfileMenuItem(Icons.Outlined.Dns, "Hosting Instances", "profile_hosting_item", onOpenHostingHistory)
                    HorizontalDivider()
                    ProfileMenuItem(Icons.Outlined.Notifications, "Notification Center", "profile_notifications_item", onOpenNotifications)
                    HorizontalDivider()
                    ProfileMenuItem(Icons.Outlined.SupportAgent, "Support Tickets", "profile_support_item", onOpenSupport)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        item { Spacer(Modifier.height(76.dp)) }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authState: AuthUiState,
    currentApiUrl: String,
    themeMode: String,
    onChangeTheme: (String) -> Unit,
    onSaveServerUrl: (String) -> Unit,
    onUpdateProfile: (String, String, String?) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onBack: () -> Unit,
    onClearFeedback: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var apiInput by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
    var fullName by remember(authState.currentUser) { mutableStateOf(authState.currentUser?.full_name.orEmpty()) }
    var phone by remember(authState.currentUser) { mutableStateOf(authState.currentUser?.phone.orEmpty()) }
    var avatarUrl by remember(authState.currentUser) { mutableStateOf(authState.currentUser?.avatar_url.orEmpty()) }

    var curPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!authState.infoMessage.isNullOrBlank()) {
                FeedbackBanner(authState.infoMessage, onDismiss = onClearFeedback)
            }
            if (!authState.errorMessage.isNullOrBlank()) {
                ErrorBanner(authState.errorMessage)
            }

            // Theme Picker
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance Theme", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("system" to "Auto", "light" to "Premium White", "dark" to "Dark Navy").forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onChangeTheme(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // Configurable API Base URL
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backend API Configuration", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = apiInput,
                        onValueChange = { apiInput = it },
                        label = { Text("Production HTTPS API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_url_input"),
                    )
                    if (authState.isTestingServer) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    if (!authState.serverHealthMessage.isNullOrBlank()) {
                        BackendVerificationStatusBox(
                            message = authState.serverHealthMessage,
                            isError = authState.serverHealthIsError,
                        )
                    }
                    Button(
                        onClick = { onSaveServerUrl(apiInput) },
                        enabled = !authState.isTestingServer && apiInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("settings_save_api_button"),
                    ) {
                        Text("Verify GET /health & Save Endpoint")
                    }
                }
            }

            // Edit Profile
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = avatarUrl, onValueChange = { avatarUrl = it }, label = { Text("Avatar Image URL (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onUpdateProfile(fullName, phone, avatarUrl) },
                        enabled = !authState.isLoading && fullName.length >= 2,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("save_profile_button"),
                    ) { Text("Update Profile") }
                }
            }

            // Change Password
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Change Password", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = curPass, onValueChange = { curPass = it }, label = { Text("Current Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("New Password (Min 8 chars)") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onChangePassword(curPass, newPass)
                            curPass = ""
                            newPass = ""
                        },
                        enabled = !authState.isLoading && curPass.isNotBlank() && newPass.length >= 8,
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("change_password_button"),
                    ) { Text("Update Password") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    portalState: UserPortalUiState,
    onLoadNotifications: () -> Unit,
    onMarkAllRead: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) { onLoadNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("mark_all_read_button"),
                    ) { Text("Mark All Read") }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (portalState.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!portalState.isLoading && portalState.notifications.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "All Caught Up",
                        subtitle = "Hosting alerts, deposit approvals, and platform announcements appear here.",
                    )
                }
            } else {
                items(portalState.notifications, key = { it.id }) { n ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (n.is_read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.title, style = MaterialTheme.typography.titleSmall)
                                Text(FormatUtils.shortDate(n.created_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(n.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    portalState: UserPortalUiState,
    onLoadTickets: () -> Unit,
    onCreateTicket: (String, String) -> Unit,
    onOpenTicket: (Int) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLoadTickets() }

    if (showCreateDialog) {
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Support Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Describe your issue") }, minLines = 4)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateTicket(subject, desc)
                        showCreateDialog = false
                    },
                    enabled = subject.length >= 3 && desc.length >= 5,
                ) { Text("Submit Ticket") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Center") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.padding(end = 8.dp).minimumInteractiveComponentSize().testTag("new_ticket_button"),
                    ) { Text("New Ticket") }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (portalState.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!portalState.isLoading && portalState.tickets.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Outlined.SupportAgent,
                        title = "No Support Tickets",
                        subtitle = "Need help with a bot deployment, VPS assignment, or BDT deposit? Open a ticket anytime.",
                        actionLabel = "Open Ticket",
                        onAction = { showCreateDialog = true },
                    )
                }
            } else {
                items(portalState.tickets, key = { it.id }) { t ->
                    Card(
                        onClick = { onOpenTicket(t.id) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("ticket_card_${t.id}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("#${t.id} • ${t.subject}", style = MaterialTheme.typography.titleSmall)
                                Text(FormatUtils.shortDate(t.updated_at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(t.status)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    ticketId: Int,
    portalState: UserPortalUiState,
    isAdmin: Boolean,
    onLoadTicket: () -> Unit,
    onReply: (String, String?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var replyText by remember { mutableStateOf("") }
    var nextStatus by remember { mutableStateOf("Replied") }

    LaunchedEffect(ticketId) { onLoadTicket() }

    val ticket = portalState.selectedTicket

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ticket?.subject ?: "Ticket #$ticketId") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ticket != null) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Ticket #${ticket.id} (${ticket.user_email ?: "User"})", style = MaterialTheme.typography.labelMedium)
                    StatusBadge(ticket.status)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ticket.messages, key = { it.id }) { m ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (m.is_admin_reply) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (m.is_admin_reply) "SmartHost Admin" else "User",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(m.message, style = MaterialTheme.typography.bodyMedium)
                                Text(FormatUtils.shortDate(m.created_at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Open", "In Progress", "Replied", "Closed").forEach { st ->
                            FilterChip(selected = nextStatus == st, onClick = { nextStatus = st }, label = { Text(st) })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Write reply...") },
                        modifier = Modifier.weight(1f).testTag("ticket_reply_input"),
                    )
                    Button(
                        onClick = {
                            onReply(replyText, if (isAdmin) nextStatus else null)
                            replyText = ""
                        },
                        enabled = !portalState.isMutating && replyText.isNotBlank(),
                        modifier = Modifier.height(52.dp).testTag("ticket_send_reply_button"),
                    ) { Text("Send") }
                }
            }
        }
    }
}
