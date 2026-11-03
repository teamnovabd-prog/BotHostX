package com.example.data.api

import com.example.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SmartHostApi {

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @GET("/")
    suspend fun probeRoot(): Response<ResponseBody>

    // --- Authentication ---
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthTokenDto

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthTokenDto

    @POST("api/auth/logout")
    suspend fun logout(): GenericMessageDto

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): GenericMessageDto

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): GenericMessageDto

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto): GenericMessageDto

    @GET("api/auth/me")
    suspend fun getMe(): UserDto

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequestDto): UserDto

    // --- Dashboard ---
    @GET("api/dashboard")
    suspend fun getDashboard(): DashboardDto

    // --- Hosting ---
    @GET("api/hosting")
    suspend fun listHosting(@Query("hosting_type") hostingType: String? = null): List<HostingDto>

    @POST("api/hosting")
    suspend fun createHosting(@Body body: CreateHostingRequestDto): HostingDto

    @GET("api/hosting/{hostingId}")
    suspend fun getHostingDetail(@Path("hostingId") hostingId: Int): HostingDto

    @POST("api/hosting/{hostingId}/start")
    suspend fun startHosting(@Path("hostingId") hostingId: Int): HostingDto

    @POST("api/hosting/{hostingId}/stop")
    suspend fun stopHosting(@Path("hostingId") hostingId: Int): HostingDto

    @POST("api/hosting/{hostingId}/restart")
    suspend fun restartHosting(@Path("hostingId") hostingId: Int): HostingDto

    @POST("api/hosting/{hostingId}/install-deps")
    suspend fun installDependencies(@Path("hostingId") hostingId: Int): HostingDto

    @PUT("api/hosting/{hostingId}/bot-config")
    suspend fun updateBotConfig(
        @Path("hostingId") hostingId: Int,
        @Body body: UpdateBotConfigRequestDto,
    ): HostingDto

    @DELETE("api/hosting/{hostingId}")
    suspend fun deleteHosting(@Path("hostingId") hostingId: Int): GenericMessageDto

    @GET("api/hosting/{hostingId}/logs")
    suspend fun getHostingLogs(
        @Path("hostingId") hostingId: Int,
        @Query("log_type") logType: String? = null,
    ): List<LogEntryDto>

    // --- File Manager ---
    @GET("api/hosting/{hostingId}/files")
    suspend fun listFiles(
        @Path("hostingId") hostingId: Int,
        @Query("directory") directory: String = "",
        @Query("search") search: String? = null,
    ): List<FileItemDto>

    @Multipart
    @POST("api/hosting/{hostingId}/upload")
    suspend fun uploadFile(
        @Path("hostingId") hostingId: Int,
        @Part("directory") directory: RequestBody,
        @Part("auto_extract_zip") autoExtractZip: RequestBody,
        @Part file: MultipartBody.Part,
    ): FileItemDto

    @POST("api/hosting/{hostingId}/folder")
    suspend fun createFolder(
        @Path("hostingId") hostingId: Int,
        @Body body: CreateFolderRequestDto,
    ): FileItemDto

    @PUT("api/hosting/{hostingId}/files/rename")
    suspend fun renameFile(
        @Path("hostingId") hostingId: Int,
        @Body body: RenameFileRequestDto,
    ): FileItemDto

    @POST("api/hosting/{hostingId}/files/extract")
    suspend fun extractZip(
        @Path("hostingId") hostingId: Int,
        @Body body: ExtractZipRequestDto,
    ): GenericMessageDto

    @GET("api/hosting/{hostingId}/files/content")
    suspend fun readFileContent(
        @Path("hostingId") hostingId: Int,
        @Query("path") path: String,
    ): FileContentDto

    @PUT("api/hosting/{hostingId}/files/content")
    suspend fun saveFileContent(
        @Path("hostingId") hostingId: Int,
        @Body body: FileContentDto,
    ): GenericMessageDto

    @DELETE("api/hosting/{hostingId}/files")
    suspend fun deleteFile(
        @Path("hostingId") hostingId: Int,
        @Query("path") path: String,
    ): GenericMessageDto

    // --- Plans & Wallet ---
    @GET("api/plans")
    suspend fun listPlans(): List<PlanDto>

    @GET("api/plans/current")
    suspend fun getCurrentPlan(): PlanDto?

    @POST("api/plans/purchase")
    suspend fun purchasePlan(@Body body: PurchasePlanRequestDto): PlanDto

    @GET("api/wallet")
    suspend fun getWalletOverview(): WalletOverviewDto

    @GET("api/wallet/transactions")
    suspend fun listTransactions(): List<WalletTransactionDto>

    @GET("api/wallet/deposits")
    suspend fun listDeposits(): List<DepositDto>

    @POST("api/wallet/deposit")
    suspend fun submitDeposit(@Body body: DepositCreateRequestDto): DepositDto

    // --- Support & Notifications ---
    @GET("api/support/tickets")
    suspend fun listTickets(): List<SupportTicketDto>

    @POST("api/support/tickets")
    suspend fun createTicket(@Body body: SupportTicketCreateRequestDto): SupportTicketDto

    @GET("api/support/tickets/{ticketId}")
    suspend fun getTicketDetail(@Path("ticketId") ticketId: Int): SupportTicketDto

    @POST("api/support/tickets/{ticketId}/reply")
    suspend fun replyTicket(
        @Path("ticketId") ticketId: Int,
        @Body body: SupportReplyRequestDto,
    ): SupportTicketDto

    @GET("api/notifications")
    suspend fun listNotifications(): List<NotificationDto>

    @POST("api/notifications/{notificationId}/read")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: Int): GenericMessageDto

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): GenericMessageDto

    // --- Admin Panel ---
    @GET("api/admin/dashboard")
    suspend fun getAdminDashboard(): AdminDashboardDto

    @GET("api/admin/users")
    suspend fun adminListUsers(@Query("search") search: String? = null): List<UserDto>

    @POST("api/admin/users/{userId}/suspend")
    suspend fun adminSuspendUser(@Path("userId") userId: Int): UserDto

    @POST("api/admin/users/{userId}/activate")
    suspend fun adminActivateUser(@Path("userId") userId: Int): UserDto

    @DELETE("api/admin/users/{userId}")
    suspend fun adminDeleteUser(@Path("userId") userId: Int): GenericMessageDto

    @GET("api/admin/hosting")
    suspend fun adminListAllHosting(@Query("search") search: String? = null): List<HostingDto>

    @GET("api/admin/plans")
    suspend fun adminListPlans(): List<PlanDto>

    @POST("api/admin/plans")
    suspend fun adminCreatePlan(@Body body: PlanWriteRequestDto): PlanDto

    @PUT("api/admin/plans/{planId}")
    suspend fun adminUpdatePlan(
        @Path("planId") planId: Int,
        @Body body: PlanWriteRequestDto,
    ): PlanDto

    @DELETE("api/admin/plans/{planId}")
    suspend fun adminDeletePlan(@Path("planId") planId: Int): GenericMessageDto

    @GET("api/admin/deposits")
    suspend fun adminListDeposits(@Query("status_filter") statusFilter: String? = null): List<DepositDto>

    @POST("api/admin/deposits/{depositId}/review")
    suspend fun adminReviewDeposit(
        @Path("depositId") depositId: Int,
        @Body body: DepositReviewRequestDto,
    ): DepositDto

    @GET("api/admin/support/tickets")
    suspend fun adminListTickets(): List<SupportTicketDto>

    @POST("api/admin/support/tickets/{ticketId}/status")
    suspend fun adminUpdateTicketStatus(
        @Path("ticketId") ticketId: Int,
        @Query("new_status") newStatus: String,
    ): SupportTicketDto

    @GET("api/admin/announcements")
    suspend fun adminListAnnouncements(): List<AnnouncementDto>

    @POST("api/admin/announcements")
    suspend fun adminCreateAnnouncement(@Body body: AnnouncementWriteRequestDto): AnnouncementDto

    @DELETE("api/admin/announcements/{announcementId}")
    suspend fun adminDeleteAnnouncement(@Path("announcementId") announcementId: Int): GenericMessageDto

    @GET("api/admin/settings")
    suspend fun adminGetSettings(): SystemSettingsDto

    @PUT("api/admin/settings")
    suspend fun adminUpdateSettings(@Body body: SystemSettingsDto): SystemSettingsDto

    // --- Multi-VPS Management ---
    @GET("api/admin/vps/summary")
    suspend fun adminGetVpsSummary(): VpsClusterSummaryDto

    @GET("api/admin/vps")
    suspend fun adminListVps(): List<VpsDto>

    @POST("api/admin/vps")
    suspend fun adminAddVps(@Body body: VpsCreateUpdateRequestDto): VpsDto

    @GET("api/admin/vps/{vpsId}")
    suspend fun adminGetVpsDetail(@Path("vpsId") vpsId: Int): VpsDto

    @PUT("api/admin/vps/{vpsId}")
    suspend fun adminUpdateVps(
        @Path("vpsId") vpsId: Int,
        @Body body: VpsCreateUpdateRequestDto,
    ): VpsDto

    @DELETE("api/admin/vps/{vpsId}")
    suspend fun adminRemoveVps(@Path("vpsId") vpsId: Int): GenericMessageDto

    @POST("api/admin/vps/{vpsId}/test-connection")
    suspend fun adminTestVpsConnection(@Path("vpsId") vpsId: Int): VpsDto

    @POST("api/admin/vps/{vpsId}/connect")
    suspend fun adminConnectVps(@Path("vpsId") vpsId: Int): VpsDto

    @POST("api/admin/vps/{vpsId}/disconnect")
    suspend fun adminDisconnectVps(@Path("vpsId") vpsId: Int): VpsDto

    @POST("api/admin/vps/{vpsId}/enable")
    suspend fun adminEnableVps(@Path("vpsId") vpsId: Int): VpsDto

    @POST("api/admin/vps/{vpsId}/disable")
    suspend fun adminDisableVps(@Path("vpsId") vpsId: Int): VpsDto

    @POST("api/admin/vps/{vpsId}/maintenance")
    suspend fun adminSetVpsMaintenance(
        @Path("vpsId") vpsId: Int,
        @Body body: MaintenanceToggleRequestDto,
    ): VpsDto

    @GET("api/admin/vps/{vpsId}/metrics")
    suspend fun adminGetVpsMetrics(@Path("vpsId") vpsId: Int): List<VpsMetricHistoryDto>

    @GET("api/admin/vps/{vpsId}/hosting")
    suspend fun adminGetVpsHosting(@Path("vpsId") vpsId: Int): List<HostingDto>

    @GET("api/admin/vps/{vpsId}/logs")
    suspend fun adminGetVpsLogs(@Path("vpsId") vpsId: Int): List<VpsOpLogDto>

    @POST("api/admin/hosting/{hostingId}/assign-vps")
    suspend fun adminAssignHostingVps(
        @Path("hostingId") hostingId: Int,
        @Body body: AssignVpsRequestDto,
    ): HostingDto

    @POST("api/admin/hosting/{hostingId}/migrate-vps")
    suspend fun adminMigrateHostingVps(
        @Path("hostingId") hostingId: Int,
        @Body body: MigrateVpsRequestDto,
    ): MigrateVpsResponseDto
}
