package net.harutiro.uwbanchorsystem.presenter.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.harutiro.uwbanchorsystem.feature.file.OtherFileStorageApi
import net.harutiro.uwbanchorsystem.feature.serial.Entity.UWBResult
import net.harutiro.uwbanchorsystem.feature.serial.repository.SerialRepository

class HomeViewModel : ViewModel() {

    val serialRepository = SerialRepository()
    var otherFileStorageApi: OtherFileStorageApi? = null
    val queue: ArrayDeque<String> = ArrayDeque(listOf())

    var resultMessage by  mutableStateOf("")

    fun connectDevice(context: Context){
        serialRepository.connectDevice(context)
    }

    fun startSensing(context: Context, fileName: String){
        if(fileName.isEmpty() || !isValidFileName(fileName)) return

        // 保存先を指定
        otherFileStorageApi = OtherFileStorageApi(
            context = context,
            name = fileName,
            queue = queue
        )
        otherFileStorageApi?.saveAtBatch()

        // ヘッダーを作成
        queue.add(UWBResult.header())

        serialRepository.startSession(
            onLineRead = { line ->
                if(line == null) return@startSession
                resultMessage = """
                            nlos: ${line.nLos}
                            distance: ${line.distance}
                            elevation: ${line.elevation}
                            azimuth: ${line.azimuth}
                            elevationFom: ${line.elevationFom}
                            rssi: ${line.rssi}
                            pDoA1: ${line.pDoA1}
                            pDoA2: ${line.pDoA2}
                            seqCount: ${line.seqCount}
                        """.trimIndent()
                Log.d("Main",line.toString())

                addQueue(line.toString())
            },
            onError = {

            }
        )
    }

    fun stopSensing():String {
        val filePath = otherFileStorageApi?.filePath
        otherFileStorageApi?.stop()
        otherFileStorageApi = null

        serialRepository.stopSession()

        return filePath ?: ""
    }

    fun addQueue(line: String){
        queue.add(line)
    }

    fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }
}
