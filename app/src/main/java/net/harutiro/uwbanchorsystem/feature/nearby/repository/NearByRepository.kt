package net.harutiro.uwbanchorsystem.feature.nearby.repository

import android.app.Activity
import android.util.Log
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.api.DiscoveredDevice
import net.harutiro.uwbanchorsystem.feature.nearby.api.NearByApi
import net.harutiro.uwbanchorsystem.feature.nearby.api.NearbyRepositoryCallback
import net.harutiro.uwbanchorsystem.feature.utils.PreferencesManager

// 簡易版用のデータクラス
data class ConnectedDevice(
    val endpointId: String,
    val deviceName: String,
)

data class Message(
    val content: String,
    val fromEndpointId: String,
)

// センシング制御コマンドのコールバック
interface SensingControlCallback {
    fun onStartSensingCommand(fileName: String)

    fun onStopSensingCommand()
}

class NearByRepository private constructor(
    val activity: Activity,
) : NearbyRepositoryCallback {
    companion object {
        @Volatile
        private var INSTANCE: NearByRepository? = null

        fun getInstance(activity: Activity): NearByRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NearByRepository(activity).also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.nearByApi?.resetAll()
            INSTANCE = null
        }
    }

    private val nearByApi: NearByApi = NearByApi(activity, this)
    private val preferencesManager = PreferencesManager.getInstance(activity)

    var connectState: String = ""
        private set
    var receivedDataList: List<Pair<String, String>> = emptyList()
        private set
    var discoveredDevices: List<DiscoveredDevice> = emptyList()
        private set
    var connectionRequests: List<ConnectionRequest> = emptyList()
        private set

    // センシング制御コールバック
    var sensingControlCallback: SensingControlCallback? = null

    // 各種コールバック
    private var onConnectionStateChangedListener: ((String) -> Unit)? = null
    private var onConnectionRequestReceivedListener: ((ConnectionRequest) -> Unit)? = null
    private var onDeviceConnectedListener: ((ConnectedDevice) -> Unit)? = null
    private var onDeviceDisconnectedListener: ((String) -> Unit)? = null
    private var onMessageReceivedListener: ((Message) -> Unit)? = null

    // 現在の端末名を取得
    private fun getCurrentDeviceName(): String {
        return preferencesManager.deviceName.ifEmpty { "未設定の端末" }
    }

    fun startAdvertise() {
        val deviceName = getCurrentDeviceName()
        nearByApi.startAdvertise(deviceName)
    }

    fun startDiscovery() {
        val deviceName = getCurrentDeviceName()
        nearByApi.startDiscovery(deviceName)
    }

    fun sendData(text: String) {
        Log.d("NearByRepository", "=== sendData開始 ===")
        Log.d("NearByRepository", "データ種別: ${getDataType(text)}")
        Log.d("NearByRepository", "データ長: ${text.length} bytes")
        Log.d("NearByRepository", "データ内容（先頭100文字）: ${text.take(100)}")

        val result = nearByApi.sendData(text)

        Log.d("NearByRepository", "=== sendData終了 ===")
        return result
    }

    // データ種別を判定
    private fun getDataType(text: String): String {
        return when {
            text.contains("\"type\":\"REALTIME_DATA\"") -> "リアルタイムデータ"
            text.contains("\"type\":\"PING\"") -> "Ping"
            text.contains("\"type\":\"FILE_TRANSFER_START\"") -> "ファイル転送開始"
            text.startsWith("SENSING_START:") -> "センシング開始コマンド"
            text == "SENSING_STOP" -> "センシング終了コマンド"
            else -> "その他（${text.take(20)}...）"
        }
    }

    fun disconnectAll() = nearByApi.disconnectAll()

    fun acceptConnection(endpointId: String) = nearByApi.acceptConnection(endpointId)

    fun rejectConnection(endpointId: String) = nearByApi.rejectConnection(endpointId)

    // ファイル送信機能を追加
    fun sendFile(
        file: java.io.File,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String) -> Unit,
    ) {
        nearByApi.sendFile(file, onProgress, onComplete)
    }

    // 手動で特定のデバイスに接続リクエストを送信
    fun requestConnection(
        endpointId: String,
        deviceName: String,
    ) = nearByApi.requestConnection(endpointId, deviceName)

    // 発見のみを停止（接続は維持）
    fun stopDiscoveryOnly() {
        nearByApi.stopDiscovery()
        // 発見されたデバイスリストと接続リクエストのみクリア
        discoveredDevices = emptyList()
        connectionRequests = emptyList()
        connectState = "発見停止（接続は維持）"
    }

    // 全てをリセット（接続も切断）
    fun resetAll() {
        nearByApi.resetAll()
        receivedDataList = emptyList()
        discoveredDevices = emptyList()
        connectionRequests = emptyList()
    }

    override fun onDeviceDiscovered(device: DiscoveredDevice) {
        Log.d("NearByRepository", "onDeviceDiscovered: ${device.name}")
        discoveredDevices = discoveredDevices.filter { it.endpointId != device.endpointId } + device
    }

    override fun onDeviceLost(endpointId: String) {
        Log.d("NearByRepository", "onDeviceLost: $endpointId")
        discoveredDevices = discoveredDevices.filter { it.endpointId != endpointId }
    }

    // 接続状態をチェック
    fun hasActiveConnections(): Boolean {
        return nearByApi.hasActiveConnections()
    }

    fun getConnectedEndpointsCount(): Int {
        return nearByApi.getConnectedEndpointsCount()
    }

    fun getDetailedConnectionStatus(): String {
        return nearByApi.getDetailedConnectionStatus()
    }

    fun sendPing() {
        nearByApi.sendPing()
    }

    // SimpleUWBViewModel用のコールバック設定メソッド
    fun setOnConnectionStateChangedListener(listener: (String) -> Unit) {
        onConnectionStateChangedListener = listener
    }

    fun setOnConnectionRequestReceivedListener(listener: (ConnectionRequest) -> Unit) {
        onConnectionRequestReceivedListener = listener
    }

    fun setOnDeviceConnectedListener(listener: (ConnectedDevice) -> Unit) {
        onDeviceConnectedListener = listener
    }

    fun setOnDeviceDisconnectedListener(listener: (String) -> Unit) {
        onDeviceDisconnectedListener = listener
    }

    fun setOnMessageReceivedListener(listener: (Message) -> Unit) {
        onMessageReceivedListener = listener
    }

    // Advertising開始（端末名指定版）
    fun startAdvertising(deviceName: String) {
        // 端末名を設定
        preferencesManager.deviceName = deviceName
        // Advertising開始
        nearByApi.startAdvertise(deviceName)
    }

    // Advertising停止
    fun stopAdvertising() {
        nearByApi.stopAdvertising()
    }

    // コールバック呼び出しの更新
    override fun onConnectionStateChanged(state: String) {
        Log.d("NearByRepository", state)
        connectState = state
        onConnectionStateChangedListener?.invoke(state)
    }

    override fun onConnectionRequested(request: ConnectionRequest) {
        Log.d("NearByRepository", "onConnectionRequested: ${request.connectionInfo.endpointName}")
        connectionRequests = connectionRequests.filter { it.endpointId != request.endpointId } + request
        onConnectionRequestReceivedListener?.invoke(request)
    }

    override fun onDataReceived(
        data: String,
        fromEndpointId: String,
    ) {
        Log.d("NearByRepository", "onDataReceived: $data")
        receivedDataList = receivedDataList + (fromEndpointId to data)

        // MessageReceivedListenerに通知
        onMessageReceivedListener?.invoke(Message(data, fromEndpointId))

        // センシング制御コマンドの処理
        when {
            data.startsWith("SENSING_START:") -> {
                val fileName = data.removePrefix("SENSING_START:")
                Log.d("NearByRepository", "センシング開始コマンド受信: fileName=$fileName")
                sensingControlCallback?.onStartSensingCommand(fileName)
            }
            data == "SENSING_STOP" -> {
                Log.d("NearByRepository", "センシング終了コマンド受信")
                sensingControlCallback?.onStopSensingCommand()
            }
        }
    }

    override fun onDeviceConnected(
        endpointId: String,
        deviceName: String,
    ) {
        Log.d("NearByRepository", "onDeviceConnected: $deviceName ($endpointId)")
        // SimpleUWBViewModel用のコールバックに通知
        onDeviceConnectedListener?.invoke(ConnectedDevice(endpointId, deviceName))
    }

    override fun onDeviceDisconnected(endpointId: String) {
        Log.d("NearByRepository", "onDeviceDisconnected: $endpointId")
        // SimpleUWBViewModel用のコールバックに通知
        onDeviceDisconnectedListener?.invoke(endpointId)
    }
}
