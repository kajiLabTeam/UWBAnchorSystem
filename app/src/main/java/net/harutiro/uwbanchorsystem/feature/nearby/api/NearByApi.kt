package net.harutiro.uwbanchorsystem.feature.nearby.api

import android.app.Activity
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
    val serviceId: String
)

data class ConnectionRequest(
    val endpointId: String,
    val connectionInfo: ConnectionInfo
)

interface NearbyRepositoryCallback {
    fun onConnectionStateChanged(state: String)
    fun onDataReceived(data: String, fromEndpointId: String)
    fun onDeviceDiscovered(device: DiscoveredDevice)
    fun onDeviceLost(endpointId: String)
    fun onConnectionRequested(request: ConnectionRequest)
}

class NearByApi(
    private val activity: Activity,
    private val callback: NearbyRepositoryCallback,
    private val serviceId: String = "net.harutiro.UWBSystem",
    private val strategy: Strategy = Strategy.P2P_STAR
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
                AdvertisingOptions.Builder().setStrategy(strategy).build()
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
                DiscoveryOptions.Builder().setStrategy(strategy).build()
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
    fun requestConnection(endpointId: String, deviceName: String) {
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
        val data = text.toByteArray()
        val payload = Payload.fromBytes(data)
        if (remoteEndpointIds.isEmpty()) {
            callback.onConnectionStateChanged("送信先なし")
            return
        }
        remoteEndpointIds.forEach {
            Nearby.getConnectionsClient(activity)
                .sendPayload(it, payload)
        }
        callback.onConnectionStateChanged("データ送信: ${remoteEndpointIds.size}台")
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            val device = DiscoveredDevice(
                endpointId = endpointId,
                name = discoveredEndpointInfo.endpointName,
                serviceId = discoveredEndpointInfo.serviceId
            )
            callback.onDeviceDiscovered(device)
            
            // 自動接続を削除 - 手動で接続できるようにする
        }
        
        override fun onEndpointLost(endpointId: String) {
            callback.onDeviceLost(endpointId)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // 手動承認のために接続情報を保存
            pendingConnections[endpointId] = connectionInfo
            val request = ConnectionRequest(endpointId, connectionInfo)
            callback.onConnectionRequested(request)
        }
        
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    remoteEndpointIds.add(endpointId)
                    callback.onConnectionStateChanged("接続成功: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
                ConnectionsStatusCodes.STATUS_ERROR -> {
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

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()?.toString(Charsets.UTF_8) ?: ""
                callback.onDataReceived(data, endpointId)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}