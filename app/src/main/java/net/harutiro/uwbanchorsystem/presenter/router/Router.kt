package net.harutiro.uwbanchorsystem.presenter.router

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import net.harutiro.uwbanchorsystem.R

@Composable
fun Router(
    toBackScreen: () -> Unit,
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
            changeTopBarTitle(context.getString(BottomNavigationBarRoute.HOME.title))
        }
    }
}

enum class BottomNavigationBarRoute(val route: String, val title: Int) {
    HOME("search", R.string.home),
}