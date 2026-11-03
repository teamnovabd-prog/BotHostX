package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.api.NetworkClient
import com.example.data.api.SmartHostApi
import com.example.data.local.SessionManager
import com.example.data.models.ApiErrorResponseDto
import com.example.data.models.FileItemDto
import com.example.data.models.HealthResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class BackendCheckOutcome(
    val verified: Boolean,
    val message: String,
)

object BackendHealthValidator {

    fun validateHealthPayload(
        httpCode: Int,
        rawBody: String?,
        baseUrl: String,
        rootProbeBody: String? = null,
    ): BackendCheckOutcome {
        if (baseUrl == SessionManager.DEFAULT_PRODUCTION_PLACEHOLDER_URL) {
            return BackendCheckOutcome(
                verified = false,
                message = "API Base URL is still set to the placeholder ($baseUrl). Enter your deployed FastAPI URL (e.g., https://smarthost-api.onrender.com).",
            )
        }

        if (httpCode == 404) {
            val isHelloWorldServer = rootProbeBody?.contains("Hello World", ignoreCase = true) == true
            return if (isHelloWorldServer) {
                BackendCheckOutcome(
                    verified = false,
                    message = "Missing GET /health endpoint (HTTP 404). Your server at $baseUrl is currently returning {\"message\":\"Hello World\"} at '/' instead of the SmartHost authentication backend. Deploy the complete FastAPI backend with /health, /api/auth/register, and /api/auth/login.",
                )
            } else {
                BackendCheckOutcome(
                    verified = false,
                    message = "Backend verification failed: GET /health endpoint is missing (HTTP 404 Not Found) at $baseUrl. Deploy the SmartHost FastAPI backend and verify again.",
                )
            }
        }

        if (httpCode !in 200..299) {
            val detailMsg = try {
                val parsed = rawBody?.takeIf { it.isNotBlank() }?.let {
                    NetworkClient.moshi.adapter(ApiErrorResponseDto::class.java).fromJson(it)
                }
                (parsed?.detail as? String) ?: parsed?.message
            } catch (_: Exception) {
                null
            }
            val suffix = if (!detailMsg.isNullOrBlank()) ": $detailMsg" else ""
            return BackendCheckOutcome(
                verified = false,
                message = "GET /health check failed (HTTP $httpCode) at $baseUrl$suffix",
            )
        }

        val body = rawBody?.trim().orEmpty()
        if (body.isEmpty()) {
            return BackendCheckOutcome(
                verified = false,
                message = "GET /health returned an empty response from $baseUrl. Expected SmartHost JSON health status.",
            )
        }

        if (body.contains("Hello World", ignoreCase = true)) {
            return BackendCheckOutcome(
                verified = false,
                message = "Verification rejected: $baseUrl returned a 'Hello World' placeholder response instead of a working SmartHost authentication backend. Deploy /health, /api/auth/register, and /api/auth/login with PostgreSQL.",
            )
        }

        val dto = try {
            NetworkClient.moshi.adapter(HealthResponseDto::class.java).fromJson(body)
        } catch (_: Exception) {
            null
        } ?: return BackendCheckOutcome(
            verified = false,
            message = "GET /health at $baseUrl returned non-JSON content. Verify that your URL points to the FastAPI backend.",
        )

        val isStatusOk = dto.status.equals("ok", ignoreCase = true)
        val hasSmartHostSignature = dto.auth_ready || dto.service.equals("smarthost-api", ignoreCase = true)

        if (!isStatusOk || !hasSmartHostSignature) {
            return BackendCheckOutcome(
                verified = false,
                message = "Server at $baseUrl responded, but is NOT a verified SmartHost authentication backend (expected status='ok' and auth_ready=true on GET /health). A simple Hello World or generic stub cannot handle Register/Login.",
            )
        }

        val platformName = dto.platform.ifBlank { "BotHostX" }
        val dbState = dto.database.ifBlank { "connected" }
        val maintSuffix = if (dto.maintenance_mode) " • Maintenance Active" else ""
        return BackendCheckOutcome(
            verified = true,
            message = "Verified $platformName backend (GET /health OK • PostgreSQL $dbState • Auth Ready)$maintSuffix",
        )
    }
}

class SmartHostRepository(
    private val context: Context,
    private val api: SmartHostApi = NetworkClient.api,
) {

    suspend fun <T> safeCall(block: suspend SmartHostApi.() -> T): Result<T> {
        return try {
            Result.success(api.block())
        } catch (t: Throwable) {
            Result.failure(Exception(NetworkClient.parseError(t), t))
        }
    }

    suspend fun verifyBackendEndpoint(baseUrl: String): BackendCheckOutcome {
        val normalizedUrl = SessionManager.normalizeBaseUrl(baseUrl)
        if (normalizedUrl == SessionManager.DEFAULT_PRODUCTION_PLACEHOLDER_URL) {
            return BackendHealthValidator.validateHealthPayload(0, null, normalizedUrl)
        }
        return try {
            val healthResponse = api.checkHealth()
            val code = healthResponse.code()
            val rawBody = if (healthResponse.isSuccessful) {
                healthResponse.body()?.string()
            } else {
                healthResponse.errorBody()?.string()
            }

            val rootProbeBody = if (code == 404) {
                runCatching {
                    val rootResp = api.probeRoot()
                    if (rootResp.isSuccessful) rootResp.body()?.string() else null
                }.getOrNull()
            } else {
                null
            }

            BackendHealthValidator.validateHealthPayload(
                httpCode = code,
                rawBody = rawBody,
                baseUrl = normalizedUrl,
                rootProbeBody = rootProbeBody,
            )
        } catch (t: Throwable) {
            BackendCheckOutcome(
                verified = false,
                message = NetworkClient.parseError(t),
            )
        }
    }

    suspend fun uploadUriToHosting(
        hostingId: Int,
        directory: String,
        autoExtractZip: Boolean,
        uri: Uri,
    ): Result<FileItemDto> {
        return try {
            val resolver = context.contentResolver
            var displayName = "uploaded_file.txt"
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Unable to read selected file from Android File Picker"))

            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val fileReqBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", displayName, fileReqBody)
            val dirBody = directory.toRequestBody("text/plain".toMediaTypeOrNull())
            val extractBody = autoExtractZip.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val uploaded = api.uploadFile(
                hostingId = hostingId,
                directory = dirBody,
                autoExtractZip = extractBody,
                file = part,
            )
            Result.success(uploaded)
        } catch (t: Throwable) {
            Result.failure(Exception(NetworkClient.parseError(t), t))
        }
    }
}
