package com.geecee.escapelauncher.core.domain.repository.settings

import kotlinx.coroutines.flow.Flow

/**
 * Settings for the pause shown before opening an app that has the open countdown.
 */
interface AppPauseSettingsRepository {
    /** Whether the pause asks what the app is being opened for */
    val askIntention: Flow<Boolean>
    suspend fun setAskIntention(enabled: Boolean)

    /** Whether the pause offers to set how long the app will be used for, with a reminder at the end */
    val askSessionLength: Flow<Boolean>
    suspend fun setAskSessionLength(enabled: Boolean)
}
