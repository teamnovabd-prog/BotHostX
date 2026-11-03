package com.example.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponseDto(
    val status: String = "",
    val service: String = "",
    val platform: String = "",
    val auth_ready: Boolean = false,
    val database: String = "",
    val maintenance_mode: Boolean = false,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponseDto(
    val detail: Any? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    val full_name: String,
    val email: String,
    val password: String,
    val phone: String? = null,
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(
    val email: String,
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    val email: String,
    val reset_code: String,
    val new_password: String,
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequestDto(
    val current_password: String,
    val new_password: String,
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    val full_name: String,
    val phone: String? = null,
    val avatar_url: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Int,
    val email: String,
    val full_name: String,
    val phone: String? = null,
    val avatar_url: String? = null,
    val is_admin: Boolean = false,
    val is_active: Boolean = true,
    val current_plan_name: String? = null,
    val plan_expires_at: String? = null,
    val wallet_balance_bdt: Double = 0.0,
    val hosting_count: Int = 0,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class AuthTokenDto(
    val access_token: String,
    val token_type: String = "bearer",
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class PlanDto(
    val id: Int,
    val name: String,
    val price_bdt: Double,
    val duration_days: Int,
    val cpu_limit_cores: Double,
    val ram_limit_mb: Int,
    val storage_limit_mb: Int,
    val max_hosting_count: Int,
    val supports_python: Boolean = true,
    val supports_php: Boolean = true,
    val supports_html: Boolean = true,
    val status: String = "Active",
)

@JsonClass(generateAdapter = true)
data class PlanWriteRequestDto(
    val name: String,
    val price_bdt: Double,
    val duration_days: Int,
    val cpu_limit_cores: Double,
    val ram_limit_mb: Int,
    val storage_limit_mb: Int,
    val max_hosting_count: Int,
    val supports_python: Boolean = true,
    val supports_php: Boolean = true,
    val supports_html: Boolean = true,
    val status: String = "Active",
)

@JsonClass(generateAdapter = true)
data class PurchasePlanRequestDto(
    val plan_id: Int,
)

@JsonClass(generateAdapter = true)
data class CreateHostingRequestDto(
    val name: String,
    val hosting_type: String,
    val subdomain_slug: String? = null,
    val entry_file: String = "main.py",
    val bot_token: String? = null,
    val env_vars: Map<String, String> = emptyMap(),
    val webhook_enabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UpdateBotConfigRequestDto(
    val entry_file: String? = null,
    val bot_token: String? = null,
    val env_vars: Map<String, String>? = null,
    val webhook_enabled: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class HostingDto(
    val id: Int,
    val user_id: Int,
    val name: String,
    val hosting_type: String,
    val status: String,
    val plan_name: String? = null,
    val assigned_vps_id: Int? = null,
    val assigned_vps_name: String? = null,
    val assigned_vps_region: String? = null,
    val cpu_limit_cores: Double = 0.5,
    val ram_limit_mb: Int = 512,
    val storage_limit_mb: Int = 1024,
    val cpu_usage_percent: Double = 0.0,
    val ram_used_mb: Int = 0,
    val storage_used_mb: Double = 0.0,
    val uptime_seconds: Int = 0,
    val public_url: String? = null,
    val php_version: String? = null,
    val entry_file: String = "main.py",
    val bot_token_masked: String? = null,
    val webhook_enabled: Boolean = false,
    val created_at: String = "",
    val expires_at: String = "",
)

@JsonClass(generateAdapter = true)
data class FileItemDto(
    val id: Int,
    val relative_path: String,
    val parent_dir: String,
    val file_name: String,
    val file_type: String,
    val is_directory: Boolean,
    val size_bytes: Int,
    val updated_at: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateFolderRequestDto(
    val folder_path: String,
)

@JsonClass(generateAdapter = true)
data class RenameFileRequestDto(
    val old_path: String,
    val new_name: String,
)

@JsonClass(generateAdapter = true)
data class ExtractZipRequestDto(
    val zip_path: String,
)

@JsonClass(generateAdapter = true)
data class FileContentDto(
    val relative_path: String,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class LogEntryDto(
    val id: Int,
    val log_type: String,
    val message: String,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class ActivityItemDto(
    val title: String,
    val subtitle: String,
    val category: String,
    val timestamp: String = "",
)

@JsonClass(generateAdapter = true)
data class DashboardDto(
    val user: UserDto,
    val active_hosting_count: Int = 0,
    val total_hosting_count: Int = 0,
    val current_plan: PlanDto? = null,
    val overall_status: String = "Ready",
    val cpu_usage_percent: Double = 0.0,
    val ram_used_mb: Int = 0,
    val ram_limit_mb: Int = 0,
    val storage_used_mb: Double = 0.0,
    val storage_limit_mb: Int = 0,
    val nearest_expiry: String? = null,
    val unread_notifications: Int = 0,
    val recent_activities: List<ActivityItemDto> = emptyList(),
    val announcements: List<Map<String, String>> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PaymentMethodDto(
    val method: String,
    val enabled: Boolean,
    val account_number: String,
    val instructions: String,
)

@JsonClass(generateAdapter = true)
data class WalletOverviewDto(
    val balance_bdt: Double = 0.0,
    val currency: String = "BDT",
    val payment_methods: List<PaymentMethodDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DepositCreateRequestDto(
    val payment_method: String,
    val amount_bdt: Double,
    val transaction_reference: String,
    val sender_number: String? = null,
)

@JsonClass(generateAdapter = true)
data class DepositDto(
    val id: Int,
    val user_id: Int,
    val user_email: String? = null,
    val payment_method: String,
    val amount_bdt: Double,
    val transaction_reference: String,
    val sender_number: String? = null,
    val status: String,
    val admin_note: String? = null,
    val created_at: String = "",
    val reviewed_at: String? = null,
)

@JsonClass(generateAdapter = true)
data class WalletTransactionDto(
    val id: Int,
    val tx_type: String,
    val amount_bdt: Double,
    val status: String,
    val reference_id: String,
    val description: String,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class SupportTicketCreateRequestDto(
    val subject: String,
    val description: String,
)

@JsonClass(generateAdapter = true)
data class SupportReplyRequestDto(
    val message: String,
    val new_status: String? = null,
)

@JsonClass(generateAdapter = true)
data class SupportMessageDto(
    val id: Int,
    val sender_user_id: Int,
    val is_admin_reply: Boolean,
    val message: String,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class SupportTicketDto(
    val id: Int,
    val user_id: Int,
    val user_email: String? = null,
    val subject: String,
    val description: String,
    val status: String,
    val created_at: String = "",
    val updated_at: String = "",
    val messages: List<SupportMessageDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Int,
    val title: String,
    val message: String,
    val category: String,
    val is_read: Boolean,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class VpsCreateUpdateRequestDto(
    val name: String,
    val provider: String,
    val ip_address: String,
    val ssh_port: Int = 22,
    val ssh_username: String = "root",
    val auth_method: String = "vps_agent",
    val agent_url: String? = null,
    val secret_credential: String? = null,
    val agent_token: String? = null,
    val host_key_fingerprint: String? = null,
    val os_name: String = "Ubuntu 24.04 LTS",
    val region: String = "Singapore",
    val cpu_capacity_cores: Double = 2.0,
    val ram_capacity_mb: Int = 4096,
    val storage_capacity_mb: Int = 40960,
    val reserved_cpu_cores: Double = 0.5,
    val reserved_ram_mb: Int = 512,
    val reserved_storage_mb: Int = 2048,
    val max_hosting_count: Int = 25,
    val runtime_type: String = "docker",
    val is_enabled: Boolean = true,
    val maintenance_mode: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class VpsDto(
    val id: Int,
    val name: String,
    val provider: String,
    val ip_address: String,
    val ssh_port: Int = 22,
    val ssh_username: String = "root",
    val auth_method: String = "vps_agent",
    val agent_url: String? = null,
    val os_name: String = "",
    val region: String = "",
    val cpu_capacity_cores: Double = 0.0,
    val available_cpu_cores: Double = 0.0,
    val ram_capacity_mb: Int = 0,
    val available_ram_mb: Int = 0,
    val storage_capacity_mb: Int = 0,
    val available_storage_mb: Int = 0,
    val reserved_cpu_cores: Double = 0.0,
    val reserved_ram_mb: Int = 0,
    val reserved_storage_mb: Int = 0,
    val max_hosting_count: Int = 0,
    val hosting_count: Int = 0,
    val running_containers: Int = 0,
    val cpu_usage_percent: Double = 0.0,
    val ram_used_mb: Int = 0,
    val disk_used_mb: Int = 0,
    val uptime_seconds: Int = 0,
    val runtime_type: String = "docker",
    val connection_status: String = "Setup Required",
    val last_error: String? = null,
    val docker_available: Boolean = false,
    val is_enabled: Boolean = true,
    val maintenance_mode: Boolean = false,
    val has_credentials_configured: Boolean = false,
    val last_health_check_at: String? = null,
)

@JsonClass(generateAdapter = true)
data class VpsClusterSummaryDto(
    val total_vps: Int = 0,
    val online_vps: Int = 0,
    val offline_vps: Int = 0,
    val unreachable_vps: Int = 0,
    val total_cpu_cores: Double = 0.0,
    val available_cpu_cores: Double = 0.0,
    val total_ram_mb: Int = 0,
    val available_ram_mb: Int = 0,
    val total_storage_mb: Int = 0,
    val available_storage_mb: Int = 0,
    val total_hosted_containers: Int = 0,
    val total_running_hosting: Int = 0,
)

@JsonClass(generateAdapter = true)
data class AdminDashboardDto(
    val total_users: Int = 0,
    val active_users: Int = 0,
    val total_hosting: Int = 0,
    val running_hosting: Int = 0,
    val stopped_hosting: Int = 0,
    val pending_deposits: Int = 0,
    val total_revenue_bdt: Double = 0.0,
    val vps_summary: VpsClusterSummaryDto = VpsClusterSummaryDto(),
)

@JsonClass(generateAdapter = true)
data class DepositReviewRequestDto(
    val approve: Boolean,
    val admin_note: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssignVpsRequestDto(
    val vps_id: Int,
)

@JsonClass(generateAdapter = true)
data class MigrateVpsRequestDto(
    val target_vps_id: Int,
    val preserve_url: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class MigrateVpsResponseDto(
    val job_id: Int,
    val status: String,
    val hosting: HostingDto,
)

@JsonClass(generateAdapter = true)
data class MaintenanceToggleRequestDto(
    val maintenance_mode: Boolean,
)

@JsonClass(generateAdapter = true)
data class VpsOpLogDto(
    val id: Int,
    val level: String,
    val message: String,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class VpsMetricHistoryDto(
    val id: Int,
    val cpu_usage_percent: Double = 0.0,
    val ram_used_mb: Int = 0,
    val disk_used_mb: Int = 0,
    val network_rx_mb: Double = 0.0,
    val network_tx_mb: Double = 0.0,
    val uptime_seconds: Int = 0,
    val running_containers: Int = 0,
    val hosting_count: Int = 0,
    val recorded_at: String = "",
)

@JsonClass(generateAdapter = true)
data class AnnouncementDto(
    val id: Int,
    val title: String,
    val content: String,
    val is_published: Boolean = true,
    val created_at: String = "",
)

@JsonClass(generateAdapter = true)
data class AnnouncementWriteRequestDto(
    val title: String,
    val content: String,
    val is_published: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SystemSettingsDto(
    val app_name: String = "BotHostX",
    val hosting_domain: String = "",
    val api_base_url: String = "",
    val maintenance_mode: Boolean = false,
    val max_upload_size_mb: Int = 50,
    val allowed_file_types: String = ".py,.php,.html,.css,.js,.json,.txt,.zip,.env",
    val default_vps_selection_policy: String = "most_available_resources",
    val hosting_assignment_policy: String = "auto",
    val bkash_number: String = "",
    val bkash_enabled: Boolean = true,
    val nagad_number: String = "",
    val nagad_enabled: Boolean = true,
    val rocket_number: String = "",
    val rocket_enabled: Boolean = true,
    val manual_payment_instructions: String = "",
    val manual_enabled: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class GenericMessageDto(
    val message: String = "",
)
