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

package com.noahjutz.gymroutines.ui.settings.strava

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.strava.StravaAuthCallback
import com.noahjutz.gymroutines.data.strava.StravaClient
import com.noahjutz.gymroutines.data.strava.StravaCredentials
import com.noahjutz.gymroutines.data.strava.StravaResult
import com.noahjutz.gymroutines.util.openUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

data class StravaSettingsState(
    val clientId: String = "",
    val clientSecret: String = "",
    val isConnected: Boolean = false,
    val isBusy: Boolean = false,
    @StringRes val message: Int? = null,
    val messageDetail: String? = null,
    val hasPendingCode: Boolean = false,
)

class StravaSettingsViewModel(
    private val client: StravaClient,
    private val credentials: StravaCredentials,
    private val application: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(
        StravaSettingsState(
            clientId = credentials.clientId,
            clientSecret = credentials.clientSecret,
            isConnected = credentials.isConnected,
            hasPendingCode = StravaAuthCallback.code.value != null,
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            StravaAuthCallback.error.filterNotNull().collect { error ->
                StravaAuthCallback.consumeError()
                _state.value = _state.value.copy(
                    message = R.string.strava_error_generic,
                    messageDetail = error
                )
            }
        }
        viewModelScope.launch {
            StravaAuthCallback.code.filterNotNull().collect { code ->
                exchange(StravaAuthCallback.consumeCode() ?: code)
            }
        }
    }

    fun setClientId(value: String) {
        _state.value = _state.value.copy(clientId = value)
    }

    fun setClientSecret(value: String) {
        _state.value = _state.value.copy(clientSecret = value)
    }

    fun saveCredentials() {
        credentials.clientId = _state.value.clientId
        credentials.clientSecret = _state.value.clientSecret
        _state.value = _state.value.copy(
            isConnected = credentials.isConnected,
            message = R.string.strava_credentials_saved,
            messageDetail = null,
        )
    }

    /** Stores the credentials and opens the Strava authorization page in the browser. */
    fun connect() {
        credentials.clientId = _state.value.clientId
        credentials.clientSecret = _state.value.clientSecret
        val url = client.authorizationUrl()
        if (url == null) {
            _state.value = _state.value.copy(
                message = R.string.strava_error_missing_credentials,
                messageDetail = null
            )
            return
        }
        _state.value = _state.value.copy(message = null, messageDetail = null)
        application.openUrl(url)
    }

    /** Fallback for when the browser redirect is not caught: exchange a pasted code. */
    fun exchangePastedCode(input: String) {
        val code = extractCode(input)
        if (code.isBlank()) {
            _state.value = _state.value.copy(
                message = R.string.strava_error_invalid_code,
                messageDetail = null
            )
            return
        }
        credentials.clientId = _state.value.clientId
        credentials.clientSecret = _state.value.clientSecret
        exchange(code)
    }

    private fun exchange(code: String) {
        if (_state.value.isBusy) return
        _state.value = _state.value.copy(isBusy = true, message = null, messageDetail = null)
        viewModelScope.launch {
            val result = client.connectWithCode(code)
            _state.value = when (result) {
                is StravaResult.Success -> _state.value.copy(
                    isBusy = false,
                    isConnected = credentials.isConnected,
                    hasPendingCode = false,
                    message = R.string.strava_connected,
                    messageDetail = null,
                )
                is StravaResult.HttpError -> _state.value.copy(
                    isBusy = false,
                    hasPendingCode = false,
                    message = R.string.strava_error_exchange,
                    messageDetail = "HTTP ${result.code}",
                )
                is StravaResult.NetworkError -> _state.value.copy(
                    isBusy = false,
                    hasPendingCode = false,
                    message = R.string.strava_error_network,
                    messageDetail = result.message,
                )
                StravaResult.NotConfigured -> _state.value.copy(
                    isBusy = false,
                    hasPendingCode = false,
                    message = R.string.strava_error_missing_credentials,
                    messageDetail = null,
                )
                StravaResult.NotConnected -> _state.value.copy(
                    isBusy = false,
                    hasPendingCode = false,
                    message = R.string.strava_error_generic,
                    messageDetail = null,
                )
            }
        }
    }

    fun disconnect() {
        client.disconnect()
        _state.value = _state.value.copy(
            isConnected = false,
            message = R.string.strava_disconnected,
            messageDetail = null,
        )
    }

    private fun extractCode(input: String): String {
        val trimmed = input.trim()
        val marker = "code="
        val index = trimmed.indexOf(marker)
        if (index < 0) return trimmed
        return trimmed.substring(index + marker.length)
            .substringBefore("&")
            .substringBefore("#")
    }
}
