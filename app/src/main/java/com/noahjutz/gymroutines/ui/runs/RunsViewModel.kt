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

package com.noahjutz.gymroutines.ui.runs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.RunRepository
import com.noahjutz.gymroutines.data.domain.Run
import com.noahjutz.gymroutines.data.strava.StravaSyncer
import com.noahjutz.gymroutines.data.strava.SyncOutcome
import com.noahjutz.gymroutines.data.strava.SyncSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncMessage(
    @StringRes val resId: Int,
    val count: Int? = null,
    val detail: String? = null,
)

class RunsViewModel(
    private val runRepository: RunRepository,
    private val syncer: StravaSyncer,
) : ViewModel() {

    val runs: Flow<List<Run>> = runRepository.runs
    val lastSyncEpochSec: Flow<Long> = syncer.lastSyncEpochSec

    private val _isConnected = MutableStateFlow(syncer.isConnected)
    val isConnected = _isConnected.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _message = MutableStateFlow<SyncMessage?>(null)
    val message = _message.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads the connection state and syncs when the last sync is stale (30 minutes). */
    fun refresh() {
        _isConnected.value = syncer.isConnected
        if (!syncer.isConnected) return
        viewModelScope.launch { sync(staleOnly = true) }
    }

    /** Manual refresh triggered from the UI. */
    fun sync() {
        viewModelScope.launch { sync(staleOnly = false) }
    }

    fun dismissMessage() {
        _message.value = null
    }

    private suspend fun sync(staleOnly: Boolean) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _message.value = null
        val summary = try {
            if (staleOnly) syncer.syncIfStale() else syncer.sync()
        } finally {
            _isSyncing.value = false
            _isConnected.value = syncer.isConnected
        }
        if (summary != null) _message.value = summary.toMessage()
    }

    private fun SyncSummary.toMessage(): SyncMessage = when (outcome) {
        SyncOutcome.Success -> SyncMessage(R.string.runs_sync_done, count = importedCount)
        SyncOutcome.NotConnected -> SyncMessage(R.string.runs_sync_error_auth)
        SyncOutcome.NotConfigured -> SyncMessage(R.string.strava_error_missing_credentials)
        SyncOutcome.AuthError -> SyncMessage(R.string.runs_sync_error_auth)
        SyncOutcome.RateLimited -> SyncMessage(R.string.runs_sync_error_rate_limit)
        SyncOutcome.NetworkError -> SyncMessage(R.string.runs_sync_error_network)
    }
}
