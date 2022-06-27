package com.example.lorarangelogger.data

import com.example.lorarangelogger.utils.TimeHelper

class LoraStatSendData(
    override val rcvTime: Long,
    override val rssi: Int,
    override val snr: Int,
    val sendRssi: Int,
    val sendSnr: Int,
    val sendTime: Long
) : LoraData {

    val rtt = rcvTime - sendTime
    override val type = "ECHO_ANSWER"

    override fun summary(): String {
        return "RSSI: ($sendRssi | $rssi)    SNR: ($sendSnr | $snr)    RTT: ${TimeHelper.getPreciseDuration(rtt)}"
    }
}