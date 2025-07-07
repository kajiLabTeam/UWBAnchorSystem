package net.harutiro.uwbanchorsystem.presenter.screen.nearby

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

class NearByViewModel : ViewModel() {
    private var nearByRepository: NearByRepository? = null
    
    private val _uiState = MutableStateFlow(NearByUiState())
    val uiState: StateFlow<NearByUiState> = _uiState.asStateFlow()
    
    fun initializeRepository(activity: Activity) {
        if (nearByRepository == null) {
            nearByRepository = NearByRepository(activity)
            startStateObservation()
        }
    }
    
    private fun startStateObservation() {
        viewModelScope.launch {
            while (true) {
                nearByRepository?.let { repository ->
                    _uiState.value = _uiState.value.copy(
                        connectionState = repository.connectState,
                        discoveredDevices = repository.discoveredDevices,
                        connectionRequests = repository.connectionRequests,
                        receivedMessages = repository.receivedDataList
                    )
                }
                delay(100) // 100msごとに状態を更新
            }
        }
    }
    
    fun startDiscovery() {
        nearByRepository?.startDiscovery()
        _uiState.value = _uiState.value.copy(isDiscovering = true)
    }
    
    fun stopDiscovery() {
        nearByRepository?.stopDiscoveryOnly()
        _uiState.value = _uiState.value.copy(
            isDiscovering = false,
            discoveredDevices = emptyList(),
            connectionRequests = emptyList()
        )
    }
    
    fun resetAll() {
        nearByRepository?.resetAll()
        _uiState.value = _uiState.value.copy(
            isDiscovering = false,
            discoveredDevices = emptyList(),
            connectionRequests = emptyList()
        )
    }
    
    fun acceptConnection(endpointId: String) {
        nearByRepository?.acceptConnection(endpointId)
    }
    
    fun rejectConnection(endpointId: String) {
        nearByRepository?.rejectConnection(endpointId)
    }
    
    fun sendMessage(message: String) {
        nearByRepository?.sendData(message)
    }
    
    fun disconnectAll() {
        nearByRepository?.disconnectAll()
    }
    
    override fun onCleared() {
        super.onCleared()
        nearByRepository?.resetAll()
    }
} 