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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handoff between the deep-link [com.noahjutz.gymroutines.ui.strava.StravaAuthActivity] and the
 * Strava settings screen. The value survives until it is consumed, so the code is not lost when the
 * app was in the background when the redirect arrived.
 */
object StravaAuthCallback {
    private val _code = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val code: StateFlow<String?> = _code
    val error: StateFlow<String?> = _error

    fun onCodeReceived(code: String) {
        _code.value = code
    }

    fun onError(error: String?) {
        _error.value = error ?: "authorization failed"
    }

    fun consumeCode(): String? {
        val current = _code.value
        _code.value = null
        return current
    }

    fun consumeError(): String? {
        val current = _error.value
        _error.value = null
        return current
    }
}
