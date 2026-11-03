package com.example

import com.example.data.local.SessionManager
import com.example.data.repository.BackendHealthValidator
import com.example.utils.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun bdtCurrencyAndTelemetryFormatting_isAccurate() {
        assertEquals("৳0.00", FormatUtils.formatBdt(0.0))
        assertEquals("৳1,499.50", FormatUtils.formatBdt(1499.5))
        assertEquals("512 B", FormatUtils.formatBytes(512))
        assertEquals("2.0 KB", FormatUtils.formatBytes(2048))
        assertEquals("5.0 GB", FormatUtils.formatStorageMb(5120))
        assertEquals("2d 3h", FormatUtils.formatUptime(183600))
    }

    @Test
    fun normalizeBaseUrl_stripsEndpointSuffixesAndTrailingSlashes() {
        assertEquals(
            "https://smarthost-api.onrender.com",
            SessionManager.normalizeBaseUrl("https://smarthost-api.onrender.com/"),
        )
        assertEquals(
            "https://smarthost-api.onrender.com",
            SessionManager.normalizeBaseUrl("https://smarthost-api.onrender.com/health"),
        )
        assertEquals(
            "https://smarthost-api.onrender.com",
            SessionManager.normalizeBaseUrl("smarthost-api.onrender.com/api/health/"),
        )
        assertEquals(
            "https://smarthost-api.onrender.com",
            SessionManager.normalizeBaseUrl("https://smarthost-api.onrender.com/api/auth/login"),
        )
    }

    @Test
    fun backendHealthValidator_rejectsMissingHealthAndHelloWorldServers() {
        // Case 1: GET /health returns 404 while GET / returns {"message":"Hello World"}
        val missingWithHelloRoot = BackendHealthValidator.validateHealthPayload(
            httpCode = 404,
            rawBody = """{"detail":"Not Found"}""",
            baseUrl = "https://demo-starter.onrender.com",
            rootProbeBody = """{"message":"Hello World"}""",
        )
        assertFalse(missingWithHelloRoot.verified)
        assertTrue(missingWithHelloRoot.message.contains("GET /health", ignoreCase = true))
        assertTrue(missingWithHelloRoot.message.contains("Hello World", ignoreCase = true))

        // Case 2: GET /health returns 404 without Hello World root
        val missingPlain = BackendHealthValidator.validateHealthPayload(
            httpCode = 404,
            rawBody = """{"detail":"Not Found"}""",
            baseUrl = "https://demo-starter.onrender.com",
            rootProbeBody = null,
        )
        assertFalse(missingPlain.verified)
        assertTrue(missingPlain.message.contains("404", ignoreCase = true))

        // Case 3: Endpoint returns 200 OK with {"message":"Hello World"} — MUST NOT pass verification
        val helloWorld200 = BackendHealthValidator.validateHealthPayload(
            httpCode = 200,
            rawBody = """{"message":"Hello World"}""",
            baseUrl = "https://demo-starter.onrender.com",
        )
        assertFalse(helloWorld200.verified)
        assertTrue(helloWorld200.message.contains("Hello World", ignoreCase = true))

        // Case 4: Generic {"status":"ok"} stub without SmartHost auth_ready / smarthost-api — MUST NOT pass
        val genericStub = BackendHealthValidator.validateHealthPayload(
            httpCode = 200,
            rawBody = """{"status":"ok"}""",
            baseUrl = "https://generic-stub.onrender.com",
        )
        assertFalse(genericStub.verified)
    }

    @Test
    fun backendHealthValidator_acceptsVerifiedSmartHostHealthEndpoint() {
        val valid = BackendHealthValidator.validateHealthPayload(
            httpCode = 200,
            rawBody = """{"status":"ok","service":"smarthost-api","platform":"BotHostX","auth_ready":true,"database":"connected","maintenance_mode":false}""",
            baseUrl = "https://smarthost-api.onrender.com",
        )
        assertTrue(valid.verified)
        assertTrue(valid.message.contains("BotHostX"))
        assertTrue(valid.message.contains("PostgreSQL connected"))
    }
}
