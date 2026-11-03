package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.BackendVerificationStatusBox
import com.example.ui.components.BrandLogoHeader
import com.example.ui.components.ErrorBanner
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.ServerConfigDialog
import com.example.ui.components.StatusBadge
import com.example.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    currentApiUrl: String,
    onLogin: (String, String) -> Unit,
    onSaveServerUrl: (String) -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    onClearFeedback: () -> Unit,
) {
    var isAdminMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = currentApiUrl,
            isTesting = uiState.isTestingServer,
            healthFeedback = uiState.serverHealthMessage,
            isHealthError = uiState.serverHealthIsError,
            onSaveAndTest = onSaveServerUrl,
            onDismiss = { showServerDialog = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandLogoHeader()
                    IconButton(
                        onClick = { showServerDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("login_server_config_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = "Configure Backend API",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Simple toggle between standard User Login and existing simple Admin Login
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isAdminMode,
                        onClick = {
                            isAdminMode = false
                            if (email == "teamnovabd@gmail.com") email = ""
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("user_login_mode_tab"),
                    ) {
                        Text("User Sign In")
                    }
                    SegmentedButton(
                        selected = isAdminMode,
                        onClick = {
                            isAdminMode = true
                            if (email.isBlank()) email = "teamnovabd@gmail.com"
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("admin_login_mode_tab"),
                    ) {
                        Text("Admin Login")
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = if (isAdminMode) "Administrator Access" else "Sign in to BotHostX",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = if (isAdminMode) {
                                "Authenticate using the initial Admin email (teamnovabd@gmail.com) and the private password configured in backend/.env."
                            } else {
                                "Manage your Python bots, Telegram bots, PHP scripts, and HTML websites."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle password visibility",
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                        )

                        if (!isAdminMode) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = onNavigateForgotPassword,
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("forgot_password_button"),
                                ) {
                                    Text("Forgot Password?")
                                }
                            }
                        }

                        Button(
                            onClick = { onLogin(email, password) },
                            enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Text(if (isAdminMode) "Sign In to Admin Panel" else "Sign In")
                            }
                        }
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    ErrorBanner(
                        message = uiState.errorMessage,
                        onConfigureServer = { showServerDialog = true },
                    )
                }
                if (!uiState.infoMessage.isNullOrBlank()) {
                    FeedbackBanner(message = uiState.infoMessage, onDismiss = onClearFeedback)
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("Backend API Server", style = MaterialTheme.typography.labelMedium)
                                    when (uiState.isServerVerified) {
                                        true -> StatusBadge("Online")
                                        false -> StatusBadge("Failed")
                                        null -> Unit
                                    }
                                }
                                Text(
                                    text = currentApiUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { onSaveServerUrl(currentApiUrl) },
                                    enabled = !uiState.isTestingServer,
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("verify_api_url_button"),
                                ) {
                                    Text(if (uiState.isTestingServer) "Checking..." else "Verify")
                                }
                                TextButton(
                                    onClick = { showServerDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("change_api_url_button"),
                                ) {
                                    Text("Change")
                                }
                            }
                        }
                        if (uiState.isTestingServer) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (!uiState.serverHealthMessage.isNullOrBlank()) {
                            BackendVerificationStatusBox(
                                message = uiState.serverHealthMessage,
                                isError = uiState.serverHealthIsError,
                            )
                        }
                    }
                }

                if (!isAdminMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("New to BotHostX?", style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = onNavigateRegister,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("navigate_register_button"),
                        ) {
                            Text("Create Account")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    currentApiUrl: String,
    onSaveServerUrl: (String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onBackToLogin: () -> Unit,
) {
    BackHandler(onBack = onBackToLogin)

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirmPassword && password.length >= 8

    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = currentApiUrl,
            isTesting = uiState.isTestingServer,
            healthFeedback = uiState.serverHealthMessage,
            isHealthError = uiState.serverHealthIsError,
            onSaveAndTest = onSaveServerUrl,
            onDismiss = { showServerDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create BotHostX Account") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("register_back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showServerDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("register_server_config_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = "Configure Backend API",
                            tint = MaterialTheme.colorScheme.primary,
                        )
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Mobile Number (Optional, for BDT Billing)") },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_phone_input"),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Min 8 characters)") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_password_input"),
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input"),
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                ErrorBanner(
                    message = uiState.errorMessage,
                    onConfigureServer = { showServerDialog = true },
                )
            }

            Button(
                onClick = { onRegister(fullName, email, phone, password) },
                enabled = !uiState.isLoading && fullName.length >= 2 && email.contains("@") && passwordsMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_submit_button"),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text("Create Cloud Account")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    uiState: AuthUiState,
    onRequestCode: (String) -> Unit,
    onConfirmReset: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onClearFeedback: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var email by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Recovery") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("forgot_back_button"),
                    ) {
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Step 1: Request a 6-digit recovery code sent to your registered email.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Account Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("forgot_email_input"),
            )
            OutlinedButton(
                onClick = { onRequestCode(email) },
                enabled = !uiState.isLoading && email.contains("@"),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_reset_code_button"),
            ) {
                Text("Send Recovery Code")
            }

            HorizontalDivider()

            Text(
                text = "Step 2: Enter the 6-digit code and set your new password.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = resetCode,
                onValueChange = { resetCode = it },
                label = { Text("6-Digit Reset Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reset_code_input"),
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password (Min 8 chars)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("reset_new_password_input"),
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                ErrorBanner(message = uiState.errorMessage)
            }
            if (!uiState.infoMessage.isNullOrBlank()) {
                FeedbackBanner(message = uiState.infoMessage, onDismiss = onClearFeedback)
            }

            Button(
                onClick = { onConfirmReset(email, resetCode, newPassword) },
                enabled = !uiState.isLoading && email.contains("@") && resetCode.length >= 6 && newPassword.length >= 8,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_password_reset_button"),
            ) {
                Text("Reset Password")
            }
        }
    }
}
