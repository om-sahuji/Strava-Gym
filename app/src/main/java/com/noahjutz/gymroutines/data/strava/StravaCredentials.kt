/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines.data.strava

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Strava client id/secret and the OAuth tokens.
 *
 * Values are encrypted with an AES/GCM key that never leaves the Android Keystore (API 23+).
 * On API 21/22 the Android Keystore does not support symmetric AES keys, so the values are only
 * base64 encoded there (see NOTES.md, "Known limitations").
 *
 * Nothing in this class is ever logged.
 */
class StravaCredentials(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var clientId: String
        get() = read(KEY_CLIENT_ID)
        set(value) = write(KEY_CLIENT_ID, value.trim())

    var clientSecret: String
        get() = read(KEY_CLIENT_SECRET)
        set(value) = write(KEY_CLIENT_SECRET, value.trim())

    var accessToken: String
        get() = read(KEY_ACCESS_TOKEN)
        set(value) = write(KEY_ACCESS_TOKEN, value)

    var refreshToken: String
        get() = read(KEY_REFRESH_TOKEN)
        set(value) = write(KEY_REFRESH_TOKEN, value)

    var expiresAtEpochSec: Long
        get() = read(KEY_EXPIRES_AT).toLongOrNull() ?: 0L
        set(value) = write(KEY_EXPIRES_AT, value.toString())

    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    val isConnected: Boolean
        get() = accessToken.isNotBlank() && refreshToken.isNotBlank()

    fun saveTokens(token: StravaToken) {
        accessToken = token.accessToken
        if (token.refreshToken.isNotEmpty()) refreshToken = token.refreshToken
        expiresAtEpochSec = token.expiresAtEpochSec
    }

    /** Clears the OAuth tokens but keeps the client id/secret so re-connecting is easy. */
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private fun read(key: String): String {
        val stored = prefs.getString(key, null) ?: return ""
        return try {
            val raw = Base64.decode(stored, Base64.NO_WRAP)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                String(raw, Charsets.UTF_8)
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
                String(
                    cipher.doFinal(raw.copyOfRange(GCM_IV_LENGTH, raw.size)),
                    Charsets.UTF_8
                )
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun write(key: String, value: String) {
        if (value.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        try {
            val payload = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                value.toByteArray(Charsets.UTF_8)
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey())
                cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            }
            prefs.edit()
                .putString(key, Base64.encodeToString(payload, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Storing the secret failed; do not fall back to plain text.
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "strava_credentials"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "strava_credentials_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128

        const val KEY_CLIENT_ID = "clientId"
        const val KEY_CLIENT_SECRET = "clientSecret"
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_REFRESH_TOKEN = "refreshToken"
        const val KEY_EXPIRES_AT = "expiresAt"
    }
}
