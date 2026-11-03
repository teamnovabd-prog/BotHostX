package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.local.SessionState
import com.example.data.models.*
import com.example.data.repository.SmartHostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val currentUser: UserDto? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val serverHealthMessage: String? = null,
    val serverHealthIsError: Boolean = false,
    val isServerVerified: Boolean? = null,
    val isTestingServer: Boolean = false,
)

class AuthViewModel(
    private val sessionManager: SessionManager,
    private val repository: SmartHostRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionManager.sessionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState())

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        verifyInitialSession()
    }

    fun verifyInitialSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingSession = true) }
            val session = sessionManager.sessionFlow.first()

            if (session.apiBaseUrl != SessionManager.DEFAULT_PRODUCTION_PLACEHOLDER_URL) {
                val health = repository.verifyBackendEndpoint(session.apiBaseUrl)
                _uiState.update {
                    it.copy(
                        isServerVerified = health.verified,
                        serverHealthIsError = !health.verified,
                        serverHealthMessage = health.message,
                    )
                }
            }

            if (session.accessToken.isBlank()) {
                _uiState.update { it.copy(isCheckingSession = false, currentUser = null) }
                return@launch
            }
            repository.safeCall { getMe() }
                .onSuccess { user ->
                    sessionManager.saveAuthenticatedSession(
                        token = session.accessToken,
                        email = user.email,
                        fullName = user.full_name,
                        isAdmin = user.is_admin,
                    )
                    _uiState.update { it.copy(isCheckingSession = false, currentUser = user) }
                }
                .onFailure {
                    sessionManager.clearSession()
                    _uiState.update { it.copy(isCheckingSession = false, currentUser = null) }
                }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            sessionManager.setOnboardingCompleted(true)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            sessionManager.setThemeMode(mode)
        }
    }

    fun saveAndTestServerUrl(url: String, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingServer = true,
                    serverHealthMessage = null,
                    serverHealthIsError = false,
                    errorMessage = null,
                )
            }
            val cleanUrl = SessionManager.normalizeBaseUrl(url)
            sessionManager.setApiBaseUrl(cleanUrl)

            val outcome = repository.verifyBackendEndpoint(cleanUrl)
            _uiState.update {
                it.copy(
                    isTestingServer = false,
                    isServerVerified = outcome.verified,
                    serverHealthIsError = !outcome.verified,
                    serverHealthMessage = outcome.message,
                )
            }
            if (outcome.verified) {
                onSaved?.invoke()
            }
        }
    }

    fun login(email: String, password: String, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                login(LoginRequestDto(email = email.trim(), password = password))
            }.onSuccess { auth ->
                sessionManager.saveAuthenticatedSession(
                    token = auth.access_token,
                    email = auth.user.email,
                    fullName = auth.user.full_name,
                    isAdmin = auth.user.is_admin,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = auth.user,
                        isServerVerified = true,
                        serverHealthIsError = false,
                    )
                }
                onSuccess(auth.user.is_admin)
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun register(fullName: String, email: String, phone: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                register(
                    RegisterRequestDto(
                        full_name = fullName.trim(),
                        email = email.trim(),
                        password = password,
                        phone = phone.trim().ifBlank { null },
                    )
                )
            }.onSuccess { auth ->
                sessionManager.saveAuthenticatedSession(
                    token = auth.access_token,
                    email = auth.user.email,
                    fullName = auth.user.full_name,
                    isAdmin = auth.user.is_admin,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = auth.user,
                        isServerVerified = true,
                        serverHealthIsError = false,
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                forgotPassword(ForgotPasswordRequestDto(email = email.trim()))
            }.onSuccess { res ->
                _uiState.update { it.copy(isLoading = false, infoMessage = res.message) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun confirmPasswordReset(email: String, code: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                resetPassword(
                    ResetPasswordRequestDto(
                        email = email.trim(),
                        reset_code = code.trim(),
                        new_password = newPassword,
                    )
                )
            }.onSuccess { res ->
                _uiState.update { it.copy(isLoading = false, infoMessage = res.message) }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun updateProfile(fullName: String, phone: String, avatarUrl: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                updateProfile(
                    UpdateProfileRequestDto(
                        full_name = fullName.trim(),
                        phone = phone.trim().ifBlank { null },
                        avatar_url = avatarUrl?.trim()?.ifBlank { null },
                    )
                )
            }.onSuccess { updated ->
                val token = sessionState.value.accessToken
                sessionManager.saveAuthenticatedSession(token, updated.email, updated.full_name, updated.is_admin)
                _uiState.update { it.copy(isLoading = false, currentUser = updated, infoMessage = "Profile saved") }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            repository.safeCall {
                changePassword(ChangePasswordRequestDto(current_password = currentPassword, new_password = newPassword))
            }.onSuccess { res ->
                _uiState.update { it.copy(isLoading = false, infoMessage = res.message) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            repository.safeCall { logout() }
            sessionManager.clearSession()
            _uiState.update { it.copy(currentUser = null, errorMessage = null, infoMessage = null) }
            onLoggedOut()
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    class Factory(
        private val sessionManager: SessionManager,
        private val repository: SmartHostRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(sessionManager, repository) as T
        }
    }
}
