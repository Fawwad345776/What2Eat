package com.aura.what2eat.model

data class AppContext(
    val country: String = "",
    val city: String = "",
    val season: String = "",
    val islamicOccasion: String = "",
    val isRamadan: Boolean = false,
    val isEidUlFitr: Boolean = false,
    val isEidUlAzha: Boolean = false
)
