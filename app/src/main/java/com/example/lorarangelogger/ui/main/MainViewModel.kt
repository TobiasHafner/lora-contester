package com.example.lorarangelogger.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lorarangelogger.data.LoraData
import com.example.lorarangelogger.utils.KissTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


private const val TAG = "LoRaViewModel"

class MainViewModel(private val context: Application) : AndroidViewModel(context) {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var btManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var mmDevice: BluetoothDevice
    private lateinit var mmSocket: BluetoothSocket
    private lateinit var mmOutputStream: OutputStream
    private lateinit var mmInputStream: InputStream

    private var stopWorker = true

    private val receivedPackets = mutableListOf<LoraData>()
    private val _receivedPacketsData = MutableLiveData<List<LoraData>>().apply {
        value = mutableListOf()
    }
    val receivedPacketsData: LiveData<List<LoraData>> = _receivedPacketsData

    private var _isOpen = MutableLiveData(false)
    val isOpen: LiveData<Boolean> = _isOpen

    var selectedDevice = Pair("", "") // name, MAC


    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<Pair<String, String>> {
        Log.d(TAG, "Finding paired devices...")
        mBluetoothAdapter = btManager.adapter
        val devices = mutableListOf<Pair<String, String>>()

        val pairedDevices = mBluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            devices.add(Pair(device.name, device.address))
            Log.d(TAG, "Device: ${device.name}, address: ${device.address}")
        }
        return devices
    }

    @SuppressLint("MissingPermission")
    fun findBT(): Boolean {
        Log.d(TAG, "Finding BT...")
        mBluetoothAdapter = btManager.adapter

        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            Log.d(TAG, "Devices:")
            for (device in pairedDevices) {

                if (device.address == selectedDevice.second) {
                    Log.d(TAG, "Your device: ${device.name}")
                    mmDevice = device;
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun openBT() {
        val uuid: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //Standard SerialPortService ID

        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid)
        try {
            mmSocket.connect()
            mmOutputStream = mmSocket.outputStream
            mmInputStream = mmSocket.inputStream
            Log.d(TAG, "Bluetooth opened")
            _isOpen.value = true
            beginListenForData()
        } catch (e: IOException) {
            Log.d(TAG, "exception: ${e.toString()}")
            Log.d(TAG, "Could not open bluetooth")
            _isOpen.value = false
        }


    }

    private fun beginListenForData() {
        stopWorker = false
        viewModelScope.launch(Dispatchers.IO) {
            while (!stopWorker) {
                readData()
                delay(1000)
            }
        }
    }

    private fun readData() {
        Log.v(TAG, "Reading data...")
        try {
            while (mmInputStream.available() > 0) {
                val frame = KissTranslator.readFrame(mmInputStream)
                if (frame.isNotEmpty()) {
                    val msg = frame.decodeToString()
                    Log.d(TAG, "Message: $msg")
                    receivedPackets.add(LoraData(System.currentTimeMillis(), frame, 0, 0))
                    _receivedPacketsData.postValue(receivedPackets)
                } else {
                    Log.d(TAG, "Bytes are empty!")
                }
            }
        } catch (ex: IOException) {
            Log.d(TAG, "Some exception occured!")
        }
    }

    private fun parseData(frame: ByteArray) {
    }

    /**
     * Tries to send the specified message over BT to the connected device.
     * Returns true if it succeeded. If not, close the connection and return false.
     */
    fun sendData(msg: String) : Boolean {
        return try {
            mmOutputStream.write(KissTranslator.makeFrame(msg.toByteArray()))
            Log.d(TAG, "Message sent")
            true
        } catch (e: IOException) {
            Log.d(TAG, "Message couldn't be sent, some exception occured")
            closeBT()
            false
        }
    }

    @Throws(IOException::class)
    fun closeBT() {
        stopWorker = true
        mmOutputStream.close()
        mmInputStream.close()
        mmSocket.close()
        _isOpen.value = false
    }
}