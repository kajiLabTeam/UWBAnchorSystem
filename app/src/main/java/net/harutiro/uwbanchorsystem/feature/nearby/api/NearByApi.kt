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

    // 接続成功・切断の通知を追加
    fun onDeviceConnected(
        endpointId: String,
        deviceName: String,
    )

    fun onDeviceDisconnected(endpointId: String)
}

class NearByApi(
    private val activity: Activity,
    private val callback: NearbyRepositoryCallback,
    private val serviceId: String = "net.harutiro.UWBSystem",
    private val strategy: Strategy = Strategy.P2P_STAR,
) {
    private val remoteEndpointIds = mutableSetOf<String>()
    private val pendingConnections = mutableMapOf<String, ConnectionInfo>()
    private val pendingConnectionTimestamps = mutableMapOf<String, Long>()
    private val TAG = "NearbyRepository"

    // 動的に変更可能なnickname
    private var currentNickname: String = "default"

    // Discovery状態管理
    private var isDiscovering = false

    // 接続タイムアウト時間（ミリ秒）
    private val CONNECTION_TIMEOUT_MS = 60000L // 60秒

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

        // 既にDiscovery中の場合は何もしない
        if (isDiscovering) {
            callback.onConnectionStateChanged("既に検索中です")
            return
        }

        Nearby.getConnectionsClient(activity)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(strategy).build(),
            )
            .addOnSuccessListener {
                isDiscovering = true
                callback.onConnectionStateChanged("発見開始 (端末名: $currentNickname)")
            }
            .addOnFailureListener {
                isDiscovering = false
                callback.onConnectionStateChanged("発見失敗")
            }
    }

    fun stopAdvertising() {
        Nearby.getConnectionsClient(activity).stopAdvertising()
    }

    fun stopDiscovery() {
        Nearby.getConnectionsClient(activity).stopDiscovery()
        isDiscovering = false
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
        pendingConnectionTimestamps.remove(endpointId)
        callback.onConnectionStateChanged("接続を承認: $endpointId")
    }

    fun rejectConnection(endpointId: String) {
        Nearby.getConnectionsClient(activity)
            .rejectConnection(endpointId)
        pendingConnections.remove(endpointId)
        pendingConnectionTimestamps.remove(endpointId)
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
        pendingConnectionTimestamps.clear()
        isDiscovering = false
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
    private var fileTransferCallback: ((Int) -> Unit)? = null
    private var fileCompleteCallback: ((Boolean, String) -> Unit)? = null

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
            fileTransferCallback = onProgress
            fileCompleteCallback = onComplete

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
                        onProgress(1) // 送信開始時は1%に設定
                        Log.d(TAG, "ファイル送信開始: ${file.name}")
                    }
                    .addOnFailureListener { exception ->
                        callback.onConnectionStateChanged("ファイル送信失敗: ${exception.message}")
                        onComplete(false, "ファイル送信エラー: ${exception.message}")
                        Log.e(TAG, "ファイル送信失敗: ${exception.message}")
                    }
            }
        } catch (e: Exception) {
            callback.onConnectionStateChanged("ファイル送信エラー: ${e.message}")
            onComplete(false, "ファイル送信エラー: ${e.message}")
            Log.e(TAG, "ファイル送信エラー: ${e.message}")
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
                pendingConnectionTimestamps[endpointId] = System.currentTimeMillis()
                val request = ConnectionRequest(endpointId, connectionInfo)
                callback.onConnectionRequested(request)

                // タイムアウト処理を開始
                startConnectionTimeout(endpointId)
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution,
            ) {
                when (result.status.statusCode) {
                    ConnectionsStatusCodes.STATUS_OK -> {
                        remoteEndpointIds.add(endpointId)
                        // 端末名を取得（保存されている場合）
                        val deviceName = pendingConnections[endpointId]?.endpointName ?: "不明な端末"
                        callback.onConnectionStateChanged("接続成功: $endpointId")
                        callback.onDeviceConnected(endpointId, deviceName)
                        // 接続完了したので保留中接続情報を削除
                        pendingConnections.remove(endpointId)
                    }
                    ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
                    ConnectionsStatusCodes.STATUS_ERROR,
                    -> {
                        remoteEndpointIds.remove(endpointId)
                        callback.onConnectionStateChanged("接続失敗: $endpointId")
                        pendingConnections.remove(endpointId)
                        pendingConnectionTimestamps.remove(endpointId)
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                remoteEndpointIds.remove(endpointId)
                callback.onConnectionStateChanged("切断: $endpointId")
                callback.onDeviceDisconnected(endpointId)
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
                        Log.d(TAG, "ファイル転送進捗: $progress% ($endpointId)")
                        callback.onConnectionStateChanged("転送中: $progress%")
                        // HomeViewModelのプログレスコールバックを呼び出し
                        fileTransferCallback?.invoke(progress)
                    }
                    PayloadTransferUpdate.Status.SUCCESS -> {
                        Log.d(TAG, "ファイル転送完了: $endpointId")
                        callback.onConnectionStateChanged("転送完了: $endpointId")
                        // 完了コールバックを呼び出し
                        fileCompleteCallback?.invoke(true, "ファイル送信完了")
                        // コールバックをクリア
                        fileTransferCallback = null
                        fileCompleteCallback = null
                    }
                    PayloadTransferUpdate.Status.FAILURE -> {
                        Log.e(TAG, "ファイル転送失敗: $endpointId")
                        callback.onConnectionStateChanged("転送失敗: $endpointId")
                        // 失敗コールバックを呼び出し
                        fileCompleteCallback?.invoke(false, "ファイル送信失敗")
                        // コールバックをクリア
                        fileTransferCallback = null
                        fileCompleteCallback = null
                    }
                    PayloadTransferUpdate.Status.CANCELED -> {
                        Log.w(TAG, "ファイル転送キャンセル: $endpointId")
                        callback.onConnectionStateChanged("転送キャンセル: $endpointId")
                        // キャンセルコールバックを呼び出し
                        fileCompleteCallback?.invoke(false, "ファイル送信キャンセル")
                        // コールバックをクリア
                        fileTransferCallback = null
                        fileCompleteCallback = null
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
            Discovery状態: ${if (isDiscovering) "検索中" else "停止中"}
            """.trimIndent()
    }

    // 接続タイムアウト処理
    private fun startConnectionTimeout(endpointId: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 接続がまだ保留中の場合はタイムアウト処理を実行
            if (pendingConnections.containsKey(endpointId)) {
                val timestamp = pendingConnectionTimestamps[endpointId] ?: 0
                val elapsed = System.currentTimeMillis() - timestamp

                if (elapsed >= CONNECTION_TIMEOUT_MS) {
                    Log.w(TAG, "接続タイムアウト: $endpointId (経過時間: ${elapsed}ms)")
                    // 接続を拒否
                    Nearby.getConnectionsClient(activity).rejectConnection(endpointId)
                    pendingConnections.remove(endpointId)
                    pendingConnectionTimestamps.remove(endpointId)
                    callback.onConnectionStateChanged("接続タイムアウト: $endpointId")
                }
            }
        }, CONNECTION_TIMEOUT_MS)
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
