package com.geecee.escapelauncher.core.domain.repository.settings

import com.geecee.escapelauncher.core.model.ClosedPeriod
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

    /** Recurring times during which apps with the countdown stay closed. Empty by default */
    val closedPeriods: Flow<List<ClosedPeriod>>
    suspend fun setClosedPeriods(periods: List<ClosedPeriod>)
}
