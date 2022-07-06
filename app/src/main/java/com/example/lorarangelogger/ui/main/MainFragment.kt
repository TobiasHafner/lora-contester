package com.example.lorarangelogger.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lorarangelogger.databinding.FragmentMainBinding
import com.google.android.material.tabs.TabLayoutMediator


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: MainPagerAdapter

    companion object {
        fun newInstance() = MainFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set ViewPager Adapter
        pagerAdapter = MainPagerAdapter(this)
        val pager = binding.pager
        pager.adapter = pagerAdapter

        // Attach tabs and set title text
        TabLayoutMediator(binding.tabLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> "Setup"
                1 -> "Message"
                2 -> "Measure"
                else -> "Config"
            }
        }.attach()


    }

}