package com.example.githubissues

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.githubissues.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
//
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
//            as NavHostFragment
//        val navController = navHostFragment.navController
//        val toolbar = binding.toolbar

        //toolbar?.setupWithNavController(navController)

//        /*
//        The MaterialToolbar can be set as the support action bar and thus
//        receive various Activity callbacks
//         */
//        setSupportActionBar(binding.toolbar)
        binding.toolbar?.title = "Issues list"

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        setContentView(binding.root)
    }

    fun changeToolbarTitle(title: String) {
        binding.toolbar?.title = title
    }
}