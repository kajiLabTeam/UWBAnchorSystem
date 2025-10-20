package net.harutiro.uwbanchorsystemminio.presenter.router

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.harutiro.uwbanchorsystemminio.R
import net.harutiro.uwbanchorsystemminio.presenter.screen.home.HomeScreen

@Composable
fun Router(
    toBackScreen: () -> Unit,
    showSnackbar: (String, Boolean) -> Unit,
    changeTopBarTitle: (String) -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = BottomNavigationBarRoute.HOME.route,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(BottomNavigationBarRoute.HOME.route) {
            HomeScreen(modifier = Modifier)
            changeTopBarTitle(context.getString(BottomNavigationBarRoute.HOME.title))
        }
    }
}

enum class BottomNavigationBarRoute(val route: String, val title: Int) {
    HOME("home", R.string.home),
}
