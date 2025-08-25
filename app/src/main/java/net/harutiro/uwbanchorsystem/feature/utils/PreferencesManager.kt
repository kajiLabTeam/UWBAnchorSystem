package net.harutiro.uwbanchorsystem.feature.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager private constructor(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "uwb_anchor_preferences"
        private const val KEY_DEVICE_NAME = "device_name"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var deviceName: String
        get() = preferences.getString(KEY_DEVICE_NAME, "") ?: ""
        set(value) = preferences.edit().putString(KEY_DEVICE_NAME, value).apply()

    fun clearAll() {
        preferences.edit().clear().apply()
    }
}
