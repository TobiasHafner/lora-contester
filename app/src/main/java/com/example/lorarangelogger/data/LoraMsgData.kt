package com.example.lorarangelogger.data

class LoraMsgData(
    override val rcvTime: Long,
    override val rssi: Int,
    override val snr: Int,
    val msg: String
) :
    LoraData {
    override val type = "MSG"
    override fun summary(): String {
        return "${headerSummary()}:    $msg"
    }

}