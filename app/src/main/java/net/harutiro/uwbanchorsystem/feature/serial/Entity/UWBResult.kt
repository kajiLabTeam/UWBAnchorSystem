package net.harutiro.uwbanchorsystem.feature.serial.Entity

data class UWBResult(
    val time: Long = 0L,
    val seqCount: Long = 0L,
    val nLos: Int = 0,
    val distance: Int = 0,
    val azimuth: Double = 0.0,
    val azimuthFom: Int = 0,
    val elevation: Double = 0.0,
    val elevationFom: Int = 0,
    val rssi: Double = 0.0,
    val pDoA1: Double = 0.0,
    val pDoA2: Double = 0.0
)