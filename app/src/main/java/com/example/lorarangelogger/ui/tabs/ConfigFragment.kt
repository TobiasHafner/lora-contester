package com.example.lorarangelogger.ui.tabs

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.R
import com.example.lorarangelogger.databinding.FragmentConfigBinding
import com.example.lorarangelogger.ui.main.MainViewModel


private const val TAG = "LoRaConfigFragment"
class ConfigFragment : Fragment() {
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setButtonEnable(viewModel.isOpen.value == true)
        viewModel.isOpen.observe(viewLifecycleOwner) {
            setButtonEnable(it)
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sf_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerConfigSf.adapter = adapter
            binding.spinnerConfigSf.setSelection(0)
        }
        binding.buttonConfigSf.setOnClickListener {
            val sfValue = binding.spinnerConfigSf.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            builder.setMessage("Do you really want to set the Spreading Factor to $sfValue?")
            builder.setPositiveButton("Yes") { _, _ -> Log.d(TAG, "Yes!")}
            builder.setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "No!")}
            builder.show()
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tx_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerConfigTx.adapter = adapter
            binding.spinnerConfigTx.setSelection(0)
        }
        binding.buttonConfigTx.setOnClickListener {
            val txValue = binding.spinnerConfigTx.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            builder.setMessage("Do you really want to set the Transmission Strength to $txValue dB?")
            builder.setPositiveButton("Yes") { _, _ -> Log.d(TAG, "Yes!")}
            builder.setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "No!")}
            builder.show()
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.bw_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerConfigBw.adapter = adapter
            binding.spinnerConfigBw.setSelection(0)
        }
        binding.buttonConfigBw.setOnClickListener {
            val txValue = binding.spinnerConfigBw.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            builder.setMessage("Do you really want to set the Bandwidth to $txValue Hz?")
            builder.setPositiveButton("Yes") { _, _ -> Log.d(TAG, "Yes!")}
            builder.setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "No!")}
            builder.show()
        }
    }

    private fun setButtonEnable(isEnabled: Boolean) {
        binding.buttonConfigSf.isEnabled = isEnabled
        binding.buttonConfigTx.isEnabled = isEnabled
        binding.buttonConfigBw.isEnabled = isEnabled
    }
}