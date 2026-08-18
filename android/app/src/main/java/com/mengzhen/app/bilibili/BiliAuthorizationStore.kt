package com.mengzhen.app.bilibili

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal enum class BiliAuthorizationMethod(val persistedValue: String) {
    QR_CODE("qr_code"),
    OFFICIAL_CONFIRMATION("official_confirmation"),
}

internal data class BiliAuthorization(
    val method: BiliAuthorizationMethod,
    val accessKey: String? = null,
    val cookieHeader: String? = null,
    val refreshToken: String? = null,
    val authorizedAt: Long,
)

internal class BiliAuthorizationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): BiliAuthorization? {
        val encrypted = preferences.getString(KEY_ENCRYPTED_SESSION, null) ?: return null
        return runCatching {
            val envelope = JSONObject(encrypted)
            val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
            val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
            val session = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
            val method = BiliAuthorizationMethod.entries.firstOrNull {
                it.persistedValue == session.optString("method")
            } ?: return@runCatching null
            BiliAuthorization(
                method = method,
                accessKey = session.optString("access_key").takeIf(String::isNotBlank),
                cookieHeader = session.optString("cookie_header").takeIf(String::isNotBlank),
                refreshToken = session.optString("refresh_token").takeIf(String::isNotBlank),
                authorizedAt = session.optLong("authorized_at"),
            ).takeIf { !it.accessKey.isNullOrBlank() || !it.cookieHeader.isNullOrBlank() }
        }.getOrElse {
            clear()
            null
        }
    }

    fun saveOfficialConfirmation(accessKey: String) {
        require(accessKey.isNotBlank())
        save(
            BiliAuthorization(
                method = BiliAuthorizationMethod.OFFICIAL_CONFIRMATION,
                accessKey = accessKey,
                authorizedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun saveQrCodeAuthorization(cookieHeader: String, refreshToken: String?) {
        require(cookieHeader.isNotBlank())
        save(
            BiliAuthorization(
                method = BiliAuthorizationMethod.QR_CODE,
                cookieHeader = cookieHeader,
                refreshToken = refreshToken?.takeIf(String::isNotBlank),
                authorizedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun clear() {
        preferences.edit().remove(KEY_ENCRYPTED_SESSION).apply()
    }

    private fun save(authorization: BiliAuthorization) {
        val session = JSONObject()
            .put("method", authorization.method.persistedValue)
            .put("access_key", authorization.accessKey.orEmpty())
            .put("cookie_header", authorization.cookieHeader.orEmpty())
            .put("refresh_token", authorization.refreshToken.orEmpty())
            .put("authorized_at", authorization.authorizedAt)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, encryptionKey())
        }
        val envelope = JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put(
                "ciphertext",
                Base64.encodeToString(cipher.doFinal(session), Base64.NO_WRAP),
            )
        preferences.edit().putString(KEY_ENCRYPTED_SESSION, envelope.toString()).apply()
    }

    private fun encryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "bili_authorization"
        const val KEY_ENCRYPTED_SESSION = "encrypted_session"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "com.mengzhen.app.bilibili.authorization"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
