package com.example.viewmodel

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

data class AdminUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val dashboard: AdminDashboardDto? = null,
    val vpsSummary: VpsClusterSummaryDto = VpsClusterSummaryDto(),
    val vpsList: List<VpsDto> = emptyList(),
    val selectedVps: VpsDto? = null,
    val vpsMetrics: List<VpsMetricHistoryDto> = emptyList(),
    val vpsHosting: List<HostingDto> = emptyList(),
    val vpsLogs: List<VpsOpLogDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val allHosting: List<HostingDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val deposits: List<DepositDto> = emptyList(),
    val tickets: List<SupportTicketDto> = emptyList(),
    val announcements: List<AnnouncementDto> = emptyList(),
    val systemSettings: SystemSettingsDto? = null,
)

class AdminViewModel(
    private val repository: SmartHostRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionMessage = null) }
    }

    fun loadAdminDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { getAdminDashboard() }
                .onSuccess { dash ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dashboard = dash,
                            vpsSummary = dash.vps_summary,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
                }
        }
    }

    // --- Multi-VPS Management ---
    fun loadVpsCluster() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val sumRes = repository.safeCall { adminGetVpsSummary() }
            val listRes = repository.safeCall { adminListVps() }
            listRes.onSuccess { servers ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        vpsList = servers,
                        vpsSummary = sumRes.getOrDefault(it.vpsSummary),
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun loadVpsDetail(vpsId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val detailRes = repository.safeCall { adminGetVpsDetail(vpsId) }
            val metricsRes = repository.safeCall { adminGetVpsMetrics(vpsId) }
            val hostRes = repository.safeCall { adminGetVpsHosting(vpsId) }
            val logsRes = repository.safeCall { adminGetVpsLogs(vpsId) }
            detailRes.onSuccess { vps ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedVps = vps,
                        vpsMetrics = metricsRes.getOrDefault(emptyList()),
                        vpsHosting = hostRes.getOrDefault(emptyList()),
                        vpsLogs = logsRes.getOrDefault(emptyList()),
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun addOrUpdateVps(vpsId: Int?, req: VpsCreateUpdateRequestDto, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val res = if (vpsId == null) {
                repository.safeCall { adminAddVps(req) }
            } else {
                repository.safeCall { adminUpdateVps(vpsId, req) }
            }
            res.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        selectedVps = saved,
                        actionMessage = "VPS '${saved.name}' saved (status: ${saved.connection_status})",
                    )
                }
                loadVpsCluster()
                onDone()
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun runVpsAction(vpsId: Int, action: String, maintenanceFlag: Boolean = false, onRemoved: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null, actionMessage = null) }
            if (action == "remove") {
                repository.safeCall { adminRemoveVps(vpsId) }
                    .onSuccess { msg ->
                        _uiState.update {
                            it.copy(
                                isMutating = false,
                                vpsList = it.vpsList.filterNot { s -> s.id == vpsId },
                                selectedVps = null,
                                actionMessage = msg.message,
                            )
                        }
                        loadVpsCluster()
                        onRemoved?.invoke()
                    }
                    .onFailure { err ->
                        _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                    }
                return@launch
            }

            val res = when (action) {
                "test" -> repository.safeCall { adminTestVpsConnection(vpsId) }
                "connect" -> repository.safeCall { adminConnectVps(vpsId) }
                "disconnect" -> repository.safeCall { adminDisconnectVps(vpsId) }
                "enable" -> repository.safeCall { adminEnableVps(vpsId) }
                "disable" -> repository.safeCall { adminDisableVps(vpsId) }
                "maintenance" -> repository.safeCall {
                    adminSetVpsMaintenance(vpsId, MaintenanceToggleRequestDto(maintenanceFlag))
                }
                else -> return@launch
            }
            res.onSuccess { updated ->
                val note = if (updated.last_error != null) {
                    "${updated.connection_status}: ${updated.last_error}"
                } else {
                    "VPS '${updated.name}' is now ${updated.connection_status}"
                }
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        selectedVps = updated,
                        vpsList = it.vpsList.map { s -> if (s.id == updated.id) updated else s },
                        actionMessage = note,
                    )
                }
                loadVpsDetail(vpsId)
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun assignHostingToVps(hostingId: Int, targetVpsId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminAssignHostingVps(hostingId, AssignVpsRequestDto(targetVpsId)) }
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            allHosting = it.allHosting.map { h -> if (h.id == updated.id) updated else h },
                            actionMessage = "Assigned '${updated.name}' to VPS '${updated.assigned_vps_name}'",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun migrateHostingBetweenVps(hostingId: Int, targetVpsId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall {
                adminMigrateHostingVps(hostingId, MigrateVpsRequestDto(target_vps_id = targetVpsId, preserve_url = true))
            }.onSuccess { migRes ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        allHosting = it.allHosting.map { h -> if (h.id == migRes.hosting.id) migRes.hosting else h },
                        actionMessage = "Migration Job #${migRes.job_id} ${migRes.status} -> '${migRes.hosting.assigned_vps_name}'",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    // --- Users, Hosting, Plans, Deposits, Support, Announcements, Settings ---
    fun loadUsers(search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminListUsers(search) }
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, users = list) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun toggleUserSuspension(user: UserDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val res = if (user.is_active) {
                repository.safeCall { adminSuspendUser(user.id) }
            } else {
                repository.safeCall { adminActivateUser(user.id) }
            }
            res.onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        users = it.users.map { u -> if (u.id == updated.id) updated else u },
                        actionMessage = if (updated.is_active) "Activated ${updated.email}" else "Suspended ${updated.email}",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminDeleteUser(userId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            users = it.users.filterNot { u -> u.id == userId },
                            actionMessage = "User deleted",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
                }
        }
    }

    fun loadAllHosting(search: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val hRes = repository.safeCall { adminListAllHosting(search) }
            val vRes = repository.safeCall { adminListVps() }
            hRes.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allHosting = list,
                        vpsList = vRes.getOrDefault(it.vpsList),
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun adminControlHosting(hostingId: Int, action: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            if (action == "delete") {
                repository.safeCall { deleteHosting(hostingId) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isMutating = false,
                                allHosting = it.allHosting.filterNot { h -> h.id == hostingId },
                                actionMessage = "Hosting #$hostingId removed",
                            )
                        }
                    }
                    .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
                return@launch
            }
            val res = when (action) {
                "start" -> repository.safeCall { startHosting(hostingId) }
                "stop" -> repository.safeCall { stopHosting(hostingId) }
                "restart" -> repository.safeCall { restartHosting(hostingId) }
                else -> return@launch
            }
            res.onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        isMutating = false,
                        allHosting = it.allHosting.map { h -> if (h.id == updated.id) updated else h },
                        actionMessage = "Hosting '${updated.name}' -> ${updated.status}",
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun loadAdminPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminListPlans() }
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, plans = list) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun saveAdminPlan(planId: Int?, req: PlanWriteRequestDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            val res = if (planId == null) {
                repository.safeCall { adminCreatePlan(req) }
            } else {
                repository.safeCall { adminUpdatePlan(planId, req) }
            }
            res.onSuccess {
                _uiState.update { it.copy(isMutating = false, actionMessage = "Saved plan '${req.name}'") }
                loadAdminPlans()
            }.onFailure { err ->
                _uiState.update { it.copy(isMutating = false, errorMessage = err.message) }
            }
        }
    }

    fun deleteAdminPlan(planId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminDeletePlan(planId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            plans = it.plans.filterNot { p -> p.id == planId },
                            actionMessage = "Plan deleted",
                        )
                    }
                }
                .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
        }
    }

    fun loadAdminDeposits(statusFilter: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminListDeposits(statusFilter) }
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, deposits = list) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun reviewDeposit(depositId: Int, approve: Boolean, note: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminReviewDeposit(depositId, DepositReviewRequestDto(approve, note.ifBlank { null })) }
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            deposits = it.deposits.map { d -> if (d.id == updated.id) updated else d },
                            actionMessage = "Deposit #${updated.id} ${updated.status}",
                        )
                    }
                }
                .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
        }
    }

    fun loadAdminTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminListTickets() }
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, tickets = list) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminListAnnouncements() }
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, announcements = list) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun createAnnouncement(title: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminCreateAnnouncement(AnnouncementWriteRequestDto(title.trim(), content.trim(), true)) }
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, actionMessage = "Announcement published") }
                    loadAnnouncements()
                }
                .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
        }
    }

    fun deleteAnnouncement(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminDeleteAnnouncement(id) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            announcements = it.announcements.filterNot { a -> a.id == id },
                            actionMessage = "Announcement deleted",
                        )
                    }
                }
                .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
        }
    }

    fun loadSystemSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.safeCall { adminGetSettings() }
                .onSuccess { cfg -> _uiState.update { it.copy(isLoading = false, systemSettings = cfg) } }
                .onFailure { err -> _uiState.update { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }

    fun saveSystemSettings(cfg: SystemSettingsDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, errorMessage = null) }
            repository.safeCall { adminUpdateSettings(cfg) }
                .onSuccess { saved ->
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            systemSettings = saved,
                            actionMessage = "System & payment settings updated",
                        )
                    }
                }
                .onFailure { err -> _uiState.update { it.copy(isMutating = false, errorMessage = err.message) } }
        }
    }

    class Factory(private val repository: SmartHostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(repository) as T
        }
    }
}
