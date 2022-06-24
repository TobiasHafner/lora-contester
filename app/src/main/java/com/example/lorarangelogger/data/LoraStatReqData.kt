package com.example.lorarangelogger.data

import com.example.lorarangelogger.utils.TimeHelper

class LoraStatReqData(
    override val rcvTime: Long,
    override val rssi: Int,
    override val snr: Int,
    val sendTime: Long
) : LoraData {
    /**
     * Only approximate, since the clocks of sender and receiver are not synchronized!
     */
    val transmissionTime = rcvTime - sendTime
    override val type = "ECHO_REQUEST"

    override fun summary(): String {
        return "${headerSummary()}:    Sender Time: ${TimeHelper.getTime(sendTime)}, Transmission Time: $transmissionTime"
    }
}