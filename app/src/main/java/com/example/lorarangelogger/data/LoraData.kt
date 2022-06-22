package com.example.lorarangelogger.data

import com.example.lorarangelogger.utils.TimeHelper

data class LoraData(
    val timestamp: Long,
    val data: ByteArray,
    val rssi: Int,
    val snr: Int
) {
    val message = extractMessage()


    private fun extractMessage(): String {
        return data.decodeToString()
    }

    override fun toString(): String {
        return "${TimeHelper.getDateTime(timestamp)} (RSSI: $rssi, SNR: $snr): $message"
    }

    // Automatically generated, since we use a ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoraData

        if (timestamp != other.timestamp) return false
        if (!data.contentEquals(other.data)) return false
        if (rssi != other.rssi) return false
        if (snr != other.snr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + rssi
        result = 31 * result + snr
        return result
    }

}