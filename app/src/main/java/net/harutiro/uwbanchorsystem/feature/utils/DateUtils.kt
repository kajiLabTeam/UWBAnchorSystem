package net.harutiro.uwbanchorsystem.feature.utils

import android.icu.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateUtils {
    companion object {
        fun getNowDate(): String {
            val df = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.JAPAN)
            val date = Date(System.currentTimeMillis())
            return df.format(date)
        }

        fun stringToDate(dateString: String): Long {
            val df = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.JAPAN)
            val date = df.parse(dateString)

            return date.time
        }

        fun getTimeStamp(): Long {
            return System.currentTimeMillis()
        }
    }
}
