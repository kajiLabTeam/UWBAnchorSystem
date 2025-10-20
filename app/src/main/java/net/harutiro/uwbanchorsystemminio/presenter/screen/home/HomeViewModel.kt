package net.harutiro.uwbanchorsystemminio.presenter.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.harutiro.uwbanchorsystemminio.feature.file.OtherFileStorageApi
import net.harutiro.uwbanchorsystemminio.feature.http.MinioApiClient
import net.harutiro.uwbanchorsystemminio.feature.serial.Entity.UWBResult
import net.harutiro.uwbanchorsystemminio.feature.serial.repository.SerialRepository

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

    fun stopSensing(context: Context) {
        val file = otherFileStorageApi?.stop()
        otherFileStorageApi = null

        serialRepository.stopSession()

        val minioApiClient = MinioApiClient(context)
        file?.let { notNullFile ->
            minioApiClient.uploadFile(
                file = notNullFile,
                bucket = "uwb",
                path = "sensing/",
            ) { success, message ->
                if (success) {
                    resultMessage = "アップロード成功: $message"
                } else {
                    resultMessage = "アップロード失敗: $message"
                }
            }
        }
    }

    fun addQueue(line: String){
        queue.add(line)
    }

    fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }
}
