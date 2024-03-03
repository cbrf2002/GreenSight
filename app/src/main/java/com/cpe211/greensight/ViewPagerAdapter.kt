package com.cpe211.greensight

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 4 // Number of fragments
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DashboardFragment()
            1 -> MonitorFragment()
            2 -> SettingsFragment()
            3 -> AboutFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
