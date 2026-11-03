package com.example.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.ServerConfigDialog
import com.example.ui.screens.*
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserPortalViewModel

private data class MainTabItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun BotHostNavHost(
    authViewModel: AuthViewModel,
    portalViewModel: UserPortalViewModel,
    adminViewModel: AdminViewModel,
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val session by authViewModel.sessionState.collectAsState()
    val portalState by portalViewModel.uiState.collectAsState()
    val adminState by adminViewModel.uiState.collectAsState()

    var showGlobalServerDialog by remember { mutableStateOf(false) }

    if (showGlobalServerDialog) {
        ServerConfigDialog(
            currentUrl = session.apiBaseUrl,
            isTesting = authState.isTestingServer,
            healthFeedback = authState.serverHealthMessage,
            isHealthError = authState.serverHealthIsError,
            onSaveAndTest = { url -> authViewModel.saveAndTestServerUrl(url) },
            onDismiss = { showGlobalServerDialog = false },
        )
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        composable("splash") {
            SplashScreen(
                isCheckingSession = authState.isCheckingSession,
                onboardingCompleted = session.onboardingCompleted,
                isAuthenticated = session.accessToken.isNotBlank(),
                onNavigateNext = { target ->
                    navController.navigate(target) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                currentApiUrl = session.apiBaseUrl,
                isTestingServer = authState.isTestingServer,
                serverHealthMessage = authState.serverHealthMessage,
                serverHealthIsError = authState.serverHealthIsError,
                onSaveServerUrl = { url -> authViewModel.saveAndTestServerUrl(url) },
                onFinishOnboarding = {
                    authViewModel.completeOnboarding()
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
            )
        }

        composable("login") {
            LoginScreen(
                uiState = authState,
                currentApiUrl = session.apiBaseUrl,
                onLogin = { email, pass ->
                    authViewModel.login(email, pass) { isAdmin ->
                        val next = if (isAdmin) "admin_panel" else "main"
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                        if (next == "admin_panel") {
                            navController.navigate("admin_panel")
                        }
                    }
                },
                onSaveServerUrl = { url -> authViewModel.saveAndTestServerUrl(url) },
                onNavigateRegister = { navController.navigate("register") },
                onNavigateForgotPassword = { navController.navigate("forgot_password") },
                onClearFeedback = { authViewModel.clearFeedback() },
            )
        }

        composable("register") {
            RegisterScreen(
                uiState = authState,
                currentApiUrl = session.apiBaseUrl,
                onSaveServerUrl = { url -> authViewModel.saveAndTestServerUrl(url) },
                onRegister = { name, email, phone, pass ->
                    authViewModel.register(name, email, phone, pass) {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onBackToLogin = { navController.popBackStack() },
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                uiState = authState,
                onRequestCode = { email -> authViewModel.requestPasswordReset(email) },
                onConfirmReset = { email, code, newPass ->
                    authViewModel.confirmPasswordReset(email, code, newPass) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                onClearFeedback = { authViewModel.clearFeedback() },
            )
        }

        composable("main") {
            MainAdaptiveShell(
                userNameFallback = session.userName,
                isAdmin = authState.currentUser?.is_admin ?: session.isAdmin,
                currentUser = authState.currentUser,
                portalState = portalState,
                portalViewModel = portalViewModel,
                onConfigureServer = { showGlobalServerDialog = true },
                onCreateHosting = { navController.navigate("create_hosting") },
                onOpenHostingDetail = { id -> navController.navigate("hosting_detail/$id") },
                onOpenNotifications = { navController.navigate("notifications") },
                onOpenSupport = { navController.navigate("support") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenAdminPanel = { navController.navigate("admin_panel") },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable("create_hosting") {
            CreateHostingScreen(
                portalState = portalState,
                onSubmit = { req ->
                    portalViewModel.createHosting(req) { newId ->
                        navController.navigate("hosting_detail/$newId") {
                            popUpTo("create_hosting") { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "hosting_detail/{hostingId}",
            arguments = listOf(navArgument("hostingId") { type = NavType.IntType }),
        ) { entry ->
            val hId = entry.arguments?.getInt("hostingId") ?: 0
            HostingDetailScreen(
                hostingId = hId,
                portalState = portalState,
                onRefresh = { portalViewModel.loadHostingDetail(hId) },
                onAction = { act ->
                    portalViewModel.controlHosting(hId, act) {
                        navController.popBackStack()
                    }
                },
                onSaveBotConfig = { entryFile, tok, env, wh ->
                    portalViewModel.saveBotConfig(hId, entryFile, tok, env, wh)
                },
                onOpenFiles = { navController.navigate("file_manager/$hId") },
                onOpenLogs = { navController.navigate("hosting_logs/$hId") },
                onUpgradePlan = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onDismissMessage = { portalViewModel.clearMessages() },
            )
        }

        composable(
            route = "file_manager/{hostingId}",
            arguments = listOf(navArgument("hostingId") { type = NavType.IntType }),
        ) { entry ->
            val hId = entry.arguments?.getInt("hostingId") ?: 0
            FileManagerScreen(
                hostingId = hId,
                portalState = portalState,
                onLoadFiles = { dir, search -> portalViewModel.loadFiles(hId, dir, search) },
                onUploadUri = { uri, unzip -> portalViewModel.uploadFileFromPicker(hId, uri, unzip) },
                onCreateFolder = { name -> portalViewModel.createNewFolder(hId, name) },
                onRenameFile = { oldP, newN -> portalViewModel.renameFileItem(hId, oldP, newN) },
                onExtractZip = { zipP -> portalViewModel.extractZipArchive(hId, zipP) },
                onOpenEditor = { path -> portalViewModel.openTextEditor(hId, path) },
                onCloseEditor = { portalViewModel.closeTextEditor() },
                onSaveEditor = { path, txt -> portalViewModel.saveEditedTextFile(hId, path, txt) },
                onDeleteItem = { path -> portalViewModel.deleteFileItem(hId, path) },
                onBack = { navController.popBackStack() },
                onDismissMessage = { portalViewModel.clearMessages() },
            )
        }

        composable(
            route = "hosting_logs/{hostingId}",
            arguments = listOf(navArgument("hostingId") { type = NavType.IntType }),
        ) { entry ->
            val hId = entry.arguments?.getInt("hostingId") ?: 0
            HostingLogsScreen(
                hostingId = hId,
                portalState = portalState,
                onLoadLogs = { t -> portalViewModel.loadLogs(hId, t) },
                onBack = { navController.popBackStack() },
            )
        }

        composable("notifications") {
            NotificationsScreen(
                portalState = portalState,
                onLoadNotifications = { portalViewModel.loadNotifications() },
                onMarkAllRead = { portalViewModel.markAllNotificationsRead() },
                onBack = { navController.popBackStack() },
            )
        }

        composable("support") {
            SupportScreen(
                portalState = portalState,
                onLoadTickets = { portalViewModel.loadSupportTickets() },
                onCreateTicket = { sub, desc ->
                    portalViewModel.createSupportTicket(sub, desc) { tId ->
                        navController.navigate("ticket_detail/$tId")
                    }
                },
                onOpenTicket = { tId -> navController.navigate("ticket_detail/$tId") },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "ticket_detail/{ticketId}",
            arguments = listOf(navArgument("ticketId") { type = NavType.IntType }),
        ) { entry ->
            val tId = entry.arguments?.getInt("ticketId") ?: 0
            TicketDetailScreen(
                ticketId = tId,
                portalState = portalState,
                isAdmin = authState.currentUser?.is_admin ?: session.isAdmin,
                onLoadTicket = { portalViewModel.loadTicketDetail(tId) },
                onReply = { msg, st -> portalViewModel.replyToSupportTicket(tId, msg, st) },
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                authState = authState,
                currentApiUrl = session.apiBaseUrl,
                themeMode = session.themeMode,
                onChangeTheme = { mode -> authViewModel.updateThemeMode(mode) },
                onSaveServerUrl = { url -> authViewModel.saveAndTestServerUrl(url) },
                onUpdateProfile = { name, ph, av -> authViewModel.updateProfile(name, ph, av) },
                onChangePassword = { cur, nw -> authViewModel.changePassword(cur, nw) },
                onBack = { navController.popBackStack() },
                onClearFeedback = { authViewModel.clearFeedback() },
            )
        }

        composable("admin_panel") {
            AdminPanelScreen(
                adminViewModel = adminViewModel,
                onOpenVpsDetail = { vpsId -> navController.navigate("admin_vps_detail/$vpsId") },
                onOpenTicketDetail = { tId -> navController.navigate("ticket_detail/$tId") },
                onOpenHostingLogs = { hId -> navController.navigate("hosting_logs/$hId") },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "admin_vps_detail/{vpsId}",
            arguments = listOf(navArgument("vpsId") { type = NavType.IntType }),
        ) { entry ->
            val vpsId = entry.arguments?.getInt("vpsId") ?: 0
            AdminVpsDetailScreen(
                vpsId = vpsId,
                adminState = adminState,
                onLoadDetail = { adminViewModel.loadVpsDetail(vpsId) },
                onRunAction = { act, flag -> adminViewModel.runVpsAction(vpsId, act, flag) },
                onUpdateVps = { req -> adminViewModel.addOrUpdateVps(vpsId, req) {} },
                onRemoveVps = {
                    adminViewModel.runVpsAction(vpsId, "remove") {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() },
                onDismissMessage = { adminViewModel.clearMessages() },
            )
        }
    }
}

@Composable
private fun MainAdaptiveShell(
    userNameFallback: String,
    isAdmin: Boolean,
    currentUser: com.example.data.models.UserDto?,
    portalState: com.example.viewmodel.UserPortalUiState,
    portalViewModel: UserPortalViewModel,
    onConfigureServer: () -> Unit,
    onCreateHosting: () -> Unit,
    onOpenHostingDetail: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("home") }

    val tabs = remember {
        listOf(
            MainTabItem("home", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
            MainTabItem("hosting", "Hosting", Icons.Filled.Dns, Icons.Outlined.Dns),
            MainTabItem("plans", "Plans", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium),
            MainTabItem("wallet", "Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            MainTabItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                if (selectedTab in listOf("home", "hosting")) {
                    ExtendedFloatingActionButton(
                        onClick = onCreateHosting,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Create Hosting") },
                        modifier = Modifier.testTag("main_create_hosting_fab"),
                    )
                }
            },
            bottomBar = {
                if (!isWideTablet) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab.key
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab.key },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("bottom_nav_${tab.key}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (isWideTablet) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = selectedTab == tab.key
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab.key },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        "home" -> DashboardScreen(
                            portalState = portalState,
                            userNameFallback = userNameFallback,
                            isAdmin = isAdmin,
                            onRefresh = { portalViewModel.loadDashboard() },
                            onConfigureServer = onConfigureServer,
                            onCreateHosting = onCreateHosting,
                            onManageHosting = { selectedTab = "hosting" },
                            onViewPlans = { selectedTab = "plans" },
                            onOpenDeposit = { selectedTab = "wallet" },
                            onOpenSupport = onOpenSupport,
                            onOpenNotifications = onOpenNotifications,
                            onOpenAdminPanel = onOpenAdminPanel,
                        )
                        "hosting" -> HostingListScreen(
                            portalState = portalState,
                            onLoadHosting = { filter -> portalViewModel.loadHostingList(filter) },
                            onCreateHosting = onCreateHosting,
                            onSelectHosting = onOpenHostingDetail,
                            onConfigureServer = onConfigureServer,
                        )
                        "plans" -> PlansScreen(
                            portalState = portalState,
                            onLoadPlans = { portalViewModel.loadPlans() },
                            onPurchasePlan = { pId -> portalViewModel.purchasePlan(pId) },
                            onConfigureServer = onConfigureServer,
                            onDismissMessage = { portalViewModel.clearMessages() },
                        )
                        "wallet" -> WalletScreen(
                            portalState = portalState,
                            onLoadWallet = { portalViewModel.loadWalletData() },
                            onSubmitDeposit = { m, amt, ref, snd -> portalViewModel.submitDepositRequest(m, amt, ref, snd) },
                            onConfigureServer = onConfigureServer,
                            onDismissMessage = { portalViewModel.clearMessages() },
                        )
                        "profile" -> ProfileScreen(
                            user = portalState.dashboard?.user ?: currentUser,
                            isAdminFallback = isAdmin,
                            onOpenSettings = onOpenSettings,
                            onOpenTransactions = { selectedTab = "wallet" },
                            onOpenHostingHistory = { selectedTab = "hosting" },
                            onOpenNotifications = onOpenNotifications,
                            onOpenSupport = onOpenSupport,
                            onOpenAdminPanel = onOpenAdminPanel,
                            onLogout = onLogout,
                        )
                    }
                }
            }
        }
    }
}
