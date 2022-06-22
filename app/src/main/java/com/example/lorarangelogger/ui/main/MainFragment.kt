package com.example.lorarangelogger.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.lorarangelogger.R
import com.example.lorarangelogger.databinding.FragmentMainBinding
import com.example.lorarangelogger.tools.KissTranslator

private const val TAG = "LoRaFragment"
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var isBtOn = false

    private val registerForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isBtOn = result.resultCode == Activity.RESULT_OK
        connect()
        /*if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            Log.d(TAG, "Intent??")
            // Handle the Intent
        }*/
    }
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Granted")
                checkBT()
                connect()
            } else {
                Log.d(TAG, "Denied")
            }
        }

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        // TODO: Use the ViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSend.setOnClickListener {
            Log.d(TAG, "Trying to send data")
            val msg = "test"
            viewModel.sendData(msg)
            //Log.d(TAG, KissTranslator.makeFrame(msg.toByteArray()).decodeToString())
        }

        binding.buttonReceive.setOnClickListener {
            Log.d(TAG, "Trying to receive data")
            viewModel.readData()
        }

        binding.buttonConnect.setOnClickListener {
            Log.d(TAG, "Trying to connect...")
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG,"Bluetooth_connect permission missing")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }

            } else {
                checkBT()
                connect()
            }
        }

    }

    private fun connect() {
        if (isBtOn) {
            Log.d(TAG, "Ready to BT!")
            if(viewModel.findBT()) {
                viewModel.openBT()

            }
        }
    }
    private fun checkBT() {
        val btManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!btManager.adapter.isEnabled) {
            isBtOn = false
            Log.d(TAG, "enable bluetooth!")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            registerForResult.launch(enableBtIntent)
        } else {
            isBtOn = true
        }
    }

}