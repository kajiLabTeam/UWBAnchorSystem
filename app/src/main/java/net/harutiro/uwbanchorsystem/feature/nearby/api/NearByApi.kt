package net.harutiro.uwbanchorsystem.feature.nearby.api

import android.app.Activity
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

data class DiscoveredDevice(
    val endpointId: String,
    val name: String,
    val serviceId: String,
)

data class ConnectionRequest(
    val endpointId: String,
    val connectionInfo: ConnectionInfo,
)

interface NearbyRepositoryCallback {
    fun onConnectionStateChanged(state: String)

    fun onDataReceived(
        data: String,
        fromEndpointId: String,
    )

    fun onDeviceDiscovered(device: DiscoveredDevice)

    fun onDeviceLost(endpointId: String)

    fun onConnectionRequested(request: ConnectionRequest)
}

class NearByApi(
    private val activity: Activity,
    private val callback: NearbyRepositoryCallback,
    private val serviceId: String = "net.harutiro.UWBSystem",
    private val strategy: Strategy = Strategy.P2P_STAR,
) {
    private val remoteEndpointIds = mutableSetOf<String>()
    private val pendingConnections = mutableMapOf<String, ConnectionInfo>()
    private val TAG = "NearbyRepository"

    // 動的に変更可能なnickname
    private var currentNickname: String = "default"

    fun updateNickname(nickname: String) {
        currentNickname = nickname.ifEmpty { "未設定" }
    }

    fun startAdvertise(nickname: String) {
        updateNickname(nickname)
        Nearby.getConnectionsClient(activity)
            .startAdvertising(
                currentNickname,
                serviceId,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(strategy).build(),
            )
            .addOnSuccessListener {
                callback.onConnectionStateChanged("広告開始 (端末名: $currentNickname)")
            }
            .addOnFailureListener {
                callback.onConnectionStateChanged("広告失敗")
            }
    }

    fun startDiscovery(nickname: String) {
        updateNickname(nickname)
        Nearby.getConnectionsClient(activity)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(strategy).build(),
            )
            .addOnSuccessListener {
                callback.onConnectionStateChanged("発見開始 (端末名: $currentNickname)")
            }
            .addOnFailureListener {
                callback.onConnectionStateChanged("発見失敗")
            }
    }

    fun stopAdvertising() {
        Nearby.getConnectionsClient(activity).stopAdvertising()
    }

    fun stopDiscovery() {
        Nearby.getConnectionsClient(activity).stopDiscovery()
    }

    // 手動で特定のデバイスに接続リクエストを送信
    fun requestConnection(
        endpointId: String,
        deviceName: String,
    ) {
        Nearby.getConnectionsClient(activity)
            .requestConnection(currentNickname, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                callback.onConnectionStateChanged("接続リクエスト送信: $deviceName (自分: $currentNickname)")
            }
            .addOnFailureListener {
                callback.onConnectionStateChanged("接続リクエスト失敗: $deviceName")
            }
    }

    fun acceptConnection(endpointId: String) {
        Nearby.getConnectionsClient(activity)
            .acceptConnection(endpointId, payloadCallback)
        pendingConnections.remove(endpointId)
        callback.onConnectionStateChanged("接続を承認: $endpointId")
    }

    fun rejectConnection(endpointId: String) {
        Nearby.getConnectionsClient(activity)
            .rejectConnection(endpointId)
        pendingConnections.remove(endpointId)
        callback.onConnectionStateChanged("接続を拒否: $endpointId")
    }

    fun disconnectAll() {
        val client = Nearby.getConnectionsClient(activity)
        remoteEndpointIds.forEach { endpointId ->
            client.disconnectFromEndpoint(endpointId)
        }
        remoteEndpointIds.clear()
        callback.onConnectionStateChanged("全端末と切断")
    }

    fun resetAll() {
        stopAdvertising()
        stopDiscovery()
        disconnectAll()
        pendingConnections.clear()
        callback.onConnectionStateChanged("全リセット")
    }

    fun sendData(text: String) {
        Log.d(TAG, "sendData開始: text=$text")
        Log.d(TAG, "接続端末数: ${remoteEndpointIds.size}")
        Log.d(TAG, "接続端末: ${remoteEndpointIds.joinToString()}")

        if (remoteEndpointIds.isEmpty()) {
            val message = "送信先なし (接続端末がありません)"
            Log.w(TAG, message)
            callback.onConnectionStateChanged(message)
            return
        }

        val data = text.toByteArray()
        val payload = Payload.fromBytes(data)

        var successCount = 0
        var failureCount = 0
        val totalEndpoints = remoteEndpointIds.size

        remoteEndpointIds.forEach { endpointId ->
            Log.d(TAG, "送信開始: endpointId=$endpointId")

            Nearby.getConnectionsClient(activity)
                .sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    successCount++
                    Log.d(TAG, "送信成功: $endpointId ($successCount/$totalEndpoints)")
                    val status = "データ送信成功: $successCount/${totalEndpoints}台"
                    callback.onConnectionStateChanged(status)
                }
                .addOnFailureListener { exception ->
                    failureCount++
                    Log.e(TAG, "送信失敗: $endpointId - ${exception.message}")
                    val status = "データ送信失敗: ${failureCount}台, 成功: ${successCount}台"
                    callback.onConnectionStateChanged(status)
                }
        }

        Log.d(TAG, "sendData完了: 対象端末数=$totalEndpoints")
        callback.onConnectionStateChanged("データ送信実行: ${totalEndpoints}台")
    }

    // ファイル送信機能
    fun sendFile(
        file: java.io.File,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String) -> Unit,
    ) {
        if (remoteEndpointIds.isEmpty()) {
            callback.onConnectionStateChanged("送信先なし")
            onComplete(false, "接続された端末がありません")
            return
        }

        try {
            val payload = Payload.fromFile(file)
            val client = Nearby.getConnectionsClient(activity)

            // ファイル送信前に通知メッセージを送信
            val fileInfo =
                """
                {
                    "type": "FILE_TRANSFER_START",
                    "fileName": "${file.name}",
                    "fileSize": ${file.length()},
                    "timestamp": ${System.currentTimeMillis()}
                }
                """.trimIndent()
            sendData(fileInfo)

            // ファイル送信
            remoteEndpointIds.forEach { endpointId ->
                client.sendPayload(endpointId, payload)
                    .addOnSuccessListener {
                        callback.onConnectionStateChanged("ファイル送信開始: ${file.name}")
                        onProgress(0)
                    }
                    .addOnFailureListener { exception ->
                        callback.onConnectionStateChanged("ファイル送信失敗: ${exception.message}")
                        onComplete(false, "ファイル送信エラー: ${exception.message}")
                    }
            }
        } catch (e: Exception) {
            callback.onConnectionStateChanged("ファイル送信エラー: ${e.message}")
            onComplete(false, "ファイル送信エラー: ${e.message}")
        }
    }

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(
                endpointId: String,
                discoveredEndpointInfo: DiscoveredEndpointInfo,
            ) {
                val device =
                    DiscoveredDevice(
                        endpointId = endpointId,
                        name = discoveredEndpointInfo.endpointName,
                        serviceId = discoveredEndpointInfo.serviceId,
                    )
                callback.onDeviceDiscovered(device)

                // 自動接続を削除 - 手動で接続できるようにする
            }

            override fun onEndpointLost(endpointId: String) {
                callback.onDeviceLost(endpointId)
            }
        }

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo,
            ) {
                // 手動承認のために接続情報を保存
                pendingConnections[endpointId] = connectionInfo
                val request = ConnectionRequest(endpointId, connectionInfo)
                callback.onConnectionRequested(request)
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution,
            ) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        remoteEndpointIds.add(endpointId)
                        callback.onConnectionStateChanged("接続成功: $endpointId")
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
                    ConnectionsStatusCodes.STATUS_ERROR,
                    -> {
                        remoteEndpointIds.remove(endpointId)
                        callback.onConnectionStateChanged("接続失敗: $endpointId")
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                remoteEndpointIds.remove(endpointId)
                callback.onConnectionStateChanged("切断: $endpointId")
            }
        }

    private val payloadCallback =
        object : PayloadCallback() {
            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload,
            ) {
                when (payload.type) {
                    Payload.Type.BYTES -> {
                        val data = payload.asBytes()?.toString(Charsets.UTF_8) ?: ""
                        callback.onDataReceived(data, endpointId)
                    }
                    Payload.Type.FILE -> {
                        // ファイル受信処理
                        payload.asFile()?.let { file ->
                            callback.onConnectionStateChanged("ファイル受信開始: ${file.asUri()}")
                        }
                    }
                    else -> {
                        // 他のタイプは現在未対応
                    }
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate,
            ) {
                when (update.status) {
                    PayloadTransferUpdate.Status.IN_PROGRESS -> {
                        val progress = (update.bytesTransferred * 100 / update.totalBytes).toInt()
                        callback.onConnectionStateChanged("転送中: $progress%")
                    }
                    PayloadTransferUpdate.Status.SUCCESS -> {
                        callback.onConnectionStateChanged("転送完了: $endpointId")
                    }
                    PayloadTransferUpdate.Status.FAILURE -> {
                        callback.onConnectionStateChanged("転送失敗: $endpointId")
                    }
                    PayloadTransferUpdate.Status.CANCELED -> {
                        callback.onConnectionStateChanged("転送キャンセル: $endpointId")
                    }
                }
            }
        }

    // 接続状態チェック機能
    fun hasActiveConnections(): Boolean {
        return remoteEndpointIds.isNotEmpty()
    }

    fun getConnectedEndpointsCount(): Int {
        return remoteEndpointIds.size
    }

    fun getConnectedEndpoints(): Set<String> {
        return remoteEndpointIds.toSet()
    }

    fun getDetailedConnectionStatus(): String {
        return """
            接続端末数: ${remoteEndpointIds.size}
            接続中端末: ${remoteEndpointIds.joinToString()}
            保留中接続: ${pendingConnections.size}
            現在のニックネーム: $currentNickname
            サービスID: $serviceId
            """.trimIndent()
    }

    // テスト用のPing送信機能
    fun sendPing() {
        val pingData =
            """
            {
                "type": "PING",
                "timestamp": ${System.currentTimeMillis()},
                "from": "$currentNickname"
            }
            """.trimIndent()

        Log.d(TAG, "Ping送信: $pingData")
        sendData(pingData)
    }
}
