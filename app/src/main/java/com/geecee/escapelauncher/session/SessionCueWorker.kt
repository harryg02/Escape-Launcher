package com.geecee.escapelauncher.session

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.geecee.escapelauncher.MainHomeScreenActivity
import com.geecee.escapelauncher.core.common.hasPermission
import com.geecee.escapelauncher.core.ui.R

/**
 * Posts the reminder that the time the user planned for an app is up. It is a single silent
 * notification; tapping it goes home. It is never repeated.
 */
class SessionCueWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val appName = inputData.getString(KEY_APP_NAME) ?: return Result.success()
        val minutes = inputData.getInt(KEY_MINUTES, 0)
        val intention = inputData.getString(KEY_INTENTION)

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !applicationContext.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            return Result.success()
        }

        createChannel(notificationManager)

        val title = applicationContext.resources.getQuantityString(
            R.plurals.session_cue_title, minutes, minutes, appName
        )
        val text = if (intention != null) {
            applicationContext.getString(R.string.session_cue_intention, intention)
        } else {
            applicationContext.getString(R.string.session_cue_go_home)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.outlineicon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(goHomeIntent())
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e("SessionCueWorker", "Notification permission was revoked", e)
        }
        return Result.success()
    }

    /**
     * High importance so the reminder appears over the app being used, but without sound or
     * vibration. Users can change this in the system notification settings.
     */
    private fun createChannel(notificationManager: NotificationManagerCompat) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.session_cue_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun goHomeIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainHomeScreenActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val KEY_APP_NAME = "app_name"
        const val KEY_MINUTES = "minutes"
        const val KEY_INTENTION = "intention"
        private const val CHANNEL_ID = "session_cue"
        private const val NOTIFICATION_ID = 4101
    }
}
