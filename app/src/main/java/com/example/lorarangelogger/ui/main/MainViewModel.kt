package com.example.lorarangelogger.ui.main

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
import com.example.lorarangelogger.data.*
import com.example.lorarangelogger.utils.KissTranslator
import com.example.lorarangelogger.utils.PacketParser
import com.example.lorarangelogger.utils.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


private const val TAG = "LoRaViewModel"
private const val MAX_RTT = 5000L // time to wait in ms for the last packet to arrive
private const val connectionCheckInterval = 30_000L //interval for checking if bt is still connected

class MainViewModel(private val context: Application) : AndroidViewModel(context) {
    val fileName = "measurements.csv"
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var btManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var mmDevice: BluetoothDevice
    private lateinit var mmSocket: BluetoothSocket
    private lateinit var mmOutputStream: OutputStream
    private lateinit var mmInputStream: InputStream

    var pollingInterval = 1000L

    private var stopWorker = true

    private val messageLog = mutableListOf<String>()
    private val _messageLogData = MutableLiveData<List<String>>(listOf())
    val messageLogData: LiveData<List<String>> = _messageLogData

    private var _isOpen = MutableLiveData(false) // is the BT connection open?
    val isOpen: LiveData<Boolean> = _isOpen

    var selectedDevice = Pair("", "") // name, MAC

    private var _measurementSeries = MeasurementSeries("Measurement 1", "-", "-", 10, 2)
    val measurementSeries: MeasurementSeries
        get() = _measurementSeries
    private val _isMeasuring = MutableLiveData(false)
    val isMeasuring: LiveData<Boolean> = _isMeasuring
    private val measureLog = mutableListOf<String>()
    private val _measureLogData = MutableLiveData<List<String>>(listOf())
    val measureLogData: LiveData<List<String>> = _measureLogData

    private val echoTimes = mutableListOf<Long>()


    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<Pair<String, String>> {
        Log.d(TAG, "Finding paired devices...")
        mBluetoothAdapter = btManager.adapter
        val devices = mutableListOf<Pair<String, String>>()

        val pairedDevices = mBluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            devices.add(Pair(device.name, device.address))
        }
        return devices
    }

    @SuppressLint("MissingPermission")
    fun findBT(): Boolean {
        Log.d(TAG, "Finding BT...")
        mBluetoothAdapter = btManager.adapter

        val pairedDevices = mBluetoothAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                if (device.address == selectedDevice.second) {
                    Log.d(TAG, "Your device: ${device.name}")
                    mmDevice = device
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
            Log.d(TAG, "Could not open bluetooth socket")
            _isOpen.value = false
        }
    }

    private fun beginListenForData() {
        stopWorker = false
        viewModelScope.launch(Dispatchers.IO) {
            var nextCheck = System.currentTimeMillis() + connectionCheckInterval
            while (!stopWorker) {
                readData()
                delay(pollingInterval)
                if (System.currentTimeMillis() > nextCheck) {
                    nextCheck = System.currentTimeMillis() + connectionCheckInterval
                    Log.d(TAG, "Checking connection")
                    try {
                        mmOutputStream.write(ByteArray(0))
                    } catch (ex: IOException) {
                        Log.d(TAG, "Device no longer connected!")
                        closeBT()
                    }
                }
            }
        }
    }

    private fun readData() {
        Log.v(TAG, "Reading data...")
        try {
            while (mmInputStream.available() > 0) {
                val frame = KissTranslator.readFrame(mmInputStream)
                if (frame.isNotEmpty()) {
                    Log.d(TAG, "Incoming: ${frame.toHex()}")
                    handleData(frame)
                } else {
                    Log.d(TAG, "Bytes are empty!")
                }
            }
        } catch (ex: IOException) {
            Log.d(TAG, "Some exception occured!")
        }
    }

    /**
     * Handle incoming messages that aren't empty
     */
    private fun handleData(frame: ByteArray) {
        when (val data = PacketParser.parsePacket(frame, System.currentTimeMillis())) {
            is LoraMsgData -> handleMsgData(data)
            is LoraStatReqData -> handleStatReqData(data)
            is LoraStatSendData -> handleStatSendData(data)
            else -> Unit // do nothing, packet was invalid
        }
    }

    private fun handleMsgData(data: LoraMsgData) {
        writeToMsgLog(data.summary(), data.type, time = data.rcvTime, incomingDir = true)
    }

    private fun handleStatReqData(data: LoraStatReqData) {
        writeToMeasureLog(data.summary(), data.type, time = data.rcvTime, incomingDir = true)
    }

    private fun handleStatSendData(data: LoraStatSendData) {
        // if STAT_SEND, look if measurement is true. if yes: add to measurement, else: echo, just log it
        if (_isMeasuring.value == true && _measurementSeries.handleAnswer(data)) {
            writeToMeasureLog(
                data.summary(),
                "${_measurementSeries.label} | ${data.type}",
                time = data.rcvTime,
                incomingDir = true
            )
        } else { // either not measuring or measurement doesn't belong to this series
            if (echoTimes.contains(data.sendTime)) {// answer to a requested echo
                echoTimes.remove(data.sendTime)
                Log.d(TAG, data.summary())
                writeToMeasureLog(
                    data.summary(), data.type, time = data.rcvTime, incomingDir = true
                )
            } else {
                // unknown answer
                Log.d(TAG, "Unknown echo: ${data.summary()}")
                Log.d(TAG, "Send time: ${data.sendTime}")
                Log.d(TAG, "Actual send time: ${echoTimes.first()}")
                writeToMeasureLog(
                    data.summary(),
                    "UNKNOWN | ${data.type}",
                    time = data.rcvTime,
                    incomingDir = true
                )
            }
        }
    }

    /**
     * Tries to send the specified message over BT to the connected device.
     * Returns true if it succeeded. If not, close the connection and return false.
     */
    fun sendData(msg: String): Boolean {
        return try {
            mmOutputStream.write(KissTranslator.makeFrame(PacketParser.create_MSG(msg)))
            Log.d(TAG, "Message sent")
            writeToMsgLog(msg, "MSG_SENT")
            true
        } catch (e: IOException) {
            Log.d(TAG, "Message couldn't be sent, some exception occured")
            closeBT()
            false
        }
    }

    fun sendData(bytes: ByteArray): Boolean {
        return try {
            mmOutputStream.write(KissTranslator.makeFrame(bytes))
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
        _isOpen.postValue(false)
    }

    fun startSeries(series: MeasurementSeries) {
        if (_isMeasuring.value == false) {
            _measurementSeries = series
            _isMeasuring.value = true
            writeToMeasureLog("Starting ${series.label}", "${series.label} | START")
            viewModelScope.launch(Dispatchers.IO) {
                var counter = 0
                while (_isMeasuring.value == true && counter < series.repetitions && _isOpen.value == true) {
                    counter++
                    val time = series.makeMeasurement()
                    if (sendData(PacketParser.create_STAT_REQUEST(time))) {
                        Log.d(TAG, "Made measurement: $time")
                        writeToMeasureLog(
                            "Sent Packet #$counter",
                            "${series.label} | ECHO_SENT",
                            time = time
                        )
                        if (counter == series.repetitions) break
                        delay(series.delay * 1000L)
                    }
                }
                if (_isMeasuring.value == true) {
                    delay(MAX_RTT / 5) // give last packet a bit of time to arrive
                    if (!series.allAnswered() && _isOpen.value == true) {
                        Log.d(TAG, "Wait for late packets..., ${series.allAnswered()}")
                        delay(MAX_RTT * 4 / 5) // wait for late packets
                    }
                    stopSeries()
                }
            }
        } else {
            Log.d(TAG, "Already measuring!")
        }
    }

    fun stopSeries() {
        _isMeasuring.postValue(false)
        Log.d(TAG, "Series was stopped. Result:")
        Log.d(TAG, _measurementSeries.toString())
        writeToMeasureLog(
            "Result: ${_measurementSeries.numAnswered} of ${_measurementSeries.measurements.size} returned.",
            "${_measurementSeries.label} | END"
        )
        // Log measurement to file
        saveSeries()
    }

    fun sendEcho() {
        val time = System.currentTimeMillis()
        val packet = PacketParser.create_STAT_REQUEST(time)
        echoTimes.add(time)
        if (sendData(packet)) writeToMeasureLog("", "ECHO_SENT", time = time)
    }

    /**
     * For debugging
     */
    private fun ByteArray.toHex(): String =
        joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }

    private fun writeToMsgLog(
        text: String,
        type: String,
        time: Long = System.currentTimeMillis(),
        incomingDir: Boolean = false
    ) {
        val t = TimeHelper.getTime(time)
        var msg = if (incomingDir) "<-" else "->"
        msg += "  $t    [$type]"
        if (text.isNotEmpty()) msg += ":\n       $text"
        messageLog.add(0, msg)
        _messageLogData.postValue(messageLog.toList()) // create copy so we don't get a ConcurrentModificationException
    }

    fun clearMessageLog() {
        messageLog.clear()
        _messageLogData.value = messageLog
    }

    private fun writeToMeasureLog(
        text: String,
        type: String,
        time: Long = System.currentTimeMillis(),
        incomingDir: Boolean = false
    ) {
        val t = TimeHelper.getTime(time)
        var msg = if (incomingDir) "<--" else "-->"
        msg += "  $t    [$type]"
        if (text.isNotEmpty()) msg += ":\n       $text"
        measureLog.add(0, msg)
        _measureLogData.postValue(measureLog.toList()) // create copy so we don't get a ConcurrentModificationException
    }

    fun clearMeasurementLog() {
        measureLog.clear()
        _measureLogData.value = measureLog
    }

    fun saveSeries() {
        context.openFileOutput(fileName, Context.MODE_PRIVATE or Context.MODE_APPEND).use {
            val line = _measurementSeries.getCsv() + "\n"
            it.write(line.toByteArray())
        }
    }
}