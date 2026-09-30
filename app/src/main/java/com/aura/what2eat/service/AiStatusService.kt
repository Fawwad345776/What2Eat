package com.aura.what2eat.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiApiStatus(
    val isWorking: Boolean,
    val provider: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AiStatusService {

    private val _currentStatus = MutableStateFlow(
        AiApiStatus(
            isWorking = true,
            provider = "Ready",
            message = "AI API is ready"
        )
    )
    val currentStatus: StateFlow<AiApiStatus> = _currentStatus.asStateFlow()

    fun reportSuccess(provider: String, details: String = "") {
        _currentStatus.value = AiApiStatus(
            isWorking = true,
            provider = provider,
            message = if (details.isNotBlank()) "AI Online: $provider ($details)" else "AI Online: $provider"
        )
    }

    fun reportOffline(reason: String = "") {
        _currentStatus.value = AiApiStatus(
            isWorking = false,
            provider = "Local Cache / Offline",
            message = if (reason.isNotBlank()) "AI Offline: Using Local Saved File ($reason)" else "AI Offline: Using Local Saved File"
        )
    }
}
