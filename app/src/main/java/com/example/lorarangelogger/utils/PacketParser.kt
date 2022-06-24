package com.example.lorarangelogger.utils

import android.util.Log
import com.example.lorarangelogger.data.LoraData
import com.example.lorarangelogger.data.LoraMsgData
import com.example.lorarangelogger.data.LoraStatReqData
import com.example.lorarangelogger.data.LoraStatSendData

object PacketParser {
    val CONTROL_ID = 0x99.toByte() //0b10011001
    val MESSAGE_ID = 0x66.toByte() //0b01100110
    val SF_SET = 0x81.toByte() //0b10000001
    val TX_SET = 0x93.toByte() //0b10010011
    val BW_SET = 0xA5.toByte() //0b10100101
    val STAT_REQUEST = 0xB7.toByte() //0b10110111
    val STAT_SEND = 0xC9.toByte() //0b11001001

    fun create_SF_SET(sf: Byte) : ByteArray{
        val message = mutableListOf<Byte>()
        message.add(CONTROL_ID)
        message.add(SF_SET)
        message.add(sf)
        return message.toByteArray()
    }

    fun create_TX_SET(tx: Byte) : ByteArray{
        val message = mutableListOf<Byte>()
        message.add(CONTROL_ID)
        message.add(TX_SET)
        message.add(tx)
        return message.toByteArray()
    }

    fun create_BW_SET(bw: Int) : ByteArray{
        val message = mutableListOf<Byte>()
        message.add(CONTROL_ID)
        message.add(BW_SET)
        for (i in 0..3) message.add((bw shr (i*8)).toByte())
        return message.toByteArray()
    }

    fun create_STAT_REQUEST(requestId: Long) : ByteArray{
        val message = mutableListOf<Byte>()
        message.add(CONTROL_ID)
        message.add(STAT_REQUEST)
        val longAsBytes = mutableListOf<Byte>()
        for (i in 0..7) longAsBytes.add((requestId shr (i*8)).toByte())
        return message.toByteArray() + longAsBytes.reversed().toByteArray()
    }

    fun create_MSG(msg: String): ByteArray {
        val message = mutableListOf<Byte>()
        message.add(MESSAGE_ID)
        return message.toByteArray() + msg.toByteArray()
    }

    /**
     * Turns the received Lora-Packet (guaranteed to not be empty) into a
     * corresponding LoraData instance.
     * If the packet is invalid, return null
     */
    fun parsePacket(packet: ByteArray, rcvTime: Long) : LoraData? {
        if (packet[0] == MESSAGE_ID) {
            return createLoraMsgData(packet, rcvTime)
        }
        if (packet[0] != CONTROL_ID) {
            return null;
        }
        if (packet[1] == STAT_SEND) {
            if (packet.size == 18) {
                return createLoraStatReqData(packet, rcvTime)
            }
            if (packet.size != 26) {
            return null
            }
            return createLoraStatSendData(packet, rcvTime)
        }
        return null 
    }

    fun createLoraMsgData(packet: ByteArray, rcvTime: Long): LoraMsgData? {
        if (packet.size < 10) {
            return null
        }
        
        val rssi = bigEndianIntConversion(packet.copyOfRange(1, 5))
        val snr = bigEndianIntConversion(packet.copyOfRange(5, 9))
        val message = String(packet.copyOfRange(9, packet.size))

        return LoraMsgData(rcvTime,rssi,snr,message)
    }

    fun createLoraStatReqData(packet: ByteArray, rcvTime: Long): LoraStatReqData?{
        //rcv time long, rssi int, snr int, send time long
        val sendTime = bigEndianLongConversion(packet.copyOfRange(2, 10))
        val rssi = bigEndianIntConversion(packet.copyOfRange(10, 14))
        val snr = bigEndianIntConversion(packet.copyOfRange(14, 18))
            
        return LoraStatReqData(rcvTime, rssi, snr, sendTime)
    }

    fun createLoraStatSendData(packet: ByteArray, rcvTime: Long): LoraStatSendData? {
        val sendTime = bigEndianLongConversion(packet.copyOfRange(2, 10))
        val sRssi = bigEndianIntConversion(packet.copyOfRange(10, 14))
        val sSnr = bigEndianIntConversion(packet.copyOfRange(14, 18))
        val rRssi = bigEndianIntConversion(packet.copyOfRange(18, 22))
        val rSnr = bigEndianIntConversion(packet.copyOfRange(22, 26))

        return LoraStatSendData(rcvTime,rRssi,rSnr,sRssi,sSnr,sendTime)
    }

    fun bigEndianIntConversion(bytes: ByteArray): Int {
        var result = 0
        for (b in bytes) {
            result = ((result shl 8) + b.toUByte().toInt())
        }
        return result
    }

    fun bigEndianLongConversion(bytes: ByteArray): Long {
        var result = 0L
        for (b in bytes) {
            result = ((result shl 8) + b.toUByte().toLong())
        }
        return result
    }

    fun ByteArray.toHex(): String = joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }
}