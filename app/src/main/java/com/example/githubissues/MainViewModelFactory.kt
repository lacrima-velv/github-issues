package com.example.githubissues

import android.app.Application
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.example.githubissues.data.GithubRepository
import java.lang.IllegalArgumentException

class MainViewModelFactory(
    private val application: Application,
    owner: SavedStateRegistryOwner
): AbstractSavedStateViewModelFactory(owner, null) {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(
        key:String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}