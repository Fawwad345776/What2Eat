package com.aura.what2eat.service

import java.util.Calendar

object SeasonService {

    /**
     * Determines the current season based on target country and the current month.
     */
    fun getCurrentSeason(country: String): String {
        val calendar = Calendar.getInstance()
        // Calendar.MONTH is 0-based: 0 = January, 11 = December
        val month = calendar.get(Calendar.MONTH) + 1
        val normalized = country.trim().lowercase()

        return when {
            // 1. Middle East / Gulf Region
            normalized.contains("uae") ||
            normalized.contains("emirates") ||
            normalized.contains("saudi") ||
            normalized.contains("qatar") ||
            normalized.contains("kuwait") ||
            normalized.contains("bahrain") ||
            normalized.contains("oman") -> {
                if (month in 4..10) "Hot Summer" else "Mild Winter"
            }

            // 2. Southern Hemisphere
            normalized.contains("australia") ||
            normalized.contains("new zealand") ||
            normalized.contains("south africa") ||
            normalized.contains("argentina") ||
            normalized.contains("chile") -> {
                when (month) {
                    12, 1, 2 -> "Summer"
                    3, 4, 5 -> "Autumn"
                    6, 7, 8 -> "Winter"
                    9, 10, 11 -> "Spring"
                    else -> "Summer"
                }
            }

            // 3. Northern Hemisphere (Pakistan, India, Bangladesh, USA, UK, Canada, China, Europe, etc.)
            else -> {
                when (month) {
                    12, 1, 2 -> "Winter"
                    3, 4, 5 -> "Spring"
                    6, 7, 8, 9 -> "Summer"
                    10, 11 -> "Autumn"
                    else -> "Summer"
                }
            }
        }
    }
}
