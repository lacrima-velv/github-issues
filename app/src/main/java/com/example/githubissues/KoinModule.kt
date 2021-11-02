package com.example.githubissues

import com.example.githubissues.api.GithubApiService
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.db.IssuesDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinModule = module {
    single {
        GithubRepository(GithubApiService.create(), IssuesDatabase.getInstance(androidContext()))
    }
}