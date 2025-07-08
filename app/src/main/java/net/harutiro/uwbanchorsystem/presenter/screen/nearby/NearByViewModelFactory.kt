package net.harutiro.uwbanchorsystem.presenter.screen.nearby

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import net.harutiro.uwbanchorsystem.feature.nearby.repository.NearByRepository

class NearByViewModelFactory(
    private val activity: Activity,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearByViewModel::class.java)) {
            val nearByRepository = NearByRepository.getInstance(activity)
            return NearByViewModel(nearByRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 
