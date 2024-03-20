package com.cpe211.greensight

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.cpe211.greensight.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.bgWhite)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.bgWhite)
        window.isStatusBarContrastEnforced = true
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS

        sharedViewModel = ViewModelProvider(this)[SharedViewModel::class.java]

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_bar_main)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Disable swipe gesture to switch pages

        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fragmentId = when (destination.id) {
                R.id.dashboardFragment -> 0
                R.id.monitorFragment -> 1
                R.id.settingsFragment -> 2
                R.id.aboutFragment -> 3
                else -> -1
            }
            viewPager.setCurrentItem(fragmentId, true)
        }
    }
}