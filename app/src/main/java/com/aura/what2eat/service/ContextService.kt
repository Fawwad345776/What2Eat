package com.aura.what2eat.service

import android.content.Context
import com.aura.what2eat.model.AppContext

object ContextService {

    /**
     * Builds the complete contextual environment for What2Eat AI suggestions by aggregating:
     * 1. Location (City, Country)
     * 2. Climate & Season
     * 3. Islamic Calendar & Holidays (Ramadan, Eid ul Fitr, Eid ul Azha)
     */
    suspend fun buildAppContext(context: Context): AppContext {
        // 1. Resolve Location (City, Country)
        val (city, country) = LocationService.getCurrentLocation(context)

        // 2. Resolve Season for country
        val season = SeasonService.getCurrentSeason(country)

        // 3. Resolve Islamic Calendar occasions
        val isRamadan = IslamicCalendarService.isRamadan()
        val isEidUlFitr = IslamicCalendarService.isEidUlFitr()
        val isEidUlAzha = IslamicCalendarService.isEidUlAzha()
        val occasion = IslamicCalendarService.getCurrentIslamicOccasion()

        return AppContext(
            country = country,
            city = city,
            season = season,
            islamicOccasion = occasion,
            isRamadan = isRamadan,
            isEidUlFitr = isEidUlFitr,
            isEidUlAzha = isEidUlAzha
        )
    }
}
