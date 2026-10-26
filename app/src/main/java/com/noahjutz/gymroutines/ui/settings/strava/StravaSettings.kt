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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.ui.components.TopBar
import org.koin.androidx.compose.getViewModel

@Composable
fun StravaSettings(
    popBackStack: () -> Unit,
    viewModel: StravaSettingsViewModel = getViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pastedCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_strava_settings),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(
                    if (state.isConnected) R.string.strava_status_connected
                    else R.string.strava_status_disconnected
                ),
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                text = stringResource(R.string.strava_status_help),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.clientId,
                onValueChange = viewModel::setClientId,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.strava_client_id)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.clientSecret,
                onValueChange = viewModel::setClientSecret,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.strava_client_secret)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = viewModel::connect,
                    enabled = !state.isBusy,
                ) {
                    Text(
                        stringResource(
                            if (state.isConnected) R.string.strava_reconnect
                            else R.string.strava_connect
                        )
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = viewModel::saveCredentials,
                    enabled = !state.isBusy,
                ) {
                    Text(stringResource(R.string.btn_save))
                }
                if (state.isConnected) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = viewModel::disconnect,
                        enabled = !state.isBusy,
                    ) {
                        Text(stringResource(R.string.strava_disconnect))
                    }
                }
            }

            if (state.isBusy) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.strava_connecting))
                }
            }

            state.message?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(message) +
                        (state.messageDetail?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.body2,
                    color = if (state.isError) {
                        MaterialTheme.colors.error
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.strava_manual_title),
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                text = stringResource(R.string.strava_manual_help),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pastedCode,
                onValueChange = { pastedCode = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.strava_manual_code)) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.exchangePastedCode(pastedCode)
                    pastedCode = ""
                },
                enabled = !state.isBusy && pastedCode.isNotBlank(),
            ) {
                Text(stringResource(R.string.strava_manual_exchange))
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.strava_setup_help),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
