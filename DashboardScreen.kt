package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.UserPortalUiState

@Composable
fun DashboardScreen(
    portalState: UserPortalUiState,
    userNameFallback: String,
    isAdmin: Boolean,
    onRefresh: () -> Unit,
    onConfigureServer: () -> Unit,
    onCreateHosting: () -> Unit,
    onManageHosting: () -> Unit,
    onViewPlans: () -> Unit,
    onOpenDeposit: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminPanel: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val dash = portalState.dashboard
    val displayName = dash?.user?.full_name?.ifBlank { userNameFallback } ?: userNameFallback.ifBlank { "Developer" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("dashboard_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandLogoHeader(subtitle = "Welcome back, $displayName")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAdmin) {
                        FilledTonalIconButton(
                            onClick = onOpenAdminPanel,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("dashboard_admin_shortcut"),
                        ) {
                            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = "Open Admin Panel")
                        }
                    }
                    BadgedBox(
                        badge = {
                            val unread = dash?.unread_notifications ?: 0
                            if (unread > 0) {
                                Badge { Text(unread.toString()) }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("dashboard_notifications_button"),
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            }
        }

        if (portalState.isLoading && dash == null) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (!portalState.errorMessage.isNullOrBlank()) {
            item {
                ErrorBanner(
                    message = portalState.errorMessage,
                    onRetry = onRefresh,
                    onConfigureServer = onConfigureServer,
                )
            }
        }

        // Hero Cloud Status Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DarkNavy900, DarkNavy700, ElectricBlueDark),
                            )
                        )
                        .padding(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = dash?.current_plan?.name ?: "No Active Plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite,
                                )
                                Text(
                                    text = "Expiry: ${FormatUtils.shortDate(dash?.nearest_expiry)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLightSecondary,
                                )
                            }
                            StatusBadge(status = dash?.overall_status ?: "Connecting")
                        }

                        HorizontalDivider(color = PureWhite.copy(alpha = 0.14f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            HeroMetricItem("Active Bots/Sites", "${dash?.active_hosting_count ?: 0}")
                            HeroMetricItem("Total Instances", "${dash?.total_hosting_count ?: 0}")
                            HeroMetricItem(
                                "Wallet (BDT)",
                                FormatUtils.formatBdt(dash?.user?.wallet_balance_bdt ?: 0.0),
                            )
                        }
                    }
                }
            }
        }

        // Admin Banner if logged in as Administrator
        if (isAdmin) {
            item {
                Card(
                    onClick = onOpenAdminPanel,
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("open_admin_panel_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SmartHost Admin & Multi-VPS Control", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Manage VPS nodes, review BDT deposits, plans, and users",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        // Quick Actions Grid (Create Hosting, Upload File, Manage Hosting, View Plans, Deposit, Support)
        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickActionTile(
                        label = "Create Hosting",
                        icon = Icons.Filled.AddCircle,
                        tag = "quick_create_hosting",
                        onClick = onCreateHosting,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Upload / Files",
                        icon = Icons.Filled.UploadFile,
                        tag = "quick_upload_files",
                        onClick = onManageHosting,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Manage Hosting",
                        icon = Icons.Filled.Dns,
                        tag = "quick_manage_hosting",
                        onClick = onManageHosting,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickActionTile(
                        label = "View Plans",
                        icon = Icons.Filled.WorkspacePremium,
                        tag = "quick_view_plans",
                        onClick = onViewPlans,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Deposit (৳)",
                        icon = Icons.Filled.AccountBalanceWallet,
                        tag = "quick_deposit",
                        onClick = onOpenDeposit,
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Support",
                        icon = Icons.Filled.SupportAgent,
                        tag = "quick_support",
                        onClick = onOpenSupport,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Real Resource Usage Telemetry Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Live Hosting Resource Usage", style = MaterialTheme.typography.titleMedium)

                    val cpuPct = (dash?.cpu_usage_percent ?: 0.0).toFloat()
                    ResourceUsageBar(
                        label = "CPU Allocation Usage",
                        valueText = "${String.format("%.1f", cpuPct)}%",
                        progressFraction = cpuPct / 100f,
                        accentColor = ElectricBlue,
                    )

                    val ramUsed = dash?.ram_used_mb ?: 0
                    val ramLim = (dash?.ram_limit_mb ?: 0).coerceAtLeast(1)
                    ResourceUsageBar(
                        label = "RAM Usage",
                        valueText = "$ramUsed / ${dash?.ram_limit_mb ?: 0} MB",
                        progressFraction = if ((dash?.ram_limit_mb ?: 0) > 0) ramUsed.toFloat() / ramLim else 0f,
                        accentColor = CyberCyan,
                    )

                    val stUsed = (dash?.storage_used_mb ?: 0.0).toFloat()
                    val stLim = (dash?.storage_limit_mb ?: 0).coerceAtLeast(1)
                    ResourceUsageBar(
                        label = "Isolated Storage Usage",
                        valueText = "${String.format("%.1f", stUsed)} / ${dash?.storage_limit_mb ?: 0} MB",
                        progressFraction = if ((dash?.storage_limit_mb ?: 0) > 0) stUsed / stLim else 0f,
                        accentColor = StatusRunningGreen,
                    )
                }
            }
        }

        // Published Announcements
        val announcements = dash?.announcements.orEmpty()
        if (announcements.isNotEmpty()) {
            item {
                Text("Platform Announcements", style = MaterialTheme.typography.titleMedium)
            }
            items(announcements) { ann ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ann["title"].orEmpty(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            ann["content"].orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Recent Activities
        item {
            Text("Recent Activities", style = MaterialTheme.typography.titleMedium)
        }
        val activities = dash?.recent_activities.orEmpty()
        if (activities.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.History,
                    title = "No Recent Activity Yet",
                    subtitle = "Deploy your first Python bot, PHP script, or HTML website to see real-time telemetry and events here.",
                    actionLabel = "Create Hosting",
                    onAction = onCreateHosting,
                )
            }
        } else {
            items(activities) { act ->
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (act.category == "wallet") Icons.Outlined.AccountBalanceWallet else Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(act.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${act.subtitle} • ${FormatUtils.shortDate(act.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun HeroMetricItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextLightSecondary,
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(tag),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}
