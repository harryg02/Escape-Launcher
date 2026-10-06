package com.geecee.escapelauncher.core.data.repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.data.datastore.PreferencesKeys
import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.domain.rhythms.ClosedSchedule
import com.geecee.escapelauncher.core.model.ClosedPeriod
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPauseSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppPauseSettingsRepository {
    override val askIntention: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ASK_INTENTION] ?: DefaultSettings.ASK_INTENTION }
    override suspend fun setAskIntention(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ASK_INTENTION] = enabled }
    }
    override val askSessionLength: Flow<Boolean> = dataStore.data.map { it[PreferencesKeys.ASK_SESSION_LENGTH] ?: DefaultSettings.ASK_SESSION_LENGTH }
    override suspend fun setAskSessionLength(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ASK_SESSION_LENGTH] = enabled }
    }
    override val closedPeriods: Flow<List<ClosedPeriod>> = dataStore.data.map { ClosedSchedule.decode(it[PreferencesKeys.CLOSED_PERIODS] ?: "") }
    override suspend fun setClosedPeriods(periods: List<ClosedPeriod>) {
        dataStore.edit { it[PreferencesKeys.CLOSED_PERIODS] = ClosedSchedule.encode(periods) }
    }
}
