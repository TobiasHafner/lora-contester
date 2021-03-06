package com.example.lorarangelogger.utils

import java.io.InputStream

object KissTranslator {
    private const val FEND = 0xC0.toByte()
    private const val FESC = 0xDB.toByte()
    private const val TFEND = 0xDC.toByte()
    private const val TFESC = 0xDD.toByte()

    private var escMode = false
    private val buf = mutableListOf<Byte>()

    fun makeFrame(msg: ByteArray): ByteArray {
        val frame = mutableListOf(FEND)
        for (b in msg) {
            when (b) {
                FESC -> {
                    frame.add(FESC)
                    frame.add(TFESC)
                }
                FEND -> {
                    frame.add(FESC)
                    frame.add(TFEND)
                }
                else -> {
                    frame.add(b)
                }
            }
        }
        frame.add(FEND)
        return frame.toByteArray()
    }

    fun readFrame(ser: InputStream): ByteArray {
        var msg = ByteArray(0)

        while (ser.available() > 0) {
            var c = ser.read().toByte()
            if (c == FEND) {
                escMode = false
                msg = buf.toByteArray()
                buf.clear()
                break
            }
            if (c == FESC) {
                escMode = true
                continue
            }
            if (escMode) {
                escMode = false
                c = if (c == TFESC) {
                    FESC
                } else if (c == TFEND) {
                    FEND
                } else {
                    continue
                }
            }
            buf.add(c)
        }
        return msg

    }

}