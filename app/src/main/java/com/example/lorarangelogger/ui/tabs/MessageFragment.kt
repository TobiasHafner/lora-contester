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
private const val MAX_MSG_LENGTH = 120
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

        viewModel.messageLogData.observe(viewLifecycleOwner) {
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
            else if (msg.length > MAX_MSG_LENGTH) Toast.makeText(
                requireContext(),
                "Your message is ${msg.length - MAX_MSG_LENGTH} character(s) too long!",
                Toast.LENGTH_SHORT
            ).show()
            else if (viewModel.sendData(msg)) binding.editTextSnd.setText("")
        }

        binding.buttonClearMsg.setOnClickListener { viewModel.clearMessageLog() }
    }

    private fun updateList(list: List<String>) {
        binding.textMsgLog.text = list.joinToString("\n\n")
    }
}