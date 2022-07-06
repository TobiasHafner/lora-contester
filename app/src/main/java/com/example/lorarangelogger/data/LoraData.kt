package com.example.lorarangelogger.data


/**
 * Represents a single packet received from the bridge.
 * It is either a message packet, a STAT_REQUEST or a STAT_SEND
 */
interface LoraData {
    val rcvTime: Long
    val rssi: Int
    val snr: Int
    val type: String

    fun summary(): String
}