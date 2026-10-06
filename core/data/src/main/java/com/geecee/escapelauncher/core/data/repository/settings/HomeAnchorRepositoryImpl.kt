package com.geecee.escapelauncher.core.data.repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.geecee.escapelauncher.core.data.datastore.PreferencesKeys
import com.geecee.escapelauncher.core.domain.repository.settings.HomeAnchorRepository
import com.geecee.escapelauncher.core.model.HomeAnchor
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HomeAnchorRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : HomeAnchorRepository {
    override val homeAnchor: Flow<HomeAnchor> = dataStore.data.map {
        HomeAnchor(
            text = it[PreferencesKeys.HOME_ANCHOR_TEXT] ?: "",
            phoneNumber = it[PreferencesKeys.HOME_ANCHOR_PHONE] ?: ""
        )
    }

    override suspend fun setHomeAnchor(anchor: HomeAnchor) {
        dataStore.edit {
            it[PreferencesKeys.HOME_ANCHOR_TEXT] = anchor.text.trim()
            it[PreferencesKeys.HOME_ANCHOR_PHONE] = anchor.phoneNumber.trim()
        }
    }

    override suspend fun clearHomeAnchor() {
        dataStore.edit {
            it.remove(PreferencesKeys.HOME_ANCHOR_TEXT)
            it.remove(PreferencesKeys.HOME_ANCHOR_PHONE)
        }
    }
}
