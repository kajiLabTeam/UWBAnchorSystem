package net.harutiro.uwbanchorsystem.feature.nearby.repository

import android.app.Activity
import android.util.Log
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.api.DiscoveredDevice
import net.harutiro.uwbanchorsystem.feature.nearby.api.NearByApi
import net.harutiro.uwbanchorsystem.feature.nearby.api.NearbyRepositoryCallback

class NearByRepository: NearbyRepositoryCallback {
    private val nearByApi : NearByApi

    constructor(
        activity: Activity
    ){
        this.nearByApi = NearByApi(activity, this)
    }

    var connectState: String = ""
        private set
    var receivedDataList: List<Pair<String, String>> = emptyList()
        private set
    var discoveredDevices: List<DiscoveredDevice> = emptyList()
        private set
    var connectionRequests: List<ConnectionRequest> = emptyList()
        private set

    fun startAdvertise() = nearByApi.startAdvertise()
    fun startDiscovery() = nearByApi.startDiscovery()
    fun sendData(text: String) = nearByApi.sendData(text)
    fun disconnectAll() = nearByApi.disconnectAll()
    fun acceptConnection(endpointId: String) = nearByApi.acceptConnection(endpointId)
    fun rejectConnection(endpointId: String) = nearByApi.rejectConnection(endpointId)
    
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

    override fun onConnectionStateChanged(state: String) {
        Log.d("NearByRepository", state)
        connectState = state
    }
    
    override fun onDataReceived(data: String, fromEndpointId: String) {
        Log.d("NearByRepository", "onDataReceived: $data")
        receivedDataList = receivedDataList + (fromEndpointId to data)
    }
    
    override fun onDeviceDiscovered(device: DiscoveredDevice) {
        Log.d("NearByRepository", "onDeviceDiscovered: ${device.name}")
        discoveredDevices = discoveredDevices.filter { it.endpointId != device.endpointId } + device
    }
    
    override fun onDeviceLost(endpointId: String) {
        Log.d("NearByRepository", "onDeviceLost: $endpointId")
        discoveredDevices = discoveredDevices.filter { it.endpointId != endpointId }
    }
    
    override fun onConnectionRequested(request: ConnectionRequest) {
        Log.d("NearByRepository", "onConnectionRequested: ${request.connectionInfo.endpointName}")
        connectionRequests = connectionRequests.filter { it.endpointId != request.endpointId } + request
    }
}