package com.example.lorarangelogger.ui.tabs

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.data.LoraData
import com.example.lorarangelogger.databinding.FragmentMessageBinding
import com.example.lorarangelogger.databinding.FragmentSetupBinding
import com.example.lorarangelogger.ui.main.MainViewModel

private const val TAG = "LoRaMessageFragment"

class MessageFragment : Fragment() {
    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSnd.isEnabled = viewModel.isOpen.value == true
        viewModel.isOpen.observe(viewLifecycleOwner) {
            binding.buttonSnd.isEnabled = it
        }

        viewModel.receivedPacketsData.observe(viewLifecycleOwner) {
            updateList(it)
        }

        binding.buttonSnd.setOnClickListener {
            Log.d(TAG, "Trying to send data")
            val msg = binding.editTextSnd.text.toString()
            if (msg == "") Toast.makeText(
                requireContext(),
                "Please enter some message to send.",
                Toast.LENGTH_SHORT
            ).show()
            else viewModel.sendData(msg)
        }
    }

    private fun updateList(list: List<LoraData>) {
        var listStr = ""
        list.map { listStr+="$it\n"}
        binding.textRcvd.text = listStr
    }
}