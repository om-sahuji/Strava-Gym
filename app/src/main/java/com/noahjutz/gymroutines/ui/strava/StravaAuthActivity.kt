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

package com.noahjutz.gymroutines.ui.strava

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.noahjutz.gymroutines.data.strava.StravaAuthCallback
import com.noahjutz.gymroutines.ui.MainActivity

/**
 * Catches `omfit://localhost/strava?code=...` (the Strava OAuth redirect), hands the code to
 * [StravaAuthCallback] and returns the user to the app. The code is exchanged by the Strava
 * settings screen, which keeps the client secret out of this activity.
 */
class StravaAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        val code = data?.getQueryParameter("code")
        val error = data?.getQueryParameter("error")
        when {
            !code.isNullOrBlank() -> StravaAuthCallback.onCodeReceived(code)
            !error.isNullOrBlank() -> StravaAuthCallback.onError(error)
        }

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }
}
