package com.example.githubissues.data

import android.content.Context
import androidx.work.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class UploadIssuesWorker(
    appContext: Context,
    workerParams: WorkerParameters
):
    CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repo: GithubRepository by inject()

    override suspend fun doWork() =
        // Do the work here. In this case - upload the Issues.
        try {
            //repo.getIssues(IssueState.ALL.state)
                repo.getIssuesByWorkManager()
            Result.success()
        } catch (throwable: Throwable) {
            Result.retry()
        }

    companion object {
        fun enqueueWork(context: Context) {
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "uploadIssuesWorkRequest",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<UploadIssuesWorker>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
                        //.setInitialDelay(5, TimeUnit.MINUTES)
                        .setConstraints(
                            Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            //.setRequiresStorageNotLow(true)
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
