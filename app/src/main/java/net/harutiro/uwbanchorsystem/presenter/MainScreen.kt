package net.harutiro.uwbanchorsystem.presenter

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.harutiro.uwbanchorsystem.R
import net.harutiro.uwbanchorsystem.presenter.components.CustomTopAppBar
import net.harutiro.uwbanchorsystem.presenter.router.Router

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val appName = context.getString(R.string.app_name)

    val navController = rememberNavController()
    var topBarTitle by remember {
        mutableStateOf(appName)
    }

    val hostState = remember { SnackbarHostState() }
    var isErrorMessage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CustomTopAppBar(
                topBarTitle = topBarTitle,
            )
        },
        snackbarHost = { CustomSnackbarHost(hostState = hostState, isErrorMessage = isErrorMessage) },
    ) { innerPadding ->
        Router(
            toBackScreen = {
                navController.popBackStack()
            },
            changeTopBarTitle = {
                topBarTitle = it
            },
            showSnackbar = { message, isError ->
                scope.launch {
                    isErrorMessage = isError
                    hostState.showSnackbar(message)
                }
            },
            navController = navController,
            modifier =
                Modifier
                    .padding(innerPadding),
        )
    }
}

@Composable
fun CustomSnackbarHost(
    hostState: SnackbarHostState,
    isErrorMessage: Boolean,
) {
    SnackbarHost(
        hostState = hostState,
    ) { snackbarData ->
        Snackbar(
            snackbarData = snackbarData,
            containerColor = if (isErrorMessage) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isErrorMessage) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
