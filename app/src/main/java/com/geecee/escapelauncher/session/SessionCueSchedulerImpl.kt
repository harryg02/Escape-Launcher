package com.geecee.escapelauncher.session

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.geecee.escapelauncher.core.domain.session.SessionCueScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses a one-off WorkManager job. This needs no extra permission and survives the launcher process
 * being killed while another app is in front. The cue may arrive a little late if the system
 * defers the job, which is acceptable for a reminder.
 */
class SessionCueSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionCueScheduler {
    override fun schedule(appName: String, minutes: Int, intention: String?) {
        val input = Data.Builder()
            .putString(SessionCueWorker.KEY_APP_NAME, appName)
            .putInt(SessionCueWorker.KEY_MINUTES, minutes)
            .putString(SessionCueWorker.KEY_INTENTION, intention)
            .build()

        val request = OneTimeWorkRequestBuilder<SessionCueWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "session_cue"
    }
}

@Suppress("unused") // It is used by hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {
    @Binds
    @Singleton
    abstract fun bindSessionCueScheduler(
        impl: SessionCueSchedulerImpl
    ): SessionCueScheduler
}
