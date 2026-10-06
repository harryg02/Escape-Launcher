package com.geecee.escapelauncher.core.domain.apps

import com.geecee.escapelauncher.core.domain.repository.db.ModifiedAppsRepository
import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.domain.rhythms.ClosedSchedule
import com.geecee.escapelauncher.core.domain.rhythms.ClosedState
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

sealed class TryOpenAppResult {
    object Launch : TryOpenAppResult()
    object ShowChallenge : TryOpenAppResult()

    /**
     * The app has the countdown and it is one of the user's closed times
     *
     * @param reopensAt When it opens again, or null if that is more than a week away
     */
    data class Closed(val reopensAt: LocalDateTime?) : TryOpenAppResult()
}

class TryOpenAppUseCase @Inject constructor(
    private val modifiedAppsRepository: ModifiedAppsRepository,
    private val appPauseSettingsRepository: AppPauseSettingsRepository
) {
    suspend operator fun invoke(
        packageName: String,
        bypassChallenge: Boolean = false,
        now: LocalDateTime = LocalDateTime.now()
    ): TryOpenAppResult {
        // If we are bypassing (like when the countdown finishes) always launch
        if (bypassChallenge) return TryOpenAppResult.Launch

        if (!modifiedAppsRepository.isChallenge(packageName)) return TryOpenAppResult.Launch

        val closedPeriods = appPauseSettingsRepository.closedPeriods.first()
        val closedState = ClosedSchedule.closedState(closedPeriods, now)
        return if (closedState is ClosedState.Closed) {
            TryOpenAppResult.Closed(closedState.reopensAt)
        } else {
            TryOpenAppResult.ShowChallenge
        }
    }
}
