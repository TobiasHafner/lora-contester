package com.example.lorarangelogger.data

class Measurement(
    val sendTime: Long
) {
    var hasAnswered = false
    var sendRssi = 0
    var answerRssi = 0
    var sendSnr = 0
    var answerSnr = 0
    var rtt = 0L


    fun answer(data: LoraStatSendData) {
        this.sendRssi = data.sendRssi
        this.answerRssi = data.rssi
        this.sendSnr = data.sendSnr
        this.answerSnr = data.snr
        this.rtt = data.rtt
        hasAnswered = true
    }

    override fun toString(): String {
        return if(hasAnswered) {
            "[time: $sendTime, rssiS: $sendRssi, snrS: $sendSnr, rssiA: $answerRssi, " +
                    "snrA: $answerSnr, rtt: $rtt]"
        } else {
            "[time: $sendTime (no answer)]"
        }
    }

}