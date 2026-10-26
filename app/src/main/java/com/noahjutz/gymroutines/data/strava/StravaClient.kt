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

import java.net.URLEncoder

/**
 * Owns the Strava OAuth2 authorization-code flow and token lifecycle. Access tokens are refreshed
 * automatically when they expire (or expire within [REFRESH_MARGIN_SEC]) and after an HTTP 401.
 */
class StravaClient(private val credentials: StravaCredentials) {

    private val api = StravaApi()
    private val refreshLock = Any()

    val isConnected: Boolean get() = credentials.isConnected
    val isConfigured: Boolean get() = credentials.isConfigured

    /** Returns the browser URL that starts the authorization-code flow, or null if unconfigured. */
    fun authorizationUrl(): String? {
        if (!credentials.isConfigured) return null
        return buildString {
            append(StravaApi.AUTHORIZE_URL)
            append("?client_id=")
            append(URLEncoder.encode(credentials.clientId, "UTF-8"))
            append("&redirect_uri=")
            append(URLEncoder.encode(StravaApi.REDIRECT_URI, "UTF-8"))
            append("&response_type=code")
            append("&approval_prompt=auto")
            append("&scope=")
            append(URLEncoder.encode("activity:read_all", "UTF-8"))
        }
    }

    /** Exchanges an authorization code for tokens and persists them. */
    suspend fun connectWithCode(code: String): StravaResult<Unit> {
        if (!credentials.isConfigured) return StravaResult.NotConfigured
        return when (
            val result = api.exchangeCode(
                clientId = credentials.clientId,
                clientSecret = credentials.clientSecret,
                code = code,
            )
        ) {
            is StravaResult.Success -> {
                credentials.saveTokens(result.value)
                StravaResult.Success(Unit)
            }
            else -> result.asFailure()!!
        }
    }

    fun disconnect() {
        credentials.clearTokens()
    }

    /**
     * Fetches one page of activities from Strava. Refreshes the token first when needed and retries
     * once after an HTTP 401.
     */
    suspend fun activities(
        page: Int,
        perPage: Int = PER_PAGE,
        afterEpochSec: Long? = null,
    ): StravaResult<String> {
        val token = validAccessToken()
        if (token !is StravaResult.Success) return token.asFailure()!!

        var result = api.getActivities(token.value, page, perPage, afterEpochSec)
        if (result is StravaResult.HttpError && result.code == 401) {
            val refresh = refreshTokens()
            if (refresh !is StravaResult.Success) return refresh.asFailure()!!
            result = api.getActivities(credentials.accessToken, page, perPage, afterEpochSec)
        }
        return result
    }

    suspend fun refreshTokens(): StravaResult<Unit> {
        if (!credentials.isConfigured) return StravaResult.NotConfigured
        if (credentials.refreshToken.isBlank()) return StravaResult.NotConnected
        return when (
            val result = api.refreshToken(
                clientId = credentials.clientId,
                clientSecret = credentials.clientSecret,
                refreshToken = credentials.refreshToken,
            )
        ) {
            is StravaResult.Success -> {
                credentials.saveTokens(
                    result.value.copy(
                        refreshToken = result.value.refreshToken.ifEmpty {
                            credentials.refreshToken
                        }
                    )
                )
                StravaResult.Success(Unit)
            }
            else -> result.asFailure()!!
        }
    }

    private suspend fun validAccessToken(): StravaResult<String> {
        if (!credentials.isConfigured) return StravaResult.NotConfigured
        if (!credentials.isConnected) return StravaResult.NotConnected
        val expiresInSec = credentials.expiresAtEpochSec - System.currentTimeMillis() / 1000
        if (expiresInSec > REFRESH_MARGIN_SEC) return StravaResult.Success(credentials.accessToken)

        synchronized(refreshLock) {
            // Another caller may have refreshed already.
            if (credentials.expiresAtEpochSec - System.currentTimeMillis() / 1000 > REFRESH_MARGIN_SEC) {
                return StravaResult.Success(credentials.accessToken)
            }
        }
        return when (val refresh = refreshTokens()) {
            is StravaResult.Success -> StravaResult.Success(credentials.accessToken)
            else -> refresh.asFailure()!!
        }
    }

    companion object {
        const val PER_PAGE = 100
        private const val REFRESH_MARGIN_SEC = 60L
    }
}
