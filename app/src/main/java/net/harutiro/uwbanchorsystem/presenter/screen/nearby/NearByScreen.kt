package net.harutiro.uwbanchorsystem.presenter.screen.nearby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.harutiro.uwbanchorsystem.feature.nearby.api.ConnectionRequest
import net.harutiro.uwbanchorsystem.feature.nearby.api.DiscoveredDevice

@Composable
fun NearByScreen(
    modifier: Modifier = Modifier,
    viewModel: NearByViewModel = viewModel(factory = NearByViewModelFactory(LocalContext.current as android.app.Activity))
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showConnectionDialog by remember { mutableStateOf<ConnectionRequest?>(null) }
    var messageText by remember { mutableStateOf("") }

    // 接続リクエストがある場合はダイアログを表示
    LaunchedEffect(uiState.connectionRequests) {
        if (uiState.connectionRequests.isNotEmpty()) {
            showConnectionDialog = uiState.connectionRequests.first()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 制御ボタンエリア
        ControlButtonsSection(
            isDiscovering = uiState.isDiscovering,
            onStartDiscovery = { viewModel.startDiscovery() },
            onStopDiscovery = { viewModel.stopDiscovery() }
        )

        // 状態表示
        StatusCard(connectionState = uiState.connectionState)

        // 発見されたデバイスリスト
        if (uiState.discoveredDevices.isNotEmpty()) {
            DiscoveredDevicesSection(devices = uiState.discoveredDevices)
        }

        // メッセージ送信エリア
        if (uiState.receivedMessages.isNotEmpty() || uiState.isDiscovering) {
            MessageSection(
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = { 
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                receivedMessages = uiState.receivedMessages,
                onDisconnectAll = { viewModel.disconnectAll() }
            )
        }
    }

    // 接続承認ダイアログ
    showConnectionDialog?.let { request ->
        ConnectionApprovalDialog(
            request = request,
            onAccept = { 
                viewModel.acceptConnection(request.endpointId)
                showConnectionDialog = null
            },
            onReject = { 
                viewModel.rejectConnection(request.endpointId)
                showConnectionDialog = null
            },
            onDismiss = { showConnectionDialog = null }
        )
    }
}

@Composable
private fun ControlButtonsSection(
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "NearBy Connection 制御",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "※停止ボタンは発見機能のみを停止し、既存の接続は維持されます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartDiscovery,
                    enabled = !isDiscovering,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("発見開始")
                }
                
                Button(
                    onClick = onStopDiscovery,
                    enabled = isDiscovering,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("発見停止")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(connectionState: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "状態: $connectionState",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DiscoveredDevicesSection(devices: List<DiscoveredDevice>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "発見されたデバイス (${devices.size}台)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(devices) { device ->
                    DeviceItem(device = device)
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(device: DiscoveredDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "ID: ${device.endpointId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageSection(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    receivedMessages: List<Pair<String, String>>,
    onDisconnectAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "メッセージ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "※全切断で既存の接続を終了できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onDisconnectAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("全切断")
                }
            }
            
            // メッセージ送信
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    label = { Text("メッセージを入力") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onSendMessage) {
                    Text("送信")
                }
            }
            
            // 受信メッセージ
            if (receivedMessages.isNotEmpty()) {
                Text(
                    text = "受信メッセージ:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(receivedMessages) { (endpointId, message) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = "From: $endpointId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionApprovalDialog(
    request: ConnectionRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("接続リクエスト") },
        text = {
            Column {
                Text("以下の端末から接続リクエストがあります:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "端末名: ${request.connectionInfo.endpointName}",
                    fontWeight = FontWeight.Bold
                )
                Text("認証トークン: ${request.connectionInfo.authenticationDigits}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("接続を許可しますか？")
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("承認")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) {
                Text("拒否")
            }
        }
    )
} 