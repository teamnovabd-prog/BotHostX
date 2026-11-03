package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.models.PlanDto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.UserPortalUiState

@Composable
fun PlansScreen(
    portalState: UserPortalUiState,
    onLoadPlans: () -> Unit,
    onPurchasePlan: (Int) -> Unit,
    onConfigureServer: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var confirmPlan by remember { mutableStateOf<PlanDto?>(null) }

    LaunchedEffect(Unit) {
        onLoadPlans()
    }

    if (confirmPlan != null) {
        val p = confirmPlan!!
        AlertDialog(
            onDismissRequest = { confirmPlan = null },
            title = { Text("Activate ${p.name}?") },
            text = {
                Text(
                    "Cost: ${FormatUtils.formatBdt(p.price_bdt)} for ${p.duration_days} days.\n" +
                        "Limits: ${p.cpu_limit_cores} vCPU • ${p.ram_limit_mb} MB RAM • ${FormatUtils.formatStorageMb(p.storage_limit_mb)} Storage • Up to ${p.max_hosting_count} Hosting Instances."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurchasePlan(p.id)
                        confirmPlan = null
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("confirm_purchase_plan_button"),
                ) {
                    Text("Confirm Purchase")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPlan = null }) { Text("Cancel") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("plans_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Cloud Hosting Plans", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Dynamic resource-isolated plans enforced by the backend in Bangladeshi Taka (৳).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (portalState.isLoading || portalState.isMutating) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (!portalState.actionMessage.isNullOrBlank()) {
            item { FeedbackBanner(portalState.actionMessage, onDismiss = onDismissMessage) }
        }
        if (!portalState.errorMessage.isNullOrBlank()) {
            item {
                ErrorBanner(
                    message = portalState.errorMessage,
                    onRetry = onLoadPlans,
                    onConfigureServer = onConfigureServer,
                )
            }
        }

        val currentId = portalState.currentPlan?.id
        if (!portalState.isLoading && portalState.plans.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = "No Active Plans Published",
                    subtitle = "Plans created by the Administrator in the Admin Panel appear dynamically here.",
                    actionLabel = "Refresh Plans",
                    onAction = onLoadPlans,
                )
            }
        } else {
            items(portalState.plans, key = { it.id }) { plan ->
                val isCurrent = plan.id == currentId
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("plan_card_${plan.id}"),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(plan.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "${FormatUtils.formatBdt(plan.price_bdt)} / ${plan.duration_days} Days",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (isCurrent) {
                                StatusBadge("Current Plan")
                            }
                        }

                        HorizontalDivider()

                        Text("• ${plan.cpu_limit_cores} vCPU Core Limit • ${plan.ram_limit_mb} MB Isolated RAM", style = MaterialTheme.typography.bodySmall)
                        Text("• ${FormatUtils.formatStorageMb(plan.storage_limit_mb)} Isolated Storage • Max ${plan.max_hosting_count} Instances", style = MaterialTheme.typography.bodySmall)
                        val supportedTypes = buildList {
                            if (plan.supports_python) add("Python & Telegram Bots")
                            if (plan.supports_php) add("PHP 8.3")
                            if (plan.supports_html) add("HTML Sites")
                        }.joinToString(" • ")
                        Text("• Supports: $supportedTypes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = { confirmPlan = plan },
                            enabled = !portalState.isMutating,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("select_plan_${plan.id}"),
                        ) {
                            Text(if (isCurrent) "Renew ${plan.name}" else "Activate / Upgrade")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(76.dp)) }
    }
}

@Composable
fun WalletScreen(
    portalState: UserPortalUiState,
    onLoadWallet: () -> Unit,
    onSubmitDeposit: (String, Double, String, String) -> Unit,
    onConfigureServer: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedMethod by remember { mutableStateOf("bKash") }
    var amountText by remember { mutableStateOf("") }
    var trxReference by remember { mutableStateOf("") }
    var senderNumber by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onLoadWallet()
    }

    val overview = portalState.walletOverview
    val methods = overview?.payment_methods.orEmpty()
    LaunchedEffect(methods) {
        if (methods.isNotEmpty() && methods.none { it.method == selectedMethod }) {
            selectedMethod = methods.first().method
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("wallet_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // BDT Balance Card
        item {
            Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DarkNavy900, ElectricBlueDark)))
                        .padding(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Available Wallet Balance", style = MaterialTheme.typography.bodySmall, color = TextLightSecondary)
                        Text(
                            text = FormatUtils.formatBdt(overview?.balance_bdt ?: 0.0),
                            style = MaterialTheme.typography.displaySmall,
                            color = PureWhite,
                        )
                        Text(
                            text = "Currency: Bangladeshi Taka (৳ BDT) • Backend-Verified Ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                        )
                    }
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
            item {
                ErrorBanner(
                    message = portalState.errorMessage,
                    onRetry = onLoadWallet,
                    onConfigureServer = onConfigureServer,
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Deposit Funds", "Deposit Requests", "Transactions").forEachIndexed { idx, title ->
                    SegmentedButton(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                        modifier = Modifier.testTag("wallet_tab_$idx"),
                    ) {
                        Text(title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Manual BDT Deposit Request", style = MaterialTheme.typography.titleMedium)

                            val methodNames = if (methods.isNotEmpty()) methods.map { it.method } else listOf("bKash", "Nagad", "Rocket", "Manual")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                methodNames.forEach { mName ->
                                    FilterChip(
                                        selected = selectedMethod == mName,
                                        onClick = { selectedMethod = mName },
                                        label = { Text(mName) },
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("payment_method_$mName"),
                                    )
                                }
                            }

                            val activeMethodInfo = methods.find { it.method == selectedMethod }
                            if (activeMethodInfo != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (activeMethodInfo.account_number.isNotBlank()) {
                                            Text("Account / Wallet: ${activeMethodInfo.account_number}", style = MaterialTheme.typography.titleSmall)
                                        }
                                        Text(activeMethodInfo.instructions, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("Deposit Amount (৳ BDT)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                            )
                            OutlinedTextField(
                                value = senderNumber,
                                onValueChange = { senderNumber = it },
                                label = { Text("Sender Mobile Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("deposit_sender_input"),
                            )
                            OutlinedTextField(
                                value = trxReference,
                                onValueChange = { trxReference = it },
                                label = { Text("Transaction ID / Payment Reference (TrxID)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("deposit_trx_input"),
                            )

                            val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                            Button(
                                onClick = {
                                    onSubmitDeposit(selectedMethod, parsedAmt, trxReference, senderNumber)
                                    amountText = ""
                                    trxReference = ""
                                },
                                enabled = !portalState.isMutating && parsedAmt > 0.0 && trxReference.trim().length >= 4,
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_deposit_button"),
                            ) {
                                Text("Submit Deposit for Admin Approval")
                            }
                        }
                    }
                }
            }

            1 -> {
                if (portalState.deposits.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "No Deposit Requests",
                            subtitle = "Submitted bKash, Nagad, Rocket, and Manual deposit requests appear here.",
                        )
                    }
                } else {
                    items(portalState.deposits, key = { it.id }) { dep ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${dep.payment_method} • ${FormatUtils.formatBdt(dep.amount_bdt)}", style = MaterialTheme.typography.titleSmall)
                                    Text("TrxID: ${dep.transaction_reference}", style = MaterialTheme.typography.labelSmall)
                                    Text(FormatUtils.shortDate(dep.created_at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!dep.admin_note.isNullOrBlank()) {
                                        Text("Note: ${dep.admin_note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                StatusBadge(dep.status)
                            }
                        }
                    }
                }
            }

            2 -> {
                if (portalState.transactions.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.SwapHoriz,
                            title = "No Ledger Transactions",
                            subtitle = "Approved wallet top-ups and plan purchases are recorded here.",
                        )
                    }
                } else {
                    items(portalState.transactions, key = { it.id }) { tx ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.description, style = MaterialTheme.typography.titleSmall)
                                    Text("Ref: ${tx.reference_id} • ${FormatUtils.shortDate(tx.created_at)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val prefix = if (tx.tx_type == "deposit") "+" else "-"
                                Text(
                                    text = "$prefix${FormatUtils.formatBdt(tx.amount_bdt)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (tx.tx_type == "deposit") StatusRunningGreen else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(76.dp)) }
    }
}
