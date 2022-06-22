package com.example.lorarangelogger.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.fragment.app.activityViewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.lorarangelogger.data.LoraData
import com.example.lorarangelogger.databinding.FragmentMainBinding

private const val TAG = "LoRaFragment"
class MainFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSend.isEnabled = viewModel.isOpen
        binding.buttonClose.isEnabled = viewModel.isOpen

        binding.buttonSend.setOnClickListener {
            Log.d(TAG, "Trying to send data")
            var msg = binding.editTextSend.text.toString()
            if (msg == "") msg = "Test!"
            viewModel.sendData(msg)
            //Log.d(TAG, KissTranslator.makeFrame(msg.toByteArray()).decodeToString())
        }

        binding.buttonClose.setOnClickListener {
            Log.d(TAG, "Trying to close connection")
            viewModel.closeBT()
            if (viewModel.isOpen) {
                Toast.makeText(requireContext(),"Something went wrong...",Toast.LENGTH_SHORT).show()
            } else {
                binding.buttonSend.isEnabled = false
                binding.buttonClose.isEnabled = false
                Toast.makeText(requireContext(),"Connection closed!",Toast.LENGTH_SHORT).show()
            }
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

        viewModel.receivedPacketsData.observe(viewLifecycleOwner) {
            updateList(it)
        }

    }

    private fun connect() {
        if (isBtOn) {
            Log.d(TAG, "Ready to BT!")
            if(viewModel.findBT()) {
                viewModel.openBT()
                if (viewModel.isOpen) {
                    Toast.makeText(requireContext(),"Connected!",Toast.LENGTH_SHORT).show()
                    binding.buttonSend.isEnabled = true
                    binding.buttonClose.isEnabled = true
                } else {
                    Toast.makeText(requireContext(),"Something went wrong!",Toast.LENGTH_SHORT).show()
                }
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

    private fun updateList(list: List<LoraData>) {
        var listStr = ""
        list.map { listStr+="$it\n"}
        binding.textReceived.text = listStr
    }

}