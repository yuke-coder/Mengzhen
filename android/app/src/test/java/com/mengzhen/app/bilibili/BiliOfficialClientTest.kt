package com.mengzhen.app.bilibili

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliOfficialClientTest {
    @Test
    fun `qr authorization account takes precedence over installed app account`() {
        val authorization = BiliAuthorization(
            method = BiliAuthorizationMethod.QR_CODE,
            cookieHeader = "DedeUserID=222; SESSDATA=encrypted",
            authorizedAt = 1L,
        )

        val savedUid = extractBiliUidFromCookies(authorization.cookieHeader.orEmpty())

        assertEquals(
            222L,
            selectAuthorizedBiliUid(
                providerUid = 111L,
                savedAuthorization = authorization,
                savedUid = savedUid,
            ),
        )
    }

    @Test
    fun `official confirmation keeps installed app account identity`() {
        val authorization = BiliAuthorization(
            method = BiliAuthorizationMethod.OFFICIAL_CONFIRMATION,
            accessKey = "opaque-access-key",
            authorizedAt = 1L,
        )

        assertEquals(
            111L,
            selectAuthorizedBiliUid(
                providerUid = 111L,
                savedAuthorization = authorization,
                savedUid = null,
            ),
        )
    }

    @Test
    fun `malformed qr identity falls back to installed app account`() {
        val authorization = BiliAuthorization(
            method = BiliAuthorizationMethod.QR_CODE,
            cookieHeader = "SESSDATA=encrypted",
            authorizedAt = 1L,
        )

        assertEquals(
            111L,
            selectAuthorizedBiliUid(
                providerUid = 111L,
                savedAuthorization = authorization,
                savedUid = extractBiliUidFromCookies(authorization.cookieHeader.orEmpty()),
            ),
        )
    }
}
