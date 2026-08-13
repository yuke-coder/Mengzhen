package com.mengzhen.app.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

@RunWith(AndroidJUnit4::class)
class AuthSessionPersistenceTest {

    @Test
    fun persistedCookieSurvivesClientRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var api = ApiClient.get(context)
        api.clearCookies()

        try {
            val url = ApiClient.BASE_URL.toHttpUrl()
            val cookie = Cookie.Builder()
                .name("mengzhen_session")
                .value("instrumentation-session")
                .hostOnlyDomain(url.host)
                .path("/")
                .expiresAt(System.currentTimeMillis() + 60_000)
                .httpOnly()
                .secure()
                .build()
            api.saveFromResponse(url, listOf(cookie))
            assertEquals("instrumentation-session", api.loadForRequest(url).single().value)

            resetApiClientSingleton()
            api = ApiClient.get(context)

            assertTrue(
                api.loadForRequest(url).any { it.name == "mengzhen_session" && it.value == "instrumentation-session" },
            )
        } finally {
            api.clearCookies()
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
