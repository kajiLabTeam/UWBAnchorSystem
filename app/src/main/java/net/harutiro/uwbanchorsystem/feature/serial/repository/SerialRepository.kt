package net.harutiro.uwbanchorsystem.feature.serial.repository

import android.content.Context
import android.util.Log
import net.harutiro.uwbanchorsystem.feature.serial.Entity.UWBResult
import net.harutiro.uwbanchorsystem.feature.serial.api.SerialApi
import net.harutiro.uwbanchorsystem.feature.utils.DateUtils

class SerialRepository {
    val serialApi = SerialApi()

    fun connectDevice(context: Context) {
        serialApi.connectDevice(context = context)
    }

    fun startSession(
        onLineRead: (UWBResult?) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        serialApi.startListening(
            onLineRead = { line ->
                Log.d("Main", "受信: $line")
                val rangePlotData = message2BlinkPacket(line)
                onLineRead(rangePlotData)
            },
            onError = { e ->
                Log.e("Main", "エラー: ${e.message}")
            },
        )
    }

    fun message2BlinkPacket(message: String): UWBResult? {
        println(message)
        // :で文字列を分割する
        val splitMessage = message.split(":")
        if (splitMessage.size != 4) {
            return null
        }
        if (splitMessage[1] == "INFO ") {
            return null
        }
        val raw = splitMessage[3].replace(" ", "")
        println(raw)

        // raw から 16進数 8文字分取得
        val uciHeader: ByteArray =
            raw.substring(0, 8)
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

        val uciPayload: ByteArray =
            raw.substring(8)
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()

        if (uciHeader.size == 4) {
            val count = uciHeader[3]
            println(count)
            if (count > 0) {
                if (
                    uciHeader[0] == 0x62.toByte() &&
                    uciHeader[1] == 0x00.toByte() &&
                    uciHeader[3] > 0x1B.toByte()
                ) {
                    println("kita")

                    val seqCount = extractSeqCnt(uciPayload)
                    println("seqCount: $seqCount")

                    if (uciPayload[27] != 0x00.toByte() && uciPayload[27] != 0x1b.toByte()) {
                        println("error")
                        return UWBResult(
                            time = 0,
                            seqCount = 0,
                            nLos = 0,
                            distance = 0,
                            azimuth = 0.0,
                            azimuthFom = 0,
                            elevation = 0.0,
                            elevationFom = 0,
                            rssi = 0.0,
                            pDoA1 = 0.0,
                            pDoA2 = 0.0,
                        )
                    } else {
                        val measNLos = extractNlos(uciPayload)
                        println(measNLos)
                        var measDistance = extractDistance(uciPayload)
                        println(measDistance)
                        if (uciPayload[27] == 0x1b.toByte()) {
                            measDistance *= -1
                        }
                        val measAzimuth = convertQFormatToFloat(extractAzimuth(uciPayload), 9, 7, 1)
                        println(measAzimuth)
                        val measAzimuthFom = extractAzimuthFom(uciPayload)
                        println(measAzimuthFom)
                        val measElevation = convertQFormatToFloat(extractElevation(uciPayload), 9, 7, 1)
                        println(measElevation)
                        val measElevationFom = extractElevationFom(uciPayload)
                        println(measElevationFom)
                        val measDestAzimuth = convertQFormatToFloat(extractDestAzimuth(uciPayload), 9, 7, 1)
                        println(measDestAzimuth)
                        val measDestElevation = convertQFormatToFloat(extractDestElevation(uciPayload), 9, 7, 1)
                        println(measDestElevation)
                        val measPdoa1 = convertQFormatToFloat(extractPdoa1(uciPayload), 9, 7, 7)
                        println(measPdoa1)
                        val measPdoa2 = convertQFormatToFloat(extractPdoa2(uciPayload), 9, 7, 7)
                        println(measPdoa2)

                        return UWBResult(
                            time = DateUtils.getTimeStamp(),
                            seqCount = seqCount,
                            nLos = measNLos,
                            distance = measDistance,
                            azimuth = measAzimuth.toDouble(),
                            azimuthFom = measAzimuthFom,
                            elevation = measElevation.toDouble(),
                            elevationFom = measElevationFom,
                            rssi = extractRssi(uciPayload).toDouble(),
                            pDoA1 = measPdoa1.toDouble(),
                            pDoA2 = measPdoa2.toDouble(),
                        )
                    }
                }
            }
        }
        return null
    }

    fun extractSeqCnt(byteArray: ByteArray): Long {
        return (
            ((byteArray[3].toInt() and 0xFF) shl 24) or
                ((byteArray[2].toInt() and 0xFF) shl 16) or
                ((byteArray[1].toInt() and 0xFF) shl 8) or
                (byteArray[0].toInt() and 0xFF)
        ).toLong()
    }

    fun extractNlos(byteArray: ByteArray): Int = byteArray[28].toInt() and 0xFF

    fun extractDistance(byteArray: ByteArray): Int = ((byteArray[30].toInt() and 0xFF) shl 8) or (byteArray[29].toInt() and 0xFF)

    fun extractAzimuth(byteArray: ByteArray): Int = ((byteArray[32].toInt() and 0xFF) shl 8) or (byteArray[31].toInt() and 0xFF)

    fun extractAzimuthFom(byteArray: ByteArray): Int = byteArray[33].toInt() and 0xFF

    fun extractElevation(byteArray: ByteArray): Int = ((byteArray[35].toInt() and 0xFF) shl 8) or (byteArray[34].toInt() and 0xFF)

    fun extractElevationFom(byteArray: ByteArray): Int = byteArray[36].toInt() and 0xFF

    fun extractDestAzimuth(byteArray: ByteArray): Int = ((byteArray[38].toInt() and 0xFF) shl 8) or (byteArray[37].toInt() and 0xFF)

    fun extractDestElevation(byteArray: ByteArray): Int = ((byteArray[41].toInt() and 0xFF) shl 8) or (byteArray[40].toInt() and 0xFF)

    fun extractPdoa1(byteArray: ByteArray): Int = ((byteArray[71].toInt() and 0xFF) shl 8) or (byteArray[70].toInt() and 0xFF)

    fun extractPdoa2(byteArray: ByteArray): Int = ((byteArray[77].toInt() and 0xFF) shl 8) or (byteArray[76].toInt() and 0xFF)

    fun extractRssi(byteArray: ByteArray): Int = byteArray[44].toInt() and 0xFF

    fun convertQFormatToFloat(
        qIn: Int,
        nInts: Int,
        nFracs: Int,
        roundOf: Int = 2,
    ): Double {
        val bits = nInts + nFracs
        var q = qIn

        // 2の補数を計算（符号ビットの処理）
        if ((qIn and (1 shl (bits - 1))) != 0) {
            q -= (1 shl bits)
        }

        // 小数部に変換
        val frac = q.toDouble() / (1 shl nFracs)

        // 四捨五入して返す
        return String.format("%.${roundOf}f", frac).toDouble()
    }

    fun stopSession() {
        serialApi.stopListening()
    }

    fun close() {
        serialApi.close()
    }
}
