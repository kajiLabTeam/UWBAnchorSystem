package net.harutiro.uwbanchorsystem.presenter.screen.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel: ViewModel() {

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }
}