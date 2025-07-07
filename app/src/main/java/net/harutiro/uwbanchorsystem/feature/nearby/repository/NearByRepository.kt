package net.harutiro.uwbanchorsystem.feature.nearby.repository

import android.app.Activity
import android.util.Log
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

    fun startAdvertise() = nearByApi.startAdvertise()
    fun startDiscovery() = nearByApi.startDiscovery()
    fun sendData(text: String) = nearByApi.sendData(text)
    fun disconnectAll() = nearByApi.disconnectAll()
    fun resetAll() {
        nearByApi.resetAll()
        receivedDataList = emptyList()
    }

    override fun onConnectionStateChanged(state: String) {
        Log.d("NearByRepository", state)
        connectState = state
    }
    override fun onDataReceived(data: String, fromEndpointId: String) {
        Log.d("NearByRepository", "onDataReceived: $data")
        receivedDataList = receivedDataList + (fromEndpointId to data)
    }
}