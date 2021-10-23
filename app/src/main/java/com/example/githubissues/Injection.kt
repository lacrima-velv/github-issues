package com.example.githubissues

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.db.IssuesDatabase

/**
 * Class that handles object creation.
 * Like this, objects can be passed as parameters in the constructors and then replaced for
 * testing, where needed.
 */
object Injection {

    /**
     * Creates an instance of [GithubRepository] based on the [GithubService] and a
     * [GithubLocalCache]
     */
    private fun provideGithubRepository(context: Context): GithubRepository {
        return GithubRepository(GithubApiService.create(), IssuesDatabase.getInstance(context))
    }

    /**
     * Provides the [ViewModelProvider.Factory] that is then used to get a reference to
     * [ViewModel] objects.
     */
    fun provideViewModelFactory(context: Context): ViewModelProvider.Factory {
        return MainViewModelFactory(provideGithubRepository(context))
    }

}