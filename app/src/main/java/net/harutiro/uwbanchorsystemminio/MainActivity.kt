package net.harutiro.uwbanchorsystemminio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.harutiro.uwbanchorsystemminio.presenter.MainScreen
import net.harutiro.uwbanchorsystemminio.presenter.theme.UWBAnchorSystemTheme

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
