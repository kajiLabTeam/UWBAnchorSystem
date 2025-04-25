package net.harutiro.uwbanchorsystem.feature.serial.Entity

import java.math.BigInteger

data class BlinkPacket (
    val uCIRangingNotification: Int,
    val uCIPayloadLength:Int,
    val sequenceCounter:Long,
    val sessionId:Long,
    val rfu1:Int,
    val rangingInterval:Long,
    val measurementType:Int,
    val rfu2:Int,
    val macAddressIndicator8Bytes:String,
    val sessionIdOfPrimarySession0ifSessionNotPartOfHybridUwbSession:Long,
    val rfu3:Long,
    val numberOfRangingMeasurement:Int,
    val macAddress:String,
    val status:Int,
    val messageControl:Int,
    val frameType:Int,
    val nLos:Int,
    val aoaAzimuth:Double,
    val aoaAzimuthFOM: Double,
    val aoaElevation:Double,
    val aoaElevationFOM:Double,
    val frameCounter:Long,
    val rxTimestamp: BigInteger,
    val vendorSpecificLength:Int,
    val vendorExtensionLength:Int,
    val rssiRx1:Double,
    val rssiRx2:Double,
    val numberOfPDoA:Int,
    val pDoA1:Double,
    val pDoA2:Double,
    val antennaPairInfo:Long,
    val wifiCoexStatus:Int,
    val authenticityInfoPresence:Int,
) {
    override fun toString(): String {
        return """
            BlinkPacket:
              uCIRangingNotification = $uCIRangingNotification
              uCIPayloadLength = $uCIPayloadLength
              sequenceCounter = $sequenceCounter
              sessionId = $sessionId
              rfu1 = $rfu1
              rangingInterval = $rangingInterval
              measurementType = $measurementType
              rfu2 = $rfu2
              macAddressIndicator8Bytes = $macAddressIndicator8Bytes
              sessionIdOfPrimarySession0ifSessionNotPartOfHybridUwbSession = $sessionIdOfPrimarySession0ifSessionNotPartOfHybridUwbSession
              rfu3 = $rfu3
              numberOfRangingMeasurement = $numberOfRangingMeasurement
              macAddress = $macAddress
              status = $status
              messageControl = $messageControl
              frameType = $frameType
              nLos = $nLos
              aoaAzimuth = $aoaAzimuth
              aoaAzimuthFOM = $aoaAzimuthFOM
              aoaElevation = $aoaElevation
              aoaElevationFOM = $aoaElevationFOM
              frameCounter = $frameCounter
              rxTimestamp = $rxTimestamp
              vendorSpecificLength = $vendorSpecificLength
              vendorExtensionLength = $vendorExtensionLength
              rssiRx1 = $rssiRx1
              rssiRx2 = $rssiRx2
              numberOfPDoA = $numberOfPDoA
              pDoA1 = $pDoA1
              pDoA2 = $pDoA2
              antennaPairInfo = $antennaPairInfo
              wifiCoexStatus = $wifiCoexStatus
              authenticityInfoPresence = $authenticityInfoPresence
        """.trimIndent()
    }
}


data class BlinkPacketField<T>(
    val name: String,
    val length: Int,
    val transform: (ByteArray) -> T
)