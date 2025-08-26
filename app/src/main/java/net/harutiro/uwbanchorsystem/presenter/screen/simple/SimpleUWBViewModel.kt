package net.harutiro.uwbanchorsystem.presenter.screen.simple

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.harutiro.uwbanchorsystem.feature.file.OtherFileStorageApi
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository
import net.harutiro.uwbanchorsystem.feature.nearby.repository.SensingControlCallback
import net.harutiro.uwbanchorsystem.feature.serial.Entity.UWBResult
import net.harutiro.uwbanchorsystem.feature.serial.repository.SerialRepository
import net.harutiro.uwbanchorsystem.feature.utils.PreferencesManager

data class SimpleUWBUiState(
    val isDiscovering: Boolean = false,
    val isConnected: Boolean = false,
    val statusMessage: String = "Mac端末からの接続を待機中...",
    val connectionRequest: ConnectionRequest? = null,
    val connectedDeviceName: String = "",
    val pairedAntenna: String? = null,
    val lastReceivedMessage: String = "",
    val savedDeviceName: String = "",
    val isSensing: Boolean = false,
    val sensingStatus: String = "",
)

class SimpleUWBViewModel(
    private val activity: Activity,
) : ViewModel(), SensingControlCallback {
    private val nearByRepository = NearByRepository.getInstance(activity)
    private val preferencesManager = PreferencesManager.getInstance(activity)
    private val serialRepository = SerialRepository()
    private val queue: ArrayDeque<String> = ArrayDeque(listOf())
    private var otherFileStorageApi: OtherFileStorageApi? = null

    private val _uiState = MutableStateFlow(SimpleUWBUiState())
    val uiState: StateFlow<SimpleUWBUiState> = _uiState.asStateFlow()

    init {
        setupNearByCallbacks()
        loadSavedDeviceName()

        // センシング制御コールバックを設定
        nearByRepository.sensingControlCallback = this

        // SerialRepositoryの初期化
        serialRepository.connectDevice(activity).onFailure { e ->
            Log.e("SimpleUWBViewModel", "USB接続初期化エラー: ${e.message}", e)
        }
    }

    private fun loadSavedDeviceName() {
        val savedName = preferencesManager.deviceName
        _uiState.value =
            _uiState.value.copy(
                savedDeviceName = savedName,
            )
    }

    private fun saveDeviceName(deviceName: String) {
        preferencesManager.deviceName = deviceName
        _uiState.value =
            _uiState.value.copy(
                savedDeviceName = deviceName,
            )
    }

    private fun setupNearByCallbacks() {
        nearByRepository.setOnConnectionStateChangedListener { state ->
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        statusMessage = state,
                    )
            }
        }

        nearByRepository.setOnConnectionRequestReceivedListener { request ->
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        connectionRequest = request,
                    )

                // Mac側で端末が保存された後、検索を停止
                if (_uiState.value.isDiscovering) {
                    stopDiscovery()
                    _uiState.value =
                        _uiState.value.copy(
                            statusMessage = "端末が登録され接続されました。アンテナとの紐付けをお待ちください...",
                        )
                }
            }
        }

        nearByRepository.setOnDeviceConnectedListener { device ->
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        isConnected = true,
                        connectedDeviceName = device.deviceName,
                        statusMessage = "${device.deviceName} に接続しました",
                        connectionRequest = null,
                    )
            }
        }

        nearByRepository.setOnDeviceDisconnectedListener { endpointId ->
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        isConnected = false,
                        connectedDeviceName = "",
                        pairedAntenna = null,
                        statusMessage = "接続が切断されました",
                        lastReceivedMessage = "",
                    )
            }
        }

        nearByRepository.setOnMessageReceivedListener { message ->
            viewModelScope.launch {
                // ペアリング情報の処理
                if (message.content.startsWith("PAIRING:")) {
                    val parts = message.content.split(":")
                    if (parts.size >= 3) {
                        val antennaName = parts[2]
                        _uiState.value =
                            _uiState.value.copy(
                                pairedAntenna = antennaName,
                                lastReceivedMessage = "アンテナ「$antennaName」とペアリングしました",
                            )
                    }
                } else if (message.content.startsWith("UNPAIR:")) {
                    _uiState.value =
                        _uiState.value.copy(
                            pairedAntenna = null,
                            lastReceivedMessage = "ペアリングが解除されました",
                        )
                } else {
                    // 通常のメッセージ
                    _uiState.value =
                        _uiState.value.copy(
                            lastReceivedMessage = message.content,
                        )
                }
            }
        }
    }

    fun startPairing(deviceName: String) {
        viewModelScope.launch {
            // 端末名を保存
            if (deviceName.isNotBlank()) {
                saveDeviceName(deviceName)
            }

            _uiState.value =
                _uiState.value.copy(
                    isDiscovering = true,
                    statusMessage = "Mac端末を検索しています...",
                )

            try {
                // 端末名を設定してAdvertisingを開始
                nearByRepository.startAdvertising(deviceName)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isDiscovering = false,
                        statusMessage = "エラー: ${e.message}",
                    )
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isDiscovering = false,
                    statusMessage = "検索を停止しました",
                )

            nearByRepository.stopAdvertising()
        }
    }

    fun acceptConnection() {
        viewModelScope.launch {
            _uiState.value.connectionRequest?.let { request ->
                nearByRepository.acceptConnection(request.endpointId)
                _uiState.value =
                    _uiState.value.copy(
                        statusMessage = "接続を承認しました",
                    )
            }
        }
    }

    fun rejectConnection() {
        viewModelScope.launch {
            _uiState.value.connectionRequest?.let { request ->
                nearByRepository.rejectConnection(request.endpointId)
                _uiState.value =
                    _uiState.value.copy(
                        connectionRequest = null,
                        statusMessage = "接続を拒否しました",
                    )
            }
        }
    }

    fun disconnectAll() {
        viewModelScope.launch {
            // センシングも確実に停止
            if (_uiState.value.isSensing) {
                stopSensing(activity)
            }
            nearByRepository.disconnectAll()
            _uiState.value =
                _uiState.value.copy(
                    isConnected = false,
                    isDiscovering = false,
                    connectedDeviceName = "",
                    pairedAntenna = null,
                    statusMessage = "すべての接続を切断しました",
                    lastReceivedMessage = "",
                )
        }
    }

    // SensingControlCallbackの実装
    override fun onStartSensingCommand(fileName: String) {
        Log.d("SimpleUWBViewModel", "リモートセンシング開始: $fileName")
        startSensing(activity, fileName)
    }

    override fun onStopSensingCommand() {
        Log.d("SimpleUWBViewModel", "リモートセンシング終了")
        stopSensing(activity)
    }

    private fun startSensing(
        context: Context,
        fileName: String,
    ) {
        if (fileName.isEmpty() || !isValidFileName(fileName)) return
        if (_uiState.value.isSensing) return

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSensing = true,
                    sensingStatus = "リモートセンシング実行中: $fileName",
                    lastReceivedMessage = "センシング開始: $fileName",
                )
        }

        // 保存先を指定
        otherFileStorageApi =
            OtherFileStorageApi(
                context = context,
                name = fileName,
                queue = queue,
            )

        // 既存ファイルをクリア（新しいセンシングセッション開始のため）
        otherFileStorageApi?.clearFile()
        otherFileStorageApi?.saveAtBatch()

        // ヘッダーを作成
        queue.add(UWBResult.header())

        // センシング開始前にUSB接続を再確認
        val connectionResult = serialRepository.connectDevice(context)
        if (connectionResult.isFailure) {
            Log.e("SimpleUWBViewModel", "USB接続エラー: ${connectionResult.exceptionOrNull()?.message}")
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    isSensing = false,
                    sensingStatus = "USB接続エラー: ${connectionResult.exceptionOrNull()?.message}",
                    lastReceivedMessage = "USB接続に失敗しました"
                )
            }
            return
        }

        serialRepository.startSession(
            onLineRead = { line ->
                if (line == null) return@startSession
                Log.d("SimpleUWBViewModel", line.toString())
                queue.add(line.toString())

                // Mac側にリアルタイムデータを送信
                sendRealtimeDataToMac(line)
            },
            onError = { error ->
                Log.e("SimpleUWBViewModel", "シリアル通信エラー: ${error.message}", error)
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isSensing = false,
                        sensingStatus = "シリアル通信エラー: ${error.message}",
                        lastReceivedMessage = "シリアル通信でエラーが発生しました"
                    )
                }
            }
        )
    }

    private fun stopSensing(context: Context) {
        serialRepository.stopSession()
        otherFileStorageApi?.saveAtBatch()

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isSensing = false,
                    sensingStatus = "センシング停止 - CSVファイル送信中",
                    lastReceivedMessage = "センシング停止 - CSVファイル準備中",
                )
        }

        // CSVファイルをMac側に送信
        sendCsvFileToMac(context)
    }

    private fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }

    private fun sendRealtimeDataToMac(uwbResult: UWBResult) {
        try {
            // リアルタイムデータをJSON形式で送信
            val realtimeData =
                """
                {
                    "type": "REALTIME_DATA",
                    "timestamp": ${System.currentTimeMillis()},
                    "deviceName": "${preferencesManager.deviceName}",
                    "data": {
                        "nlos": ${uwbResult.nLos},
                        "distance": ${uwbResult.distance},
                        "elevation": ${uwbResult.elevation},
                        "azimuth": ${uwbResult.azimuth},
                        "elevationFom": ${uwbResult.elevationFom},
                        "rssi": ${uwbResult.rssi},
                        "pDoA1": ${uwbResult.pDoA1},
                        "pDoA2": ${uwbResult.pDoA2},
                        "seqCount": ${uwbResult.seqCount}
                    }
                }
                """.trimIndent()

            nearByRepository.sendData(realtimeData)
            Log.d("SimpleUWBViewModel", "リアルタイムデータ送信: ${uwbResult.distance}m")
        } catch (e: Exception) {
            Log.e("SimpleUWBViewModel", "リアルタイムデータ送信エラー", e)
        }
    }

    private fun sendCsvFileToMac(context: Context) {
        viewModelScope.launch {
            try {
                // 少し待機してファイル書き込みが完了するのを待つ
                kotlinx.coroutines.delay(1000)

                // 最新のCSVファイルを検索
                val csvFile = findLatestCsvFile(context)
                if (csvFile != null && csvFile.exists()) {
                    Log.d("SimpleUWBViewModel", "CSVファイル送信開始: ${csvFile.name}")

                    _uiState.value =
                        _uiState.value.copy(
                            sensingStatus = "CSVファイル送信中: ${csvFile.name}",
                            lastReceivedMessage = "CSVファイル送信中: ${csvFile.name}",
                        )

                    nearByRepository.sendFile(
                        file = csvFile,
                        onProgress = { progress ->
                            viewModelScope.launch {
                                _uiState.value =
                                    _uiState.value.copy(
                                        sensingStatus = "CSVファイル送信中: $progress%",
                                    )
                            }
                        },
                        onComplete = { success, message ->
                            viewModelScope.launch {
                                _uiState.value =
                                    _uiState.value.copy(
                                        sensingStatus = if (success) "CSVファイル送信完了" else "CSVファイル送信失敗: $message",
                                        lastReceivedMessage = if (success) "CSVファイル送信完了: ${csvFile.name}" else "CSVファイル送信失敗: $message",
                                    )
                            }
                            Log.d("SimpleUWBViewModel", if (success) "CSVファイル送信完了" else "CSVファイル送信失敗: $message")
                        },
                    )
                } else {
                    Log.e("SimpleUWBViewModel", "CSVファイルが見つかりません")
                    _uiState.value =
                        _uiState.value.copy(
                            sensingStatus = "CSVファイルが見つかりません",
                            lastReceivedMessage = "CSVファイルが見つかりません",
                        )
                }
            } catch (e: Exception) {
                Log.e("SimpleUWBViewModel", "CSVファイル送信エラー", e)
                _uiState.value =
                    _uiState.value.copy(
                        sensingStatus = "CSVファイル送信エラー: ${e.message}",
                        lastReceivedMessage = "CSVファイル送信エラー: ${e.message}",
                    )
            }
        }
    }

    private fun findLatestCsvFile(context: Context): java.io.File? {
        try {
            // アプリのDocumentsディレクトリを確認
            val documentsDir = java.io.File(context.getExternalFilesDir(null), "Documents")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // .csvファイルを検索
            val csvFiles =
                documentsDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() == "csv"
                }

            // 最新のファイルを取得（最後に更新されたファイル）
            return csvFiles?.maxByOrNull { it.lastModified() }
        } catch (e: Exception) {
            Log.e("SimpleUWBViewModel", "CSVファイル検索エラー", e)
            return null
        }
    }

    override fun onCleared() {
        super.onCleared()
        // センシングを停止してUSB接続をクローズ
        serialRepository.stopSession()
        serialRepository.close()
        nearByRepository.disconnectAll()
    }
}

class SimpleUWBViewModelFactory(
    private val activity: Activity,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SimpleUWBViewModel::class.java)) {
            return SimpleUWBViewModel(activity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
