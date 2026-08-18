package com.mengzhen.app.bilibili

import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class BiliQrAuthorizationClientTest {
    @Test
    fun `transient dns failure keeps qr polling alive`() = runBlocking {
        val offlineClient = OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    throw UnknownHostException(hostname)
                }
            })
            .build()

        val result = BiliQrAuthorizationClient(offlineClient).poll("test-key")

        assertEquals(BiliQrPollResult.Retrying, result)
    }
}
