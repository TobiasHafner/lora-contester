package com.example.lorarangelogger.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.AndroidViewModel
import com.example.lorarangelogger.tools.KissTranslator
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


private const val TAG = "LoRaViewModel"

class MainViewModel(private val context: Application) : AndroidViewModel(context) {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var btManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var mmDevice: BluetoothDevice
    private lateinit var mmSocket: BluetoothSocket
    private lateinit var mmOutputStream: OutputStream
    private lateinit var mmInputStream: InputStream

    init {
        //findBT()
    }

    fun doSomething(): String {
        return "hi!"
    }

    @SuppressLint("MissingPermission")
    fun findBT() : Boolean{
        Log.d(TAG, "Finding BT...")
        mBluetoothAdapter = btManager.adapter

        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            Log.d(TAG, "Devices:")
            for (device in pairedDevices)
            {

                Log.d(TAG, "Device: ${device.name}, address: ${device.address}")
                if (device.address == "78:21:84:A0:50:EE") {
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
        } catch (e: IOException) {
            Log.d(TAG, "exception: ${e.toString()}")
            Log.d(TAG, "Could not open bluetooth")
        }


        //beginListenForData()


    }

    fun readData() {
        try {
            if (mmInputStream.available() > 0) {
                val frame = KissTranslator.readFrame(mmInputStream)
                if (frame.isNotEmpty()) {
                    Log.d(TAG, "Bytes: ${frame.decodeToString()}")
                } else {
                    Log.d(TAG, "Bytes are empty!")
                }
            } else {
                Log.d(TAG, "Nothing received")
            }
        } catch (ex: IOException) {
            Log.d(TAG, "Some exception occured!")
        }
    }

    fun sendData(msg: String) {
        mmOutputStream.write(KissTranslator.makeFrame(msg.toByteArray()))
        Log.d(TAG, "Message sent")
    }
}