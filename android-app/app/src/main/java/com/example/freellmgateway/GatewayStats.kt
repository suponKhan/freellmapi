package com.example.freellmgateway

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

data class GatewayStats(
    val requests: Long = 0,
    val models: Long = 0,
    val errors: Long = 0,
    val uptimeMs: Long = 0,
    val port: Int = 0
)

object StatsStore {
    private val _flow = MutableStateFlow(GatewayStats())
    val flow: StateFlow<GatewayStats> = _flow.value

    private val requestCount = AtomicLong(0)
    private val modelCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private var startTime = 0L

    fun reset() {
        requestCount.set(0)
        modelCount.set(0)
        errorCount.set(0)
        startTime = System.currentTimeMillis()
        push()
    }

    fun bumpRequest() {
        requestCount.incrementAndGet()
        push()
    }

    fun bumpModel() {
        modelCount.incrementAndGet()
        push()
    }

    fun bumpError() {
        errorCount.incrementAndGet()
        push()
    }

    fun setPort(port: Int) {
        push(port = port)
    }

    private fun push(port: Int = 0) {
        _flow.value = GatewayStats(
            requests = requestCount.get(),
            models = modelCount.get(),
            errors = errorCount.get(),
            uptimeMs = System.currentTimeMillis() - startTime,
            port = port
        )
    }
}