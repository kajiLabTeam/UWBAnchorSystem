package net.harutiro.uwbanchorsystem.feature.serial.Entity

data class UWBResult(
    var time: Long = 0L,
    val seqCount: Long = 0L,
    val nLos: Int = 0,
    val distance: Int = 0,
    val azimuth: Double = 0.0,
    val azimuthFom: Int = 0,
    val elevation: Double = 0.0,
    val elevationFom: Int = 0,
    val rssi: Double = 0.0,
    val pDoA1: Double = 0.0,
    val pDoA2: Double = 0.0,
) {

    companion object {
        fun header(): String {
            return "time,seqCount,nLos,distance,azimuth,azimuthFom,elevation,elevationFom,rssi,pDoA1,pDoA2"
        }
    }

    override fun toString(): String {
        return "$time,$seqCount,$nLos,$distance,$azimuth,$azimuthFom,$elevation,$elevationFom,$rssi,$pDoA1,$pDoA2"
    }
}
