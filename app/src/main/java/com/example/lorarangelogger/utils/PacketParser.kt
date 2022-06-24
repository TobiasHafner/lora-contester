package com.example.lorarangelogger.utils

import com.example.lorarangelogger.data.LoraData
import com.example.lorarangelogger.data.LoraMsgData
import com.example.lorarangelogger.data.LoraStatReqData
import com.example.lorarangelogger.data.LoraStatSendData

object PacketParser {

    fun create_SF_SET(sf: Int) : ByteArray{
        return ByteArray(0)
    }

    fun create_TX_SET(tx: Int) : ByteArray{
        return ByteArray(0)
    }

    fun create_BW_SET(bw: Int) : ByteArray{
        return ByteArray(0)
    }

    fun create_STAT_REQUEST(requestId: Long) : ByteArray{
        return ByteArray(0)
    }

    fun create_MSG(msg: String): ByteArray {
        return msg.toByteArray()
    }

    /**
     * Turns the received Lora-Packet (guaranteed to not be empty) into a
     * corresponding LoraData instance.
     * If the packet is invalid, return null
     */
    fun parsePacket(packet: ByteArray, rcvTime: Long) : LoraData? {
        return LoraMsgData(rcvTime,0,0,"")
        //return LoraStatReqData(rcvTime, 0, 0, 0)
        //return LoraStatSendData(rcvTime,0,0,0,0,0)
    }


}