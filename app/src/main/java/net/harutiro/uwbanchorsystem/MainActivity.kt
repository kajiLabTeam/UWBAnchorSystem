package net.harutiro.uwbanchorsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository
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
    
    override fun onDestroy() {
        super.onDestroy()
        // アプリ終了時にシングルトンインスタンスを破棄
        NearByRepository.destroyInstance()
    }
}
