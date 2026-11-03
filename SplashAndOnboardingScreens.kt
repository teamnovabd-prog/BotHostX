package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.ServerConfigDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    isCheckingSession: Boolean,
    onboardingCompleted: Boolean,
    isAuthenticated: Boolean,
    onNavigateNext: (String) -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "logo_scale",
    )

    LaunchedEffect(isCheckingSession, onboardingCompleted, isAuthenticated) {
        if (!isCheckingSession) {
            delay(550)
            val destination = when {
                isAuthenticated -> "main"
                !onboardingCompleted -> "onboarding"
                else -> "login"
            }
            onNavigateNext(destination)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkNavy950, DarkNavy800, ElectricBlueDark),
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = PureWhite,
                shadowElevation = 16.dp,
                modifier = Modifier.size(136.dp).scale(pulseScale),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bothostx_logo),
                    contentDescription = stringResource(id = R.string.cd_brand_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                )
            }
            Text(
                text = "BotHostX",
                style = MaterialTheme.typography.displaySmall,
                color = PureWhite,
            )
            Text(
                text = "SmartHost Mobile Cloud Hosting Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLightSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(
                color = CyberCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp).testTag("splash_loading_indicator"),
            )
        }
    }
}

private data class OnboardingPageData(
    val icon: ImageVector,
    val badge: String,
    val title: String,
    val description: String,
)

@Composable
fun OnboardingScreen(
    currentApiUrl: String,
    isTestingServer: Boolean,
    serverHealthMessage: String?,
    serverHealthIsError: Boolean = false,
    onSaveServerUrl: (String) -> Unit,
    onFinishOnboarding: () -> Unit,
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                icon = Icons.Filled.SmartToy,
                badge = "PYTHON & TELEGRAM BOTS",
                title = "Deploy Bots Directly From Your Phone",
                description = "Upload .py scripts, ZIP projects, and requirements.txt. Store bot tokens encrypted on the server and stream live console logs anytime.",
            ),
            OnboardingPageData(
                icon = Icons.Filled.CloudDone,
                badge = "PHP 8.3 & HTML CLOUD",
                title = "Isolated Docker Hosting Runtimes",
                description = "Host PHP scripts and static HTML websites inside strict CPU, RAM, and storage-isolated containers with a built-in code editor & file manager.",
            ),
            OnboardingPageData(
                icon = Icons.Filled.Dns,
                badge = "MULTI-VPS CLUSTER & BDT WALLET",
                title = "Enterprise Multi-VPS Control & BDT Billing",
                description = "Top up via bKash, Nagad, or Rocket (৳) to activate dynamic plans while administrators manage, monitor, and migrate live Linux VPS servers.",
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = currentApiUrl,
            isTesting = isTestingServer,
            healthFeedback = serverHealthMessage,
            isHealthError = serverHealthIsError,
            onSaveAndTest = onSaveServerUrl,
            onDismiss = { showServerDialog = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { showServerDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_server_button"),
                ) {
                    Icon(Icons.Outlined.SettingsEthernet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("API Endpoint")
                }
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_skip_button"),
                ) {
                    Text("Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.size(132.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = page.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ElectricBlue.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = page.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { idx ->
                        val active = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 28.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                        )
                    }
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                ) {
                    Text(if (pagerState.currentPage < pages.lastIndex) "Continue" else "Get Started")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
