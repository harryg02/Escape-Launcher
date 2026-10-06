package com.geecee.escapelauncher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geecee.escapelauncher.core.domain.apps.LaunchAppUseCase
import com.geecee.escapelauncher.core.domain.apps.TryOpenAppResult
import com.geecee.escapelauncher.core.domain.apps.TryOpenAppUseCase
import com.geecee.escapelauncher.core.domain.launcher.GetIsDefaultLauncherUseCase
import com.geecee.escapelauncher.core.domain.managedprofiles.IsManagedProfileSupportedUseCase
import com.geecee.escapelauncher.core.domain.managedprofiles.ManagedProfileExistsUseCase
import com.geecee.escapelauncher.core.domain.managedprofiles.ManagedProfileType
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.domain.repository.settings.AppPauseSettingsRepository
import com.geecee.escapelauncher.core.domain.repository.settings.OnboardingRepository
import com.geecee.escapelauncher.core.domain.repository.settings.ScreenTimeSettingsRepository
import com.geecee.escapelauncher.core.domain.repository.settings.LauncherBehaviorRepository
import com.geecee.escapelauncher.core.domain.session.SessionCueScheduler
import com.geecee.escapelauncher.core.domain.system.LockScreenUseCase
import com.geecee.escapelauncher.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainPagerScreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val onboardingRepository: OnboardingRepository,
    private val screenTimeSettingsRepository: ScreenTimeSettingsRepository,
    launcherBehaviorRepository: LauncherBehaviorRepository,
    appPauseSettingsRepository: AppPauseSettingsRepository,
    private val tryOpenAppUseCase: TryOpenAppUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val managedProfileExistsUseCase: ManagedProfileExistsUseCase,
    private val isManagedProfileSupportedUseCase: IsManagedProfileSupportedUseCase,
    private val getIsDefaultLauncherUseCase: GetIsDefaultLauncherUseCase,
    private val lockScreenUseCase: LockScreenUseCase,
    private val sessionCueScheduler: SessionCueScheduler
) : AndroidViewModel(context as Application) {
    fun managedProfileExists(type: ManagedProfileType): Boolean = managedProfileExistsUseCase(type)
    fun isManagedProfileSupported(type: ManagedProfileType): Boolean = isManagedProfileSupportedUseCase(type)
    fun setFirstTimeHelp(value: Boolean) {
        viewModelScope.launch {
            onboardingRepository.setFirstTimeHelp(value)
        }
    }

    val hideScreenTimePage = screenTimeSettingsRepository.hideScreenTimePage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )
    val doubleTapToLock = launcherBehaviorRepository.doubleTapToLock
    val hapticFeedBackEnabled = launcherBehaviorRepository.hapticFeedBackEnabled

    val isHiddenPrivateSpace = launcherBehaviorRepository.hidePrivateSpace

    val askIntention = appPauseSettingsRepository.askIntention.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DefaultSettings.ASK_INTENTION
    )

    val askSessionLength = appPauseSettingsRepository.askSessionLength.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DefaultSettings.ASK_SESSION_LENGTH
    )

    /**
     * Opens the app the pause was shown for, and if the user planned how long to use it for,
     * schedules the reminder for when that time is up
     */
    fun openAppAfterPause(intention: String?, minutes: Int?, onAppOpened: (String) -> Unit) {
        val app = currentSelectedApp.value
        openApp(app = app, overrideChallenge = true, onAppOpened = { packageName ->
            onAppOpened(packageName)
            if (minutes != null) {
                sessionCueScheduler.schedule(app.displayName, minutes, intention)
            }
        })
    }

    /**
     * The user is back home, so a planned time reminder is no longer needed
     */
    fun onHomeResumed() {
        sessionCueScheduler.cancel()
    }

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    fun updateLauncherStatus() {
        _isDefaultLauncher.value = getIsDefaultLauncherUseCase()
    }

    fun lockScreen() {
        lockScreenUseCase()
    }

    var currentSelectedApp = mutableStateOf(InstalledApp("", "", ComponentName("", "")))

    var showOpenChallenge = mutableStateOf(false)

    // Shown instead of the pause during one of the user's closed times
    var showClosedNotice = mutableStateOf(false)
    var closedNoticeReopensAt = mutableStateOf<LocalDateTime?>(null)

    val interactionSource = MutableInteractionSource()

    val appsListScrollState = LazyListState()

    val pagerState = PagerState(
        currentPage = if (hideScreenTimePage.value) 0 else 1,
        currentPageOffsetFraction = 0f
    ) {
        if (hideScreenTimePage.value) 2 else 3
    }

    private fun getMainPageIndex(): Int {
        return if (hideScreenTimePage.value) 0 else 1
    }

    suspend fun goToMainPage() {
        pagerState.scrollToPage(getMainPageIndex())
    }

    suspend fun animatedGoToMainPage() {
        val targetPage = getMainPageIndex()

        if (pagerState.currentPage == targetPage && pagerState.currentPageOffsetFraction == 0f) {
            return
        }

        pagerState.animateScrollToPage(
            targetPage,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    init {
        updateLauncherStatus()

        // The pager is constructed before DataStore has emitted, so `hideScreenTimePage` is still
        // its placeholder value at that point. Re-anchor the pager on the main page once the real
        // value is known, and again whenever the setting is toggled (the page indices shift).
        viewModelScope.launch {
            screenTimeSettingsRepository.hideScreenTimePage
                .distinctUntilChanged()
                .collect { hide -> pagerState.scrollToPage(if (hide) 0 else 1) }
        }
    }

    fun dismissOpenChallenge() {
        showOpenChallenge.value = false
        showClosedNotice.value = false
    }

    /**
     * The user chose to open an app during a closed time. They still get the normal pause.
     */
    fun openAnyway() {
        showClosedNotice.value = false
        showOpenChallenge.value = true
    }

    fun updateSelectedApp(app: InstalledApp) {
        currentSelectedApp.value = app
    }

    /**
     * Logic to handle what happens when an app is launched
     */
    fun onAppLaunched(app: InstalledApp) {
        updateSelectedApp(app)
        viewModelScope.launch {
            delay(500.milliseconds)
            goToMainPage()
        }
    }

    /**
     * High-level function to open an app, handling challenge checks asynchronously
     */
    fun openApp(
        app: InstalledApp,
        overrideChallenge: Boolean = false,
        onAppOpened: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            when (val result = tryOpenAppUseCase(app.packageName, overrideChallenge)) {
                TryOpenAppResult.ShowChallenge -> {
                    updateSelectedApp(app)
                    showOpenChallenge.value = true
                }
                is TryOpenAppResult.Closed -> {
                    updateSelectedApp(app)
                    closedNoticeReopensAt.value = result.reopensAt
                    showClosedNotice.value = true
                }
                TryOpenAppResult.Launch -> {
                    if (launchAppUseCase(app, onAppOpened)) {
                        onAppLaunched(app)

                        // At the end of an open challenge countdown, it runs this openApp function again with overrideChallenge set to true so this is used to hide the challenge ui
                        if (overrideChallenge) {
                            showOpenChallenge.value = false
                        }
                    }
                }
            }
        }
    }
}
