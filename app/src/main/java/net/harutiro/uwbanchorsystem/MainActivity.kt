package net.harutiro.uwbanchorsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.harutiro.uwbanchorsystem.presenter.MainScreen
import net.harutiro.uwbanchorsystem.presenter.theme.UWBAnchorSystemTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UWBAnchorSystemTheme {
                MainScreen()
            }
        }
    }
}
