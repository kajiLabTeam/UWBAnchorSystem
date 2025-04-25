package net.harutiro.uwbanchorsystem.presenter.screen.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.harutiro.uwbanchorsystem.feature.serial.repository.SerialRepository
import net.harutiro.uwbanchorsystem.presenter.components.CustomOutlinedTextField
import net.harutiro.uwbanchorsystem.presenter.components.CustomTopAppBar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    var fileName by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
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
                val serialRepository = SerialRepository()
                serialRepository.connectDevice(context)
                serialRepository.startSession(
                    onLineRead = { line ->
                        if(line == null) return@startSession
                        resultMessage = """
                            nlos: ${line.nLos}
                            distance: ${line.distance}
                            elevation: ${line.elevation}
                            azimuth: ${line.azimuth}
                            elevationFom: ${line.elevationFom}
                            rssi: ${line.rssi}
                            pDoA1: ${line.pDoA1}
                            pDoA2: ${line.pDoA2}
                            seqCount: ${line.seqCount}
                        """.trimIndent()
                        Log.d("Main",line.toString())
                    },
                    onError = {

                    }
                )
            }
        ){
            Text(text = "センサーデータの変換")
        }
        Text("hello\n"+resultMessage)

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
        HomeScreen(modifier = Modifier.padding(innerPadding))
    }
}
