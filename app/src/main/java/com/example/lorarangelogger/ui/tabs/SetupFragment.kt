package com.example.lorarangelogger.ui.tabs

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lorarangelogger.R
import com.example.lorarangelogger.databinding.FragmentSetupBinding
import com.example.lorarangelogger.ui.main.MainViewModel
import com.example.lorarangelogger.utils.PacketParser
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.util.*


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
        val isConnected = viewModel.isOpen.value == false
        binding.buttonDisconnect.isEnabled = isConnected
        binding.buttonConn.isEnabled = !isConnected && viewModel.selectedDevice.first != ""
        binding.buttonDevice.isEnabled = !isConnected

        viewModel.isOpen.observe(viewLifecycleOwner) {
            binding.buttonDisconnect.isEnabled = it
            binding.buttonConn.isEnabled = !it && viewModel.selectedDevice.first != ""
            binding.buttonDevice.isEnabled = !it
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
                Toast.makeText(requireContext(), "Something went wrong...", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Connection closed!", Toast.LENGTH_SHORT).show()
            }
        }

        checkFile()
        binding.buttonCheck.setOnClickListener { checkFile() }
        binding.buttonShare.setOnClickListener { shareFile() }
        binding.buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirm Deletion")
            builder.setMessage("Do you really want to delete all measurements?")
            builder.setPositiveButton("Yes") { _, _ ->
                Log.d(TAG, "Yes!")
                deleteFile()
            }
            builder.setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "No!") }
            builder.show()
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.polling_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerPoll.adapter = adapter
            val arr = resources.getStringArray(R.array.polling_array)
            val idx = arr.indexOf(viewModel.pollingInterval.toString() + " ms")
            binding.spinnerPoll.setSelection(idx)
        }
        binding.spinnerPoll.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                viewModel.pollingInterval =
                    binding.spinnerPoll.selectedItem.toString().substringBefore(" ").toLong()
                Log.d(TAG, "Changed Polling rate to: ${binding.spinnerPoll.selectedItem}")
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

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
                // automatically try to connect:
                checkBT()
                if (isBtOn) {
                    connect()
                }
            }
        builder.create().show()
    }

    private fun updateSelectedDevice() {
        val devName = viewModel.selectedDevice.first
        binding.textDevice.text = if (devName == "") {
            binding.buttonConn.isEnabled = false
            "Select device:"
        } else {
            binding.buttonConn.isEnabled = true
            devName
        }

    }

    private fun connect() {
        Log.d(TAG, "Ready to BT!")
        if (viewModel.findBT()) {
            viewModel.openBT()
            if (viewModel.isOpen.value == true) {
                Toast.makeText(requireContext(), "Connected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkFile() {
        val measurements = mutableMapOf<String, Array<Int>>()

        try {
            val doesExist = File(requireActivity().filesDir, viewModel.fileName).exists()
            binding.buttonDelete.isEnabled = doesExist
            binding.buttonShare.isEnabled = doesExist
            if (doesExist) {
                val reader = requireActivity().openFileInput(viewModel.fileName).bufferedReader()
                val lines = reader.use {
                    it.readLines()
                }
                lines.forEach {
                    val data = it.split(";")
                    if (data.size == 12) {
                        val label = data[0]
                        val location = data[1]
                        val key = "$label ($location)"
                        if (key.isNotEmpty()) {
                            measurements.putIfAbsent(key, arrayOf(0,0,0,0,0,0))
                            val y = arrayOf(1,data[5].toInt(),data[8].toInt(), data[9].toInt(), data[10].toInt(), data[11].toInt())
                            measurements[key] = measurements[key]!!.zip(y) { xv, yv -> xv + yv }.toTypedArray()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "An exception while reading the file occured.")
        }
        setMeasurementsText(measurements.toSortedMap())
    }

    /**
     * Takes a map with label + location of a measurement series as key
     * and an array cotaining
     * # of sent packets, # of received packets, sum of send rssi, sum of send snr
     * sum of receive rssi, sum of receive snr
     * as value.
     * The avereage values are calculated here
     */
    private fun setMeasurementsText(m: SortedMap<String, Array<Int>>) {
        val ssb = SpannableStringBuilder()
        if (m.isEmpty()) {
            ssb.append("No measurements found.")
        } else {
            for ((k, v) in m) {
                ssb.bold { append("$k:\n") }
                ssb.append("#: (${v[0]} | ${v[1]})")
                if (v[1] > 0) {
                    ssb.append("    RSSI: (${v[2]/v[1]} | ${v[4]/v[1]})")
                    ssb.append("    SNR: (${v[3]/v[1]} | ${v[5]/v[1]})")
                }
                ssb.append("\n\n")
            }
            ssb.bold { append("*Direction: (--> | <--)") }
        }
        binding.textMeasurements.text = ssb

    }

    private fun shareFile() {
        val file = File(requireActivity().filesDir, viewModel.fileName)
        if (file.exists()) {
            val uri: Uri =
                FileProvider.getUriForFile(
                    requireActivity(), "com.example.lorarangelogger.fileprovider", file
                )
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain";
            intent.putExtra(Intent.EXTRA_STREAM, uri);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Couldn't find any measurements!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun deleteFile() {
        val file = File(requireActivity().filesDir, viewModel.fileName)
        if (file.exists()) {
            if (file.delete()) {
                Log.d(TAG, "File Deleted")
                checkFile()
                Toast.makeText(requireContext(), "Deleted measurements.", Toast.LENGTH_SHORT).show()
                return
            } else {
                Log.d(TAG, "File not deleted!")
            }
        }
        Toast.makeText(requireContext(), "Couldn't delete measurements!", Toast.LENGTH_SHORT).show()
    }
}