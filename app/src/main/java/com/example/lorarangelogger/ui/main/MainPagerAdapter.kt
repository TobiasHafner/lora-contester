package com.example.lorarangelogger.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.lorarangelogger.ui.tabs.ConfigFragment
import com.example.lorarangelogger.ui.tabs.MeasureFragment
import com.example.lorarangelogger.ui.tabs.MessageFragment
import com.example.lorarangelogger.ui.tabs.SetupFragment

class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        // Return a new fragment instance
        val fragment = when(position) {
            0 -> SetupFragment()
            1 -> MessageFragment()
            2 -> MeasureFragment()
            else -> ConfigFragment()
        }
        return fragment
    }

}