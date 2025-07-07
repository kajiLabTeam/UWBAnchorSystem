package net.harutiro.uwbanchorsystem.presenter.screen.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.api.DiscoveredDevice
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository

data class NearByUiState(
    val isDiscovering: Boolean = false,
    val connectionState: String = "",
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val connectionRequests: List<ConnectionRequest> = emptyList(),
    val connectedDevices: List<String> = emptyList(),
    val receivedMessages: List<Pair<String, String>> = emptyList()
)

class NearByViewModel(private val nearByRepository: NearByRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NearByUiState())
    val uiState: StateFlow<NearByUiState> = _uiState.asStateFlow()
    
    init {
        startStateObservation()
    }
    
    private fun startStateObservation() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    connectionState = nearByRepository.connectState,
                    discoveredDevices = nearByRepository.discoveredDevices,
                    connectionRequests = nearByRepository.connectionRequests,
                    receivedMessages = nearByRepository.receivedDataList
                )
                delay(100) // 100msごとに状態を更新
            }
        }
    }
    
    fun startDiscovery() {
        nearByRepository.startDiscovery()
        _uiState.value = _uiState.value.copy(isDiscovering = true)
    }
    
    fun stopDiscovery() {
        nearByRepository.stopDiscoveryOnly() // 接続を維持したまま発見のみ停止
        _uiState.value = _uiState.value.copy(
            isDiscovering = false,
            discoveredDevices = emptyList(),
            connectionRequests = emptyList()
        )
    }
    
    fun resetAll() {
        nearByRepository.resetAll() // 全てをリセット（接続も切断）
        _uiState.value = _uiState.value.copy(
            isDiscovering = false,
            discoveredDevices = emptyList(),
            connectionRequests = emptyList()
        )
    }
    
    fun requestConnection(endpointId: String, deviceName: String) {
        nearByRepository.requestConnection(endpointId, deviceName)
    }
    
    fun acceptConnection(endpointId: String) {
        nearByRepository.acceptConnection(endpointId)
    }
    
    fun rejectConnection(endpointId: String) {
        nearByRepository.rejectConnection(endpointId)
    }
    
    fun sendMessage(message: String) {
        nearByRepository.sendData(message)
    }
    
    fun disconnectAll() {
        nearByRepository.disconnectAll()
    }
    
    // 画面が破棄されても接続は維持（MainActivityで管理）
    override fun onCleared() {
        super.onCleared()
        // 発見のみを停止し、接続は維持
        nearByRepository.stopDiscoveryOnly()
    }
} 