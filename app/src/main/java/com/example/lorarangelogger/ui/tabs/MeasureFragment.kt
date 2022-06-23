package com.example.lorarangelogger.ui.tabs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.R
import com.example.lorarangelogger.databinding.FragmentMeasureBinding
import com.example.lorarangelogger.databinding.FragmentMessageBinding
import com.example.lorarangelogger.ui.main.MainViewModel


class MeasureFragment : Fragment() {
    private var _binding: FragmentMeasureBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMeasureBinding.inflate(inflater, container, false)
        return binding.root
    }
}