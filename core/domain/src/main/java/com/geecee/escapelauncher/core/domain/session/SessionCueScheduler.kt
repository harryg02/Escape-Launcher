package com.geecee.escapelauncher.core.domain.session

/**
 * Schedules the gentle reminder shown when the time the user planned to spend in an app is up.
 * Only one reminder exists at a time.
 */
interface SessionCueScheduler {
    /**
     * Shows the reminder after [minutes], replacing any earlier one
     *
     * @param appName Name of the app that was opened, shown in the reminder
     * @param intention What the user said they opened the app for, if they were asked
     */
    fun schedule(appName: String, minutes: Int, intention: String?)

    /**
     * Cancels the pending reminder, if there is one. Called when the user is back home.
     */
    fun cancel()
}
