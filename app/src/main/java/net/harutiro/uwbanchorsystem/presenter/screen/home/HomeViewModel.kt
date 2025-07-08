package net.harutiro.uwbanchorsystem.presenter.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import net.harutiro.uwbanchorsystem.feature.file.OtherFileStorageApi
import net.harutiro.uwbanchorsystem.feature.http.MinioApiClient
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository
import net.harutiro.uwbanchorsystem.feature.nearby.repository.SensingControlCallback
import net.harutiro.uwbanchorsystem.feature.serial.Entity.UWBResult
import net.harutiro.uwbanchorsystem.feature.serial.repository.SerialRepository
import net.harutiro.uwbanchorsystem.feature.utils.PreferencesManager

class HomeViewModel : ViewModel(), SensingControlCallback {

    val serialRepository = SerialRepository()
    var otherFileStorageApi: OtherFileStorageApi? = null
    val queue: ArrayDeque<String> = ArrayDeque(listOf())
    private var nearByRepository: NearByRepository? = null

    var resultMessage by  mutableStateOf("")
    var deviceName by mutableStateOf("")
        private set
    var isSensing by mutableStateOf(false)
        private set
    var sensingStatus by mutableStateOf("")
        private set

    fun initializeDeviceName(context: Context) {
        val preferencesManager = PreferencesManager.getInstance(context)
        deviceName = preferencesManager.deviceName
    }

    fun updateDeviceName(context: Context, newDeviceName: String) {
        deviceName = newDeviceName
        val preferencesManager = PreferencesManager.getInstance(context)
        preferencesManager.deviceName = newDeviceName
    }

    fun connectDevice(context: Context){
        serialRepository.connectDevice(context)
        
        // NearByRepositoryの初期化とコールバック設定
        if (context is android.app.Activity) {
            nearByRepository = NearByRepository.getInstance(context)
            nearByRepository?.sensingControlCallback = this
            sensingStatus = "リモート制御待機中"
        }
    }

    // SensingControlCallbackの実装
    override fun onStartSensingCommand(fileName: String) {
        Log.d("HomeViewModel", "リモートセンシング開始: $fileName")
        sensingStatus = "リモートセンシング開始: $fileName"
        if (nearByRepository?.activity != null) {
            startSensing(nearByRepository!!.activity, fileName)
        }
    }

    override fun onStopSensingCommand() {
        Log.d("HomeViewModel", "リモートセンシング終了")
        sensingStatus = "リモートセンシング終了"
        if (nearByRepository?.activity != null) {
            stopSensing(nearByRepository!!.activity)
        }
    }

    fun startSensing(context: Context, fileName: String){
        if(fileName.isEmpty() || !isValidFileName(fileName)) return
        if(isSensing) return

        isSensing = true
        sensingStatus = if (sensingStatus.contains("リモート")) "リモートセンシング実行中: $fileName" else "ローカルセンシング実行中: $fileName"

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
        if(!isSensing) return
        
        isSensing = false
        sensingStatus = if (sensingStatus.contains("リモート")) "リモートセンシング終了" else "ローカルセンシング終了"
        
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

    fun isValidDeviceName(deviceName: String): Boolean {
        return deviceName.isNotBlank() && deviceName.length in 1..20 && deviceName.matches(Regex("[\\w\\-. ]+"))
    }
}
