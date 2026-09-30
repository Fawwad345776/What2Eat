package com.aura.what2eat.service

import android.os.Build
import android.util.Log
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Date
import kotlin.math.floor

object IslamicCalendarService {

    private const val TAG = "IslamicCalendarService"

    // Hijri Month Constants
    const val HIJRI_RAMADAN = 9
    const val HIJRI_SHAWWAL = 10
    const val HIJRI_DHUL_HIJJAH = 12

    /**
     * Determines the current Hijri (Islamic) month (1-12) and day of month (1-30).
     */
    fun getHijriMonthAndDay(date: Date = Date()): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijrahDate = HijrahDate.now()
                val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
                val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
                Pair(month, day)
            } catch (e: Exception) {
                Log.w(TAG, "HijrahDate failed, falling back to algorithmic calculation: ${e.message}")
                calculateHijriAlgorithmic(date)
            }
        } else {
            calculateHijriAlgorithmic(date)
        }
    }

    /**
     * Algorithmic calculation of Hijri date from Gregorian date.
     * Returns Pair(month, day).
     */
    private fun calculateHijriAlgorithmic(date: Date): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { time = date }
        var year = cal.get(Calendar.YEAR)
        var month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        if (month < 3) {
            year -= 1
            month += 12
        }

        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + b - 1524.5

        val epoch = 1948439.5
        val z = jd - epoch
        val cyc = floor(z / 10631.0)
        val j = z - 10631.0 * cyc
        val jYear = floor((j - 0.5) / 354.366)
        val remainder = j - floor(jYear * 354.366 + 0.5)

        val hMonth = (floor((remainder + 28.5) / 29.5)).toInt().coerceIn(1, 12)
        val hDay = (remainder - floor((hMonth - 1) * 29.5) + 1).toInt().coerceIn(1, 30)

        return Pair(hMonth, hDay)
    }

    /**
     * Returns true if today falls in the holy month of Ramadan (9th Hijri month).
     */
    fun isRamadan(): Boolean {
        val (month, _) = getHijriMonthAndDay()
        return month == HIJRI_RAMADAN
    }

    /**
     * Returns true during Eid ul Fitr (1st, 2nd, and 3rd of Shawwal).
     */
    fun isEidUlFitr(): Boolean {
        val (month, day) = getHijriMonthAndDay()
        return month == HIJRI_SHAWWAL && day in 1..3
    }

    /**
     * Returns true during Eid ul Azha (10th, 11th, 12th, and 13th of Dhul Hijjah).
     */
    fun isEidUlAzha(): Boolean {
        val (month, day) = getHijriMonthAndDay()
        return month == HIJRI_DHUL_HIJJAH && day in 10..13
    }

    /**
     * Returns the current Islamic occasion: "Ramadan", "Eid ul Fitr", "Eid ul Azha", or "Normal".
     */
    fun getCurrentIslamicOccasion(): String {
        return when {
            isEidUlFitr() -> "Eid ul Fitr"
            isEidUlAzha() -> "Eid ul Azha"
            isRamadan() -> "Ramadan"
            else -> "Normal"
        }
    }
}
