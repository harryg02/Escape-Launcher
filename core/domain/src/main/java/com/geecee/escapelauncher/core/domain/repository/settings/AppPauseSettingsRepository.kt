package com.geecee.escapelauncher.core.domain.repository.settings

import kotlinx.coroutines.flow.Flow

/**
 * Settings for the pause shown before opening an app that has the open countdown.
 */
interface AppPauseSettingsRepository {
    /** Whether the pause asks what the app is being opened for */
    val askIntention: Flow<Boolean>
    suspend fun setAskIntention(enabled: Boolean)
}
