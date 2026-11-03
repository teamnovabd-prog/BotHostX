package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repository.SmartHostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserPortalUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val dashboard: DashboardDto? = null,
    val hostingList: List<HostingDto> = emptyList(),
    val selectedHosting: HostingDto? = null,
    val files: List<FileItemDto> = emptyList(),
    val currentDirectory: String = "",
    val editingFile: FileContentDto? = null,
    val logs: List<LogEntryDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val currentPlan: PlanDto? = null,
    val walletOverview: WalletOverviewDto? = null,
    val transactions: List<WalletTransactionDto> = emptyList(),
    val deposits: List<DepositDto> = emptyList(),
    val tickets: List<SupportTicketDto> = emptyList(),
    val selectedTicket: SupportTicketDto? = null,
    val notifications: List<NotificationDto> = emptyList(),
)

class UserPortalViewModel(
    private val repository: SmartHostRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserPortalUiState())
    val uiState: StateFlow<UserPortalUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { getDashboard() }
                .onSuccess { dash ->
                    _uiState.update { it.copy(isLoading = false, dashboard = dash) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun loadHostingList(typeFilter: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { listHosting(typeFilter) }
                .onSuccess { items ->
                    _uiState.update { it.copy(isLoading = false, hostingList = items) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun createHosting(request: CreateHostingRequestDto, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            repository.safeCall { createHosting(request) }
                .onSuccess { created ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            selectedHosting = created,
                            hostingList = listOf(created) + it.hostingList,
                            actionMessage = "Hosting '${created.name}' created.",
                        )
                    }
                    onCreated(created.id)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun loadHostingDetail(hostingId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { getHostingDetail(hostingId) }
                .onSuccess { item ->
                    _uiState.update { it.copy(isLoading = false, selectedHosting = item) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun controlHosting(hostingId: Int, action: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            if (action == "delete") {
                repository.safeCall { deleteHosting(hostingId) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isMutating = false,
                                hostingList = it.hostingList.filterNot { h -> h.id == hostingId },
                                selectedHosting = null,
                                actionMessage = "Hosting deleted",
                            )
                        }
                        onDeleted?.invoke()
                    }
                    .onFailure { err ->
                        _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                    }
                return@launch
            }

            val result = when (action) {
                "start" -> repository.safeCall { startHosting(hostingId) }
                "stop" -> repository.safeCall { stopHosting(hostingId) }
                "restart" -> repository.safeCall { restartHosting(hostingId) }
                "install_deps" -> repository.safeCall { installDependencies(hostingId) }
                else -> return@launch
            }
            result.onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        selectedHosting = updated,
                        hostingList = it.hostingList.map { h -> if (h.id == updated.id) updated else h },
                        actionMessage = "Hosting status: ${updated.status}",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun saveBotConfig(hostingId: Int, entryFile: String, botToken: String?, envVars: Map<String, String>, webhook: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall {
                updateBotConfig(
                    hostingId,
                    UpdateBotConfigRequestDto(
                        entry_file = entryFile,
                        bot_token = botToken?.ifBlank { null },
                        env_vars = envVars,
                        webhook_enabled = webhook,
                    ),
                )
            }.onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        selectedHosting = updated,
                        actionMessage = "Bot token & environment variables stored securely.",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    // --- File Manager ---
    fun loadFiles(hostingId: Int, directory: String = _uiState.value.currentDirectory, search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentDirectory = directory, errorMessage = null) }
            repository.safeCall { listFiles(hostingId, directory, search) }
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, files = list) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun uploadFileFromPicker(hostingId: Int, uri: Uri, autoExtractZip: Boolean) {
        val dir = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            repository.uploadUriToHosting(hostingId, dir, autoExtractZip, uri)
                .onSuccess { item ->
                    _uiState.update {
                        it.copy(isMutating = false, actionMessage = "Uploaded '${item.file_name}'")
                    }
                    loadFiles(hostingId, dir)
                    loadHostingDetail(hostingId)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun createNewFolder(hostingId: Int, folderName: String) {
        val cur = _uiState.value.currentDirectory
        val fullPath = if (cur.isBlank()) folderName.trim() else "$cur/${folderName.trim()}"
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { createFolder(hostingId, CreateFolderRequestDto(fullPath)) }
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, actionMessage = "Folder created") }
                    loadFiles(hostingId, cur)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun renameFileItem(hostingId: Int, oldPath: String, newName: String) {
        val cur = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { renameFile(hostingId, RenameFileRequestDto(oldPath, newName.trim())) }
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, actionMessage = "Renamed to '${newName.trim()}'") }
                    loadFiles(hostingId, cur)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun extractZipArchive(hostingId: Int, zipPath: String) {
        val cur = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { extractZip(hostingId, ExtractZipRequestDto(zipPath)) }
                .onSuccess { res ->
                    _uiState.update { it.copy(isMutating = false, actionMessage = res.message) }
                    loadFiles(hostingId, cur)
                    loadHostingDetail(hostingId)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun openTextEditor(hostingId: Int, relativePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { readFileContent(hostingId, relativePath) }
                .onSuccess { contentDto ->
                    _uiState.update { it.copy(isMutating = false, editingFile = contentDto) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun closeTextEditor() {
        _uiState.update { it.copy(editingFile = null) }
    }

    fun saveEditedTextFile(hostingId: Int, relativePath: String, content: String) {
        val cur = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { saveFileContent(hostingId, FileContentDto(relativePath, content)) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            editingFile = null,
                            actionMessage = "Saved '$relativePath'",
                        )
                    }
                    loadFiles(hostingId, cur)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun deleteFileItem(hostingId: Int, relativePath: String) {
        val cur = _uiState.value.currentDirectory
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { deleteFile(hostingId, relativePath) }
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, actionMessage = "Deleted '$relativePath'") }
                    loadFiles(hostingId, cur)
                    loadHostingDetail(hostingId)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    // --- Logs ---
    fun loadLogs(hostingId: Int, logType: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { getHostingLogs(hostingId, logType) }
                .onSuccess { entries ->
                    _uiState.update { it.copy(isLoading = false, logs = entries) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    // --- Plans & Wallet ---
    fun loadPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val plansRes = repository.safeCall { listPlans() }
            val curRes = repository.safeCall { getCurrentPlan() }
            plansRes.onSuccess { list ->
                _uiState.update {
                    it.copy(isLoading = false, plans = list, currentPlan = curRes.getOrNull())
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun purchasePlan(planId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            repository.safeCall { purchasePlan(PurchasePlanRequestDto(planId)) }
                .onSuccess { plan ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            currentPlan = plan,
                            actionMessage = "Activated ${plan.name}!",
                        )
                    }
                    loadWalletData()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun loadWalletData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val ovRes = repository.safeCall { getWalletOverview() }
            val txRes = repository.safeCall { listTransactions() }
            val depRes = repository.safeCall { listDeposits() }
            ovRes.onSuccess { overview ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        walletOverview = overview,
                        transactions = txRes.getOrDefault(emptyList()),
                        deposits = depRes.getOrDefault(emptyList()),
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun submitDepositRequest(method: String, amountBdt: Double, trxRef: String, senderNumber: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            repository.safeCall {
                submitDeposit(
                    DepositCreateRequestDto(
                        payment_method = method,
                        amount_bdt = amountBdt,
                        transaction_reference = trxRef.trim(),
                        sender_number = senderNumber.trim().ifBlank { null },
                    )
                )
            }.onSuccess { dep ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        deposits = listOf(dep) + it.deposits,
                        actionMessage = "Deposit request submitted (#${dep.id}). Pending Admin verification.",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    // --- Support & Notifications ---
    fun loadSupportTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { listTickets() }
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, tickets = list) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun createSupportTicket(subject: String, description: String, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { createTicket(SupportTicketCreateRequestDto(subject.trim(), description.trim())) }
                .onSuccess { ticket ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            tickets = listOf(ticket) + it.tickets,
                            selectedTicket = ticket,
                            actionMessage = "Support ticket #${ticket.id} opened",
                        )
                    }
                    onCreated(ticket.id)
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun loadTicketDetail(ticketId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { getTicketDetail(ticketId) }
                .onSuccess { ticket ->
                    _uiState.update { it.copy(isLoading = false, selectedTicket = ticket) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun replyToSupportTicket(ticketId: Int, message: String, newStatus: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { replyTicket(ticketId, SupportReplyRequestDto(message.trim(), newStatus)) }
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            selectedTicket = updated,
                            tickets = it.tickets.map { t -> if (t.id == updated.id) updated else t },
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { listNotifications() }
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, notifications = list) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.safeCall { markAllNotificationsRead() }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(notifications = state.notifications.map { it.copy(is_read = true) })
                    }
                }
        }
    }

    class Factory(private val repository: SmartHostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserPortalViewModel(repository) as T
        }
    }
}
