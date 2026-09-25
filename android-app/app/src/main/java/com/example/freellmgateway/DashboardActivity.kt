package com.example.freellmgateway

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : ComponentActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvRequests: TextView
    private lateinit var tvModels: TextView
    private lateinit var tvErrors: TextView
    private lateinit var tvUptime: TextView
    private lateinit var tvNetwork: TextView
    private lateinit var tvIp: TextView
    private lateinit var btnStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tvStatus = findViewById(R.id.tvStatus)
        tvRequests = findViewById(R.id.tvRequests)
        tvModels = findViewById(R.id.tvModels)
        tvErrors = findViewById(R.id.tvErrors)
        tvUptime = findViewById(R.id.tvUptime)
        tvNetwork = findViewById(R.id.tvNetwork)
        tvIp = findViewById(R.id.tvIp)
        btnStop = findViewById(R.id.btnStop)

        val monitor = NetworkMonitor(this)
        tvNetwork.text = "Network: ${monitor.currentType()}"
        tvIp.text = "IP: ${monitor.localIpAddress() ?: "unknown"}"

        btnStop.setOnClickListener {
            GatewayRepository.stop()
            finish()
        }

        refresh()
    }

    private fun refresh() {
        val s = StatsStore.flow.value
        val running = GatewayRepository.server != null
        tvStatus.text = if (running) "Status: RUNNING" else "Status: STOPPED"
        tvRequests.text = "Requests: ${s.requests}"
        tvModels.text = "Models: ${s.models}"
        tvErrors.text = "Errors: ${s.errors}"
        tvUptime.text = "Uptime: ${s.uptimeMs / 1000}s"

        CoroutineScope(Dispatchers.Main).launch {
            while (!isDestroyed && !isFinishing) {
                withContext(Dispatchers.Default) {
                    val live = StatsStore.flow.value
                    val alive = GatewayRepository.server != null
                }
                tvStatus.text = if (GatewayRepository.server != null) "Status: RUNNING" else "Status: STOPPED"
                val live = StatsStore.flow.value
                tvRequests.text = "Requests: ${live.requests}"
                tvModels.text = "Models: ${live.models}"
                tvErrors.text = "Errors: ${live.errors}"
                tvUptime.text = "Uptime: ${live.uptimeMs / 1000}s"
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}