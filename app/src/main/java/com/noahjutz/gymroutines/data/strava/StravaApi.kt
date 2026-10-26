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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StravaToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSec: Long,
)

sealed class StravaResult<out T> {
    data class Success<T>(val value: T) : StravaResult<T>()
    data class HttpError(val code: Int) : StravaResult<Nothing>()
    data class NetworkError(val message: String) : StravaResult<Nothing>()

    /** Client id / secret have not been entered yet. */
    data object NotConfigured : StravaResult<Nothing>()

    /** Credentials are present, but Strava is not connected (or the connection broke). */
    data object NotConnected : StravaResult<Nothing>()
}

/** Re-types a failed result so it can be returned from a function with a different value type. */
fun StravaResult<*>.asFailure(): StravaResult<Nothing>? = when (this) {
    is StravaResult.Success -> null
    is StravaResult.HttpError -> StravaResult.HttpError(code)
    is StravaResult.NetworkError -> StravaResult.NetworkError(message)
    StravaResult.NotConfigured -> StravaResult.NotConfigured
    StravaResult.NotConnected -> StravaResult.NotConnected
}

/**
 * Minimal Strava v3 API client built on [HttpURLConnection]. No third-party HTTP dependency is
 * added. Tokens and secrets are never logged.
 */
class StravaApi {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchangeCode(
        clientId: String,
        clientSecret: String,
        code: String,
    ): StravaResult<StravaToken> {
        return requestToken(
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "code" to code,
                "grant_type" to "authorization_code",
            )
        )
    }

    suspend fun refreshToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
    ): StravaResult<StravaToken> {
        return requestToken(
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "refresh_token" to refreshToken,
                "grant_type" to "refresh_token",
            )
        )
    }

    /** Returns the raw JSON body of the activities page. */
    suspend fun getActivities(
        accessToken: String,
        page: Int,
        perPage: Int,
        afterEpochSec: Long?,
    ): StravaResult<String> {
        val url = StringBuilder("$BASE_URL/athlete/activities?per_page=$perPage&page=$page")
        if (afterEpochSec != null) url.append("&after=$afterEpochSec")
        return get(url.toString(), accessToken)
    }

    private suspend fun requestToken(form: Map<String, String>): StravaResult<StravaToken> {
        return when (val result = post("$AUTHORIZE_BASE_URL/oauth/token", form)) {
            is StravaResult.Success -> {
                val token = try {
                    json.decodeFromString<StravaTokenDto>(result.value)
                } catch (e: Exception) {
                    null
                }
                if (token == null) {
                    StravaResult.NetworkError("Unexpected response from Strava")
                } else {
                    StravaResult.Success(
                        StravaToken(
                            accessToken = token.accessToken,
                            refreshToken = token.refreshToken ?: "",
                            expiresAtEpochSec = token.expiresAt,
                        )
                    )
                }
            }
            else -> result.asFailure()!!
        }
    }

    private suspend fun get(url: String, accessToken: String): StravaResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection(url).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                readResponse(connection)
            } catch (e: Exception) {
                StravaResult.NetworkError(e.message ?: e.javaClass.simpleName)
            }
        }

    private suspend fun post(url: String, form: Map<String, String>): StravaResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = openConnection(url).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                val body = form.entries.joinToString("&") { (key, value) ->
                    "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                readResponse(connection)
            } catch (e: Exception) {
                StravaResult.NetworkError(e.message ?: e.javaClass.simpleName)
            }
        }

    private fun readResponse(connection: HttpURLConnection): StravaResult<String> {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()
        return if (code in 200..299) {
            StravaResult.Success(text)
        } else {
            StravaResult.HttpError(code)
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
        }
    }

    companion object {
        private const val AUTHORIZE_BASE_URL = "https://www.strava.com"
        const val BASE_URL = "https://www.strava.com/api/v3"
        const val AUTHORIZE_URL = "https://www.strava.com/oauth/mobile/authorize"
        const val REDIRECT_URI = "omfit://localhost/strava"
        private const val TIMEOUT_MS = 20_000
    }
}
