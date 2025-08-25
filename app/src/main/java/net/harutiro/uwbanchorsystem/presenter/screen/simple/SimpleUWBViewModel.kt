package net.harutiro.uwbanchorsystem.presenter.screen.simple

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository
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
)

class SimpleUWBViewModel(
    private val activity: Activity,
) : ViewModel() {
    private val nearByRepository = NearByRepository.getInstance(activity)
    private val preferencesManager = PreferencesManager.getInstance(activity)

    private val _uiState = MutableStateFlow(SimpleUWBUiState())
    val uiState: StateFlow<SimpleUWBUiState> = _uiState.asStateFlow()

    init {
        setupNearByCallbacks()
        loadSavedDeviceName()
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

    override fun onCleared() {
        super.onCleared()
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
