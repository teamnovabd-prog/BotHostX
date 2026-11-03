package com.example.data.api

import com.example.data.local.SessionManager
import com.example.data.models.ApiErrorResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object NetworkClient {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val dynamicEndpointAndAuthInterceptor = Interceptor { chain ->
        val original = chain.request()
        val configuredBase = SessionManager.normalizeBaseUrl(SessionManager.currentBaseUrlVolatile)

        val targetBase = configuredBase.toHttpUrlOrNull()
        val rewrittenUrl = if (targetBase != null) {
            val encodedPath = original.url.encodedPath
            val basePath = targetBase.encodedPath.trimEnd('/')
            original.url.newBuilder()
                .scheme(targetBase.scheme)
                .host(targetBase.host)
                .port(targetBase.port)
                .encodedPath("$basePath$encodedPath")
                .build()
        } else {
            original.url
        }

        val reqBuilder = original.newBuilder().url(rewrittenUrl)
        val token = SessionManager.currentTokenVolatile
        if (token.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(reqBuilder.build())
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(dynamicEndpointAndAuthInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    val api: SmartHostApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SmartHostApi::class.java)
    }

    fun parseError(throwable: Throwable): String {
        val currentUrl = SessionManager.normalizeBaseUrl(SessionManager.currentBaseUrlVolatile)
        if (currentUrl == SessionManager.DEFAULT_PRODUCTION_PLACEHOLDER_URL) {
            return "Backend API URL is still set to placeholder ($currentUrl). Tap 'Configure Backend API' and enter your deployed FastAPI server URL."
        }
        return when (throwable) {
            is HttpException -> {
                val statusCode = throwable.code()
                val reqPath = throwable.response()?.raw()?.request?.url?.encodedPath.orEmpty()
                val rawErr = try {
                    throwable.response()?.errorBody()?.string().orEmpty()
                } catch (_: Exception) {
                    ""
                }

                if (statusCode == 404) {
                    when {
                        reqPath.endsWith("/health") ->
                            return "Backend verification failed: GET /health endpoint is missing (HTTP 404 Not Found) on $currentUrl. Deploy the complete SmartHost FastAPI backend."
                        reqPath.endsWith("/api/auth/register") || reqPath.endsWith("/api/auth/login") ->
                            return "Authentication endpoint $reqPath is missing (HTTP 404 Not Found) on $currentUrl. Your server appears to be running a starter app (such as 'Hello World') instead of the SmartHost authentication backend."
                    }
                }

                if (rawErr.isNotBlank()) {
                    try {
                        val parsed = moshi.adapter(ApiErrorResponseDto::class.java).fromJson(rawErr)
                        val detail = parsed?.detail
                        when {
                            detail is String && detail.isNotBlank() -> {
                                if (statusCode == 404 && detail.equals("Not Found", ignoreCase = true) && reqPath.isNotBlank()) {
                                    "Endpoint $reqPath was not found (HTTP 404) on $currentUrl. Ensure the complete SmartHost FastAPI backend is deployed."
                                } else {
                                    detail
                                }
                            }
                            detail is List<*> && detail.isNotEmpty() -> {
                                val firstMap = detail.firstOrNull() as? Map<*, *>
                                val msg = firstMap?.get("msg")?.toString()
                                msg ?: "Validation error (HTTP $statusCode)"
                            }
                            !parsed?.message.isNullOrBlank() -> parsed?.message.orEmpty()
                            else -> "Server error (HTTP $statusCode)"
                        }
                    } catch (_: Exception) {
                        "Server error (HTTP $statusCode)"
                    }
                } else {
                    "HTTP $statusCode: ${throwable.message()}"
                }
            }
            is SocketTimeoutException -> {
                "Timed out connecting to $currentUrl. If hosted on Render Free tier, wait ~25 seconds for cold start and try again."
            }
            is IOException -> {
                "Cannot reach SmartHost Backend at $currentUrl. Verify your server is running and HTTPS/network is reachable."
            }
            else -> throwable.localizedMessage ?: "Unexpected request failure"
        }
    }
}
