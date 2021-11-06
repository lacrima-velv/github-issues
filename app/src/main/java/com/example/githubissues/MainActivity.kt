package com.example.githubissues

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.githubissues.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContentView(binding.root)
    }

    // Used during sliding two panes layout
    fun changeToolbarTitle(title: String) {
        binding.toolbar?.title = title
    }
}