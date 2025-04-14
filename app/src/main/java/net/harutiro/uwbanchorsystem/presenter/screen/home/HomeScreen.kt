package net.harutiro.uwbanchorsystem.presenter.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.harutiro.uwbanchorsystem.presenter.components.CustomOutlinedTextField
import net.harutiro.uwbanchorsystem.presenter.components.CustomTopAppBar

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel:HomeViewModel = viewModel(),
) {

    var fileName = viewModel.fileName.collectAsState().value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        CustomOutlinedTextField(
            value = fileName,
            placeholder = "", // hintメッセージ
            label = "センサーデータのファイル名",
            onChange = {
                fileName = it
            },
            isError = fileName.isNotEmpty() && !viewModel.isValidFileName(fileName),
            icon = Icons.Filled.Description,
            errorMessage = "ファイル名に無効な文字が含まれています",
            isPassword = false
        )
    }
}

@Preview(
    showBackground = true,
    device = "id:pixel_8a",
    showSystemUi = true
)
@Composable
fun HomeScreenPreview() {
    Scaffold(
        topBar = {
            CustomTopAppBar("ホームスクリーンプレビュー")
        }
    ) { innerPadding ->
        HomeScreen(modifier = Modifier.padding(innerPadding))
    }
}
