package com.example.githubissues.api

import com.example.githubissues.model.Issue
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface GithubApiService {
    @Headers("Accept: application/vnd.github.v3+json")
    @GET("repos/JetBrains/compose-jb/issues")
    suspend fun getIssues(
        @Query("state") state: String,
        @Query("page") page: Int,
        @Query("per_page") itemsPerPage: Int): List<Issue>

    companion object {
        private const val BASE_URL = "https://api.github.com"

        fun create(): GithubApiService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GithubApiService::class.java)
        }
    }
}