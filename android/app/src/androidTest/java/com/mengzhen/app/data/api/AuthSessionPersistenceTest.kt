package com.mengzhen.app.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mengzhen.app.data.model.parseUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthSessionPersistenceTest {

    @Test
    fun loginCookieSurvivesClientRecreation() {
        val arguments = InstrumentationRegistry.getArguments()
        val username = arguments.getString("authUsername").orEmpty()
        val password = arguments.getString("authPassword").orEmpty()
        assertTrue("authUsername is required", username.isNotBlank())
        assertTrue("authPassword is required", password.isNotBlank())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var api = ApiClient.get(context)
        api.clearCookies()

        try {
            assertTrue(api.login(username, password).optBoolean("success", false))
            assertEquals(username, parseUser(api.me())?.username)

            resetApiClientSingleton()
            api = ApiClient.get(context)

            val restored = parseUser(api.me())
            assertNotNull(restored)
            assertEquals(username, restored?.username)
        } finally {
            runCatching { api.logout() }
            resetApiClientSingleton()
        }
    }

    private fun resetApiClientSingleton() {
        ApiClient::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            set(null, null)
        }
    }
}
