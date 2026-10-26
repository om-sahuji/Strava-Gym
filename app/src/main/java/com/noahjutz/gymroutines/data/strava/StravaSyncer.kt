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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.noahjutz.gymroutines.data.AppPrefs
import com.noahjutz.gymroutines.data.RunRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncOutcome {
    Success,
    NotConnected,
    NotConfigured,
    RateLimited,
    AuthError,
    NetworkError,
}

data class SyncSummary(
    val outcome: SyncOutcome,
    val importedCount: Int = 0,
)

/**
 * Imports running activities from Strava into the local `run_table`.
 *
 * The first sync walks the full history (`after` is omitted). Later syncs ask Strava for activities
 * after the last successful sync, minus [INCREMENTAL_LOOKBACK_SEC] to also catch activities that
 * were uploaded late; upserts by `stravaId` make the overlap harmless.
 */
class StravaSyncer(
    private val client: StravaClient,
    private val runRepository: RunRepository,
    private val preferences: DataStore<Preferences>,
) {
    private val mutex = Mutex()

    /** Epoch seconds of the last successful sync (0 = never). */
    val lastSyncEpochSec: Flow<Long> =
        preferences.data.map { it[AppPrefs.LastStravaSync.key] ?: 0L }

    val isConnected: Boolean get() = client.isConnected

    /** Syncs if the last successful sync is older than [staleAfterSec]. */
    suspend fun syncIfStale(staleAfterSec: Long = DEFAULT_STALE_AFTER_SEC): SyncSummary? {
        if (!client.isConnected) return null
        val last = preferences.data.first()[AppPrefs.LastStravaSync.key] ?: 0L
        val ageSec = System.currentTimeMillis() / 1000 - last
        if (last != 0L && ageSec < staleAfterSec) return null
        return sync()
    }

    suspend fun sync(): SyncSummary = mutex.withLock {
        if (!client.isConfigured) return@withLock SyncSummary(SyncOutcome.NotConfigured)
        if (!client.isConnected) return@withLock SyncSummary(SyncOutcome.NotConnected)

        val lastSync = preferences.data.first()[AppPrefs.LastStravaSync.key] ?: 0L
        val after = if (lastSync == 0L) {
            null
        } else {
            (lastSync - INCREMENTAL_LOOKBACK_SEC).coerceAtLeast(0L)
        }

        var page = 1
        var imported = 0

        while (true) {
            val result = client.activities(
                page = page,
                perPage = StravaClient.PER_PAGE,
                afterEpochSec = after,
            )
            val body = when (result) {
                is StravaResult.Success -> result.value
                is StravaResult.HttpError -> return@withLock when (result.code) {
                    401, 403 -> SyncSummary(SyncOutcome.AuthError)
                    429 -> SyncSummary(SyncOutcome.RateLimited)
                    else -> SyncSummary(SyncOutcome.NetworkError)
                }
                is StravaResult.NetworkError -> return@withLock SyncSummary(SyncOutcome.NetworkError)
                StravaResult.NotConfigured -> return@withLock SyncSummary(SyncOutcome.NotConfigured)
                StravaResult.NotConnected -> return@withLock SyncSummary(SyncOutcome.NotConnected)
            }

            val activities = StravaMapper.parseActivitiesOrNull(body)
                ?: return@withLock SyncSummary(SyncOutcome.NetworkError)

            if (activities.isEmpty()) break

            val runs = activities.mapNotNull { StravaMapper.toRun(it) }
            if (runs.isNotEmpty()) runRepository.upsert(runs)
            imported += runs.size

            if (activities.size < StravaClient.PER_PAGE) break
            page++
        }

        preferences.edit {
            it[AppPrefs.LastStravaSync.key] = System.currentTimeMillis() / 1000
        }

        SyncSummary(SyncOutcome.Success, imported)
    }

    companion object {
        const val DEFAULT_STALE_AFTER_SEC = 30 * 60L
        private const val INCREMENTAL_LOOKBACK_SEC = 7 * 24 * 60 * 60L
    }
}
