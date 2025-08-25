package net.harutiro.uwbanchorsystem.presenter.screen.simple

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleUWBScreen(
    modifier: Modifier = Modifier,
    viewModel: SimpleUWBViewModel =
        viewModel(
            factory = SimpleUWBViewModelFactory(LocalContext.current as android.app.Activity),
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deviceName by remember { mutableStateOf("") }
    var showConnectionDialog by remember { mutableStateOf(false) }

    // 保存された端末名を初期値として設定
    LaunchedEffect(uiState.savedDeviceName) {
        if (deviceName.isEmpty() && uiState.savedDeviceName.isNotEmpty()) {
            deviceName = uiState.savedDeviceName
        }
    }

    // 接続リクエストがある場合はダイアログを表示
    LaunchedEffect(uiState.connectionRequest) {
        if (uiState.connectionRequest != null) {
            showConnectionDialog = true
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // アプリタイトル
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "UWB センサー端末",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // 端末名入力
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("端末名を入力") },
                    placeholder = { Text("例: センサー端末A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                )

                // 接続状態表示
                ConnectionStatusCard(uiState = uiState)

                // メイン操作ボタン
                when {
                    uiState.isConnected -> {
                        // 接続済み状態
                        ConnectedActionsSection(
                            connectedDeviceName = uiState.connectedDeviceName,
                            pairedAntenna = uiState.pairedAntenna,
                            onDisconnect = { viewModel.disconnectAll() },
                        )
                    }
                    uiState.isDiscovering -> {
                        // 検索中状態
                        Button(
                            onClick = { viewModel.stopDiscovery() },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "検索停止",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    else -> {
                        // 待機中状態
                        Button(
                            onClick = {
                                if (deviceName.isNotBlank()) {
                                    viewModel.startPairing(deviceName)
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            enabled = deviceName.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "端末登録開始",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                // 受信メッセージ表示（簡易版）
                if (uiState.lastReceivedMessage.isNotEmpty()) {
                    MessageCard(message = uiState.lastReceivedMessage)
                }
            }
        }
    }

    // 接続承認ダイアログ
    if (showConnectionDialog && uiState.connectionRequest != null) {
        ConnectionApprovalDialog(
            deviceName = uiState.connectionRequest!!.connectionInfo.endpointName,
            authToken = uiState.connectionRequest!!.connectionInfo.authenticationDigits,
            onAccept = {
                viewModel.acceptConnection()
                showConnectionDialog = false
            },
            onReject = {
                viewModel.rejectConnection()
                showConnectionDialog = false
            },
            onDismiss = { showConnectionDialog = false },
        )
    }
}

@Composable
private fun ConnectionStatusCard(uiState: SimpleUWBUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        uiState.isConnected -> MaterialTheme.colorScheme.primaryContainer
                        uiState.isDiscovering -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    when {
                        uiState.isConnected -> Icons.Default.CheckCircle
                        uiState.isDiscovering -> Icons.Default.Search
                        else -> Icons.Default.RadioButtonUnchecked
                    },
                contentDescription = null,
                tint =
                    when {
                        uiState.isConnected -> MaterialTheme.colorScheme.primary
                        uiState.isDiscovering -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            Column {
                Text(
                    text =
                        when {
                            uiState.isConnected -> "接続中"
                            uiState.isDiscovering -> "検索中..."
                            else -> "待機中"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConnectedActionsSection(
    connectedDeviceName: String,
    pairedAntenna: String?,
    onDisconnect: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 接続情報表示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "接続中:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = connectedDeviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (pairedAntenna != null) {
                    Text(
                        text = "割り当てアンテナ: $pairedAntenna",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        // 切断ボタン
        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("接続を切断")
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "受信メッセージ:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ConnectionApprovalDialog(
    deviceName: String,
    authToken: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "接続リクエスト",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("以下の端末から接続要求があります:")

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "認証コード: $authToken",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Text(
                    "この接続を許可しますか？",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("許可")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("拒否")
            }
        },
    )
}
