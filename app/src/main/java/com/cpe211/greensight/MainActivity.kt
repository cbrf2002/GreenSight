package com.cpe211.greensight

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.cpe211.greensight.databinding.ActivityMainBinding
import androidx.navigation.ui.setupWithNavController


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.bgWhite)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.aWhite)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_bar_main)
        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.dashboardFragment) {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in,  // Entering animation
                        R.anim.fade_out, // Exiting animation
                        R.anim.fade_in,  // Pop-enter animation
                        R.anim.fade_out  // Pop-exit animation
                    )
                    .commit()
            }
        }
    }
}
