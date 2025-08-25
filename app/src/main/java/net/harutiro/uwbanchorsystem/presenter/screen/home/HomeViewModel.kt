package net.harutiro.uwbanchorsystem.presenter.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

    var resultMessage by mutableStateOf("")
    var deviceName by mutableStateOf("")
        private set
    var isSensing by mutableStateOf(false)
        private set
    var sensingStatus by mutableStateOf("")
        private set
    var connectionStatus by mutableStateOf("未接続")
        private set
    var dataTransmissionCount by mutableStateOf(0)
        private set
    var isFileSending by mutableStateOf(false)
        private set

    fun initializeDeviceName(context: Context) {
        val preferencesManager = PreferencesManager.getInstance(context)
        deviceName = preferencesManager.deviceName
    }

    fun updateDeviceName(
        context: Context,
        newDeviceName: String,
    ) {
        deviceName = newDeviceName
        val preferencesManager = PreferencesManager.getInstance(context)
        preferencesManager.deviceName = newDeviceName
    }

    fun connectDevice(context: Context) {
        serialRepository.connectDevice(context)

        // NearByRepositoryの初期化とコールバック設定
        if (context is android.app.Activity) {
            nearByRepository = NearByRepository.getInstance(context)
            nearByRepository?.sensingControlCallback = this
            sensingStatus = "リモート制御待機中"
            connectionStatus = "NearBy初期化完了"
            Log.d("HomeViewModel", "NearByRepository初期化完了")
        } else {
            connectionStatus = "初期化エラー: Activityではありません"
            Log.e("HomeViewModel", "Context is not Activity")
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

    fun startSensing(
        context: Context,
        fileName: String,
    ) {
        if (fileName.isEmpty() || !isValidFileName(fileName)) return
        if (isSensing) return

        isSensing = true
        sensingStatus = if (sensingStatus.contains("リモート")) "リモートセンシング実行中: $fileName" else "ローカルセンシング実行中: $fileName"

        // 保存先を指定
        otherFileStorageApi =
            OtherFileStorageApi(
                context = context,
                name = fileName,
                queue = queue,
            )
        otherFileStorageApi?.saveAtBatch()

        // ヘッダーを作成
        queue.add(UWBResult.header())

        serialRepository.startSession(
            onLineRead = { line ->
                if (line == null) return@startSession
                resultMessage =
                    """
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
                Log.d("Main", line.toString())

                addQueue(line.toString())

                // リアルタイムでMac側にelevation、azimuthデータを送信
                sendRealtimeData(line)
            },
            onError = {
            },
        )
    }

    fun stopSensing(context: Context) {
        if (!isSensing) return

        isSensing = false
        sensingStatus = if (sensingStatus.contains("リモート")) "リモートセンシング終了" else "ローカルセンシング終了"

        val file = otherFileStorageApi?.stop()
        otherFileStorageApi = null

        serialRepository.stopSession()

        // Mac接続がある場合は自動的にMacに送信、なければMinioにアップロード
        if (nearByRepository != null && isConnectedToMac()) {
            sendFileToMac(context, file)
        } else {
            uploadToMinio(context, file)
        }
    }

    // Macに接続されているかチェック
    private fun isConnectedToMac(): Boolean {
        // NearByRepositoryの接続状態をチェック
        return nearByRepository?.hasActiveConnections() ?: false
    }

    // Macへの直接送信（内部メソッド）
    private fun sendFileToMac(
        context: Context,
        file: java.io.File?,
    ) {
        file?.let { notNullFile ->
            isFileSending = true
            resultMessage = "Macに自動送信中..."
            Log.d("HomeViewModel", "ファイル送信開始: ${notNullFile.name}")

            nearByRepository?.sendFile(
                file = notNullFile,
                onProgress = { progress ->
                    resultMessage = "Macに送信中: $progress%"
                    connectionStatus = "ファイル送信中: $progress%"
                },
                onComplete = { success, message ->
                    isFileSending = false
                    if (success) {
                        resultMessage = "Mac送信完了: ${notNullFile.name}"
                        connectionStatus = "ファイル送信完了"
                        Log.d("HomeViewModel", "ファイル送信成功: ${notNullFile.name}")
                    } else {
                        resultMessage = "Mac送信失敗: $message - Minioにバックアップ中..."
                        connectionStatus = "ファイル送信失敗 - Minioバックアップ中"
                        Log.e("HomeViewModel", "ファイル送信失敗: $message")
                        // Mac送信失敗時はMinioにバックアップ
                        uploadToMinio(context, notNullFile)
                    }
                },
            )
        } ?: run {
            isFileSending = false
            resultMessage = "送信するファイルがありません"
            Log.w("HomeViewModel", "送信するファイルがありません")
        }
    }

    // Minioへのアップロード
    private fun uploadToMinio(
        context: Context,
        file: java.io.File?,
    ) {
        val minioApiClient = MinioApiClient(context)
        file?.let { notNullFile ->
            minioApiClient.uploadFile(
                file = notNullFile,
                bucket = "uwb",
                path = "sensing/",
            ) { success, message ->
                if (success) {
                    resultMessage = "Minioアップロード成功: $message"
                } else {
                    resultMessage = "Minioアップロード失敗: $message"
                }
            }
        }
    }

    fun addQueue(line: String) {
        queue.add(line)
    }

    // リアルタイムデータ送信
    private fun sendRealtimeData(uwbResult: UWBResult) {
        Log.d("HomeViewModel", "=== sendRealtimeData開始 ===")
        Log.d(
            "HomeViewModel",
            "UWBResult: elevation=${uwbResult.elevation}, azimuth=${uwbResult.azimuth}, distance=${uwbResult.distance}, seqCount=${uwbResult.seqCount}",
        )

        // ファイル送信中はリアルタイムデータ送信をスキップ
        if (isFileSending) {
            Log.d("HomeViewModel", "ファイル送信中のためリアルタイムデータ送信をスキップ")
            return
        }

        nearByRepository?.let { repository ->
            // 接続状態をチェック
            val hasConnections = repository.hasActiveConnections()
            val connectedCount = repository.getConnectedEndpointsCount()
            Log.d("HomeViewModel", "接続状態: hasConnections=$hasConnections, count=$connectedCount")

            if (!hasConnections) {
                connectionStatus = "Mac未接続"
                Log.w("HomeViewModel", "Mac未接続のためデータ送信をスキップ")
                return
            }

            // elevation、azimuthを含むJSONデータを作成（フォーマット改善）
            val realtimeData = """{"type":"REALTIME_DATA","deviceName":"$deviceName","timestamp":${System.currentTimeMillis()},"elevation":${uwbResult.elevation},"azimuth":${uwbResult.azimuth},"distance":${uwbResult.distance},"nlos":${uwbResult.nLos},"rssi":${uwbResult.rssi},"seqCount":${uwbResult.seqCount}}"""

            Log.d("HomeViewModel", "送信JSON: $realtimeData")
            Log.d("HomeViewModel", "JSON長: ${realtimeData.length} bytes")

            try {
                repository.sendData(realtimeData)
                dataTransmissionCount++
                connectionStatus =
                    if (isFileSending) {
                        "ファイル送信中... (データ送信数: $dataTransmissionCount)"
                    } else {
                        "データ送信中 (送信数: $dataTransmissionCount)"
                    }
                Log.d("HomeViewModel", "リアルタイムデータ送信成功: count=$dataTransmissionCount")

                // 送信頻度をログ出力（10回おき）
                if (dataTransmissionCount % 10 == 0) {
                    Log.i("HomeViewModel", "リアルタイムデータ送信状況: ${dataTransmissionCount}回送信完了")
                }
            } catch (e: Exception) {
                connectionStatus = "送信エラー: ${e.message}"
                Log.e("HomeViewModel", "リアルタイムデータ送信エラー", e)
            }
        } ?: run {
            connectionStatus = "NearByRepository未初期化"
            Log.e("HomeViewModel", "NearByRepository is null")
        }

        Log.d("HomeViewModel", "=== sendRealtimeData終了 ===")
    }

    fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }

    fun isValidDeviceName(deviceName: String): Boolean {
        return deviceName.isNotBlank() && deviceName.length in 1..20 && deviceName.matches(Regex("[\\w\\-. ]+"))
    }

    // NearBy接続を手動開始（デバッグ用）
    fun startNearByDiscovery() {
        nearByRepository?.let { repository ->
            try {
                repository.startDiscovery()
                connectionStatus = "Mac検索中..."
                Log.d("HomeViewModel", "NearBy Discovery開始")
            } catch (e: Exception) {
                connectionStatus = "Discovery開始エラー: ${e.message}"
                Log.e("HomeViewModel", "Discovery開始エラー", e)
            }
        } ?: run {
            connectionStatus = "NearByRepository未初期化"
            Log.e("HomeViewModel", "NearByRepository is null for discovery")
        }
    }

    // NearBy広告を手動開始（デバッグ用）
    fun startNearByAdvertising() {
        nearByRepository?.let { repository ->
            try {
                repository.startAdvertise()
                connectionStatus = "Mac接続待機中..."
                Log.d("HomeViewModel", "NearBy Advertising開始")
            } catch (e: Exception) {
                connectionStatus = "Advertising開始エラー: ${e.message}"
                Log.e("HomeViewModel", "Advertising開始エラー", e)
            }
        } ?: run {
            connectionStatus = "NearByRepository未初期化"
            Log.e("HomeViewModel", "NearByRepository is null for advertising")
        }
    }

    // 接続状態の詳細取得
    fun getDetailedConnectionStatus(): String {
        return nearByRepository?.let { repository ->
            "接続状態: ${repository.connectState}\n" +
                "受信データ数: ${repository.receivedDataList.size}\n" +
                "発見デバイス数: ${repository.discoveredDevices.size}\n" +
                "接続要求数: ${repository.connectionRequests.size}"
        } ?: "NearByRepository未初期化"
    }

    // Ping送信テスト機能
    fun sendPingTest() {
        nearByRepository?.let { repository ->
            try {
                repository.sendPing()
                connectionStatus = "Ping送信テスト実行"
                Log.d("HomeViewModel", "Ping送信テスト実行")
            } catch (e: Exception) {
                connectionStatus = "Ping送信エラー: ${e.message}"
                Log.e("HomeViewModel", "Ping送信エラー", e)
            }
        } ?: run {
            connectionStatus = "NearByRepository未初期化"
            Log.e("HomeViewModel", "NearByRepository is null for ping")
        }
    }

    // 詳細な接続状態取得
    fun getDetailedNearByStatus(): String {
        return nearByRepository?.getDetailedConnectionStatus() ?: "未初期化"
    }
}
