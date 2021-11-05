package com.example.githubissues.data

import android.content.Context
import androidx.work.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UploadIssuesWorker(
    appContext: Context,
    workerParams: WorkerParameters
):
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repo: GithubRepository by inject()

    override suspend fun doWork() =
        // Do the work here: upload the Issues.
        try {
            Timber.d("doWork() is called")
            repo.getIssuesByWorkManager()
            Result.success()
        } catch (throwable: Throwable) {
            Result.retry()
        }

    companion object {
        fun enqueueWork(context: Context) {
            Timber.d("enqueueWork() is called")
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "uploadIssuesWorkRequest",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<UploadIssuesWorker>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                        .setInitialDelay(5, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            PeriodicWorkRequest.MAX_BACKOFF_MILLIS,
                            TimeUnit.MILLISECONDS
                        )
                        .build()
                )
        }
    }

}

