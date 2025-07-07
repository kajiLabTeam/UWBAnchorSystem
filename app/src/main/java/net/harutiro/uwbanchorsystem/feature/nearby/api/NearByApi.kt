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


interface NearbyRepositoryCallback {
    fun onConnectionStateChanged(state: String)
    fun onDataReceived(data: String, fromEndpointId: String)
}

class NearByApi(
    private val activity: Activity,
    private val callback: NearbyRepositoryCallback,
    private val nickName: String = "harutiro",
    private val serviceId: String = "net.harutiro.UWBSystem",
    private val strategy: Strategy = Strategy.P2P_STAR
) {
    private val remoteEndpointIds = mutableSetOf<String>()
    private val TAG = "NearbyRepository"

    fun startAdvertise() {
        Nearby.getConnectionsClient(activity)
            .startAdvertising(
                nickName,
                serviceId,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(strategy).build()
            )
            .addOnSuccessListener {
                callback.onConnectionStateChanged("広告開始")
            }
            .addOnFailureListener {
                callback.onConnectionStateChanged("広告失敗")
            }
    }

    fun startDiscovery() {
        Nearby.getConnectionsClient(activity)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(strategy).build()
            )
            .addOnSuccessListener {
                callback.onConnectionStateChanged("発見開始")
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
            Nearby.getConnectionsClient(activity)
                .requestConnection(nickName, endpointId, connectionLifecycleCallback)
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(activity)
                .acceptConnection(endpointId, payloadCallback)
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