package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.utils.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun verifyAppNameAndCurrencyFormatting() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BotHostX", appName)
        assertEquals("৳499.00", FormatUtils.formatBdt(499.0))

        val sessionManager = SessionManager(context)
        assertEquals("https://api.bothostx.com", sessionManager.sanitizeBaseUrl("api.bothostx.com/"))
    }
}
