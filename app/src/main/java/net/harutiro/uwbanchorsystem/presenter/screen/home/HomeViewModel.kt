package net.harutiro.uwbanchorsystem.presenter.screen.home

import androidx.lifecycle.ViewModel

class HomeViewModel: ViewModel() {
    fun isValidFileName(fileName: String): Boolean {
        return fileName.matches(Regex("[\\w\\-. ]+"))
    }
}