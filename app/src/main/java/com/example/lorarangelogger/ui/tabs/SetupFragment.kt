package com.example.lorarangelogger.ui.tabs

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.databinding.FragmentSetupBinding
import com.example.lorarangelogger.ui.main.MainViewModel

private const val TAG = "LoRaSetupFragment"

class SetupFragment : Fragment() {
    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var isBtOn = false

    private val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBtOn = result.resultCode == Activity.RESULT_OK
        connect()
    }
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Granted")
                chooseDevice()
            } else {
                Log.d(TAG, "Denied")
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateSelectedDevice()
        binding.buttonDisconnect.isEnabled = viewModel.isOpen.value == true

        viewModel.isOpen.observe(viewLifecycleOwner) {
            binding.buttonDisconnect.isEnabled = it
        }

        binding.buttonDevice.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Bluetooth_connect permission missing")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else {
                chooseDevice()
            }
        }

        binding.buttonConn.setOnClickListener {
            checkBT()
            if (isBtOn) {
                connect()
            }
        }

        binding.buttonDisconnect.setOnClickListener {
            Log.d(TAG, "Trying to close connection")
            viewModel.closeBT()
            if (viewModel.isOpen.value == true) {
                Toast.makeText(requireContext(),"Something went wrong...",Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(),"Connection closed!",Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkBT() {
        val btManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!btManager.adapter.isEnabled) {
            isBtOn = false
            Log.d(TAG, "enable bluetooth!")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerForResult.launch(enableBtIntent)
        } else {
            isBtOn = true
        }
    }

    private fun chooseDevice() {
        val devices = viewModel.pairedDevices()
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Choose a device")
            .setItems(
                devices.map { "${it.first} (${it.second})" }.toTypedArray()
            ) { _, which ->
                viewModel.selectedDevice = devices[which]
                updateSelectedDevice()
                Log.d(TAG, "Selected device: ${viewModel.selectedDevice}")
            }
        builder.create().show()
    }

    private fun updateSelectedDevice() {
        val devName = viewModel.selectedDevice.first
        binding.textDevice.text = if (devName == "") {
            binding.buttonConn.isEnabled = false
            "No device selected"
        } else {
            binding.buttonConn.isEnabled = true
            devName
        }

    }

    private fun connect() {
        Log.d(TAG, "Ready to BT!")
        if(viewModel.findBT()) {
            viewModel.openBT()
            if (viewModel.isOpen.value == true) {
                Toast.makeText(requireContext(),"Connected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(),"Something went wrong!", Toast.LENGTH_SHORT).show()
            }
        }

    }
}