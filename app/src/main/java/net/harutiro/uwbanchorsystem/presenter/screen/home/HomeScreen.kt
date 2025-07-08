package net.harutiro.uwbanchorsystem.presenter.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.harutiro.uwbanchorsystem.presenter.components.CustomOutlinedTextField
import net.harutiro.uwbanchorsystem.presenter.components.CustomTopAppBar
import net.harutiro.uwbanchorsystem.presenter.router.BottomNavigationBarRoute

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    navController: NavHostController? = null,
) {
    var fileName by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.connectDevice(context)
        viewModel.initializeDeviceName(context)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 端末名入力フィールド
        CustomOutlinedTextField(
            value = viewModel.deviceName,
            placeholder = "端末名を入力してください",
            label = "端末名",
            onChange = { newDeviceName ->
                viewModel.updateDeviceName(context, newDeviceName)
            },
            isError = viewModel.deviceName.isNotEmpty() && !viewModel.isValidDeviceName(viewModel.deviceName),
            icon = Icons.Filled.Person,
            errorMessage = "端末名は1〜20文字で、英数字・ハイフン・ドット・スペースのみ使用可能です",
            isPassword = false,
        )

        CustomOutlinedTextField(
            value = fileName,
            // hintメッセージ
            placeholder = "",
            label = "センサーデータのファイル名",
            onChange = {
                fileName = it
            },
            isError = fileName.isNotEmpty() && !viewModel.isValidFileName(fileName),
            icon = Icons.Filled.Description,
            errorMessage = "ファイル名に無効な文字が含まれています",
            isPassword = false,
        )

        Button(
            onClick = {
                viewModel.startSensing(context, fileName)
            },
        ) {
            Text(text = "センシング開始")
        }

        Button(
            onClick = {
                viewModel.stopSensing(context)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "センシング終了（自動でMacに送信）")
        }

        Text(
            text = "※センシング終了時、接続されたMacがあれば自動送信されます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // センシング状態表示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = if (viewModel.isSensing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = "センシング状態",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (viewModel.isSensing) "実行中" else "停止中",
                    color = if (viewModel.isSensing) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (viewModel.sensingStatus.isNotEmpty()) {
                    Text(
                        text = viewModel.sensingStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Mac接続状態とデバッグ情報
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    text = "Mac接続状態",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = viewModel.connectionStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "送信データ数: ${viewModel.dataTransmissionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NearBy接続テストボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.startNearByAdvertising() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "広告開始", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.startNearByDiscovery() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = "Mac検索", fontSize = 12.sp)
                    }
                }

                // Ping送信テストボタン
                Button(
                    onClick = { viewModel.sendPingTest() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Ping送信テスト", fontSize = 12.sp)
                }

                // 詳細状態表示（クリックで切り替え）
                var showDetails by remember { mutableStateOf(false) }

                Button(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = if (showDetails) "詳細を隠す" else "詳細を表示", fontSize = 12.sp)
                }

                if (showDetails) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = "基本状態:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = viewModel.getDetailedConnectionStatus(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        Text(
                            text = "NearBy詳細状態:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = viewModel.getDetailedNearByStatus(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // NearBy Connection画面への移動ボタン
        Button(
            onClick = {
                navController?.navigate(BottomNavigationBarRoute.NEARBY.route)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "NearBy Connection")
        }

        Text(viewModel.resultMessage)
    }
}

@Preview(
    showBackground = true,
    device = "id:pixel_8a",
    showSystemUi = true,
)
@Composable
fun HomeScreenPreview() {
    Scaffold(
        topBar = {
            CustomTopAppBar("ホームスクリーンプレビュー")
        },
    ) { innerPadding ->
        HomeScreen(
            modifier = Modifier.padding(innerPadding),
            navController = rememberNavController(),
        )
    }
}
