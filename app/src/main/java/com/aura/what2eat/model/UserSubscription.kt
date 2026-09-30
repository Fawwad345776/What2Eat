package com.aura.what2eat.model

data class UserSubscription(
    val isPro: Boolean = false,
    val planType: String = "free", // "free", "monthly", "yearly"
    val expiryDate: Long? = null
)
