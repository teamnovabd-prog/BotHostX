package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.*

@Composable
fun BrandLogoHeader(
    subtitle: String = "Your Bots. Always Online.",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = PureWhite,
            shadowElevation = 4.dp,
            modifier = Modifier.size(52.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_bothostx_logo),
                contentDescription = stringResource(id = R.string.cd_brand_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        }
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val norm = status.lowercase()
    val (bg, fg) = when {
        norm in listOf("running", "online", "approved", "active", "operational", "completed") ->
            StatusRunningGreen.copy(alpha = 0.15f) to StatusRunningGreen
        norm in listOf("starting", "restarting", "connecting", "pending", "in progress", "replied") ->
            StatusWarningAmber.copy(alpha = 0.16f) to StatusWarningAmber
        norm in listOf("error", "unreachable", "rejected", "expired", "failed", "suspended") ->
            StatusErrorRed.copy(alpha = 0.15f) to StatusErrorRed
        norm in listOf("maintenance", "setup required", "open") ->
            StatusInfoCyan.copy(alpha = 0.16f) to StatusInfoCyan
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun ResourceUsageBar(
    label: String,
    valueText: String,
    progressFraction: Float,
    accentColor: Color = ElectricBlue,
    modifier: Modifier = Modifier,
) {
    val clamped = progressFraction.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = clamped, label = "resource_progress")
    val barColor = when {
        clamped >= 0.9f -> StatusErrorRed
        clamped >= 0.75f -> StatusWarningAmber
        else -> accentColor
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { animated },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
        )
    }
}

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color = ElectricBlue,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    onConfigureServer: (() -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = StatusErrorRed.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, StatusErrorRed.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().testTag("error_banner_card"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = StatusErrorRed,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onRetry != null) {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("error_retry_button"),
                    ) {
                        Text(stringResource(id = R.string.action_retry))
                    }
                }
                if (onConfigureServer != null) {
                    Button(
                        onClick = onConfigureServer,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("error_configure_server_button"),
                    ) {
                        Text(stringResource(id = R.string.action_configure_server))
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackBanner(message: String, onDismiss: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = StatusRunningGreen.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, StatusRunningGreen.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("dismiss_feedback_button"),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("empty_state_action_button"),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun BackendVerificationStatusBox(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (isError) StatusErrorRed else StatusRunningGreen
    val icon = if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle
    Surface(
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth().testTag("backend_health_status_box"),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isError) "Verification failed" else "Backend verified",
                tint = accent,
                modifier = Modifier.size(18.dp).padding(top = 2.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun ServerConfigDialog(
    currentUrl: String,
    isTesting: Boolean,
    healthFeedback: String?,
    isHealthError: Boolean = false,
    onSaveAndTest: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SmartHost Backend API") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your deployed FastAPI HTTPS Base URL (for example, Render or Linux VPS). Verification checks GET /health and validates PostgreSQL + Auth readiness.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://smarthost-api.onrender.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("server_url_input"),
                )
                if (isTesting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (!healthFeedback.isNullOrBlank()) {
                    BackendVerificationStatusBox(
                        message = healthFeedback,
                        isError = isHealthError,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndTest(inputUrl) },
                enabled = !isTesting && inputUrl.isNotBlank(),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("save_server_url_button"),
            ) {
                Text("Verify GET /health & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("close_server_dialog_button"),
            ) {
                Text("Close")
            }
        },
    )
}
