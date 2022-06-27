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
import com.example.lorarangelogger.utils.PacketParser


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
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConfigSf.adapter = adapter
            binding.spinnerConfigSf.setSelection(0)
        }
        binding.buttonConfigSf.setOnClickListener {
            val sfValue = binding.spinnerConfigSf.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            builder.setMessage("Do you really want to set the Spreading Factor to $sfValue?")
            builder.setPositiveButton("Yes") { _, _ ->
                viewModel.sendData(PacketParser.create_SF_SET(sfValue.toByte()))
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.show()
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tx_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConfigTx.adapter = adapter
            binding.spinnerConfigTx.setSelection(12)
        }
        binding.buttonConfigTx.setOnClickListener {
            val txValue = binding.spinnerConfigTx.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            if (txValue.substringBefore(" ").toInt() > 14) {
                builder.setMessage(
                    "Do you really want to set the Transmission Power to $txValue? " +
                            "(>14 dBm is illegal in Europe!)"
                )
            } else builder.setMessage("Do you really want to set the Transmission Power to $txValue?")

            builder.setPositiveButton("Yes") { _, _ ->
                viewModel.sendData(
                    PacketParser.create_TX_SET(
                        txValue.substringBefore(" ").toByte()
                    )
                )
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.show()
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.bw_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConfigBw.adapter = adapter
            binding.spinnerConfigBw.setSelection(7)
        }
        binding.buttonConfigBw.setOnClickListener {
            val bwValue = binding.spinnerConfigBw.selectedItem as String
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm config change")
            builder.setMessage("Do you really want to set the Bandwidth to $bwValue?")
            builder.setPositiveButton("Yes") { _, _ ->
                viewModel.sendData(
                    PacketParser.create_BW_SET(
                        bwValue.replace("'", "").substringBefore(" ").toInt()
                    )
                )
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.show()
        }
    }

    private fun setButtonEnable(isEnabled: Boolean) {
        binding.buttonConfigSf.isEnabled = isEnabled
        binding.buttonConfigTx.isEnabled = isEnabled
        binding.buttonConfigBw.isEnabled = isEnabled
    }
}