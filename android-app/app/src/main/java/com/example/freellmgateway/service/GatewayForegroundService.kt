package com.example.freellmgateway.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.freellmgateway.GatewayRepository
import com.example.freellmgateway.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GatewayForegroundService : LifecycleService() {

    companion object {
        private const val TAG = "GatewayForegroundService"
        private const val CHANNEL_ID = "freellm_gateway_channel"
        private const val NOTIF_ID = 4242

        fun start(context: Context, host: String, port: Int, baseUrl: String, apiKey: String, streaming: Boolean) {
            val intent = Intent(context, GatewayForegroundService::class.java).apply {
                putExtra("host", host)
                putExtra("port", port)
                putExtra("baseUrl", baseUrl)
                putExtra("apiKey", apiKey)
                putExtra("streaming", streaming)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GatewayForegroundService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val host = intent?.getStringExtra("host") ?: "0.0.0.0"
        val port = intent?.getIntExtra("port", 8080) ?: 8080
        val baseUrl = intent?.getStringExtra("baseUrl") ?: ""
        val apiKey = intent?.getStringExtra("apiKey") ?: ""
        val streaming = intent?.getBooleanExtra("streaming", true) ?: true

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FreeLLM Gateway")
            .setContentText("Router running on $host:$port")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        CoroutineScope(Dispatchers.IO).launch {
            GatewayRepository.start(host, port, baseUrl, apiKey, streaming)
            Log.i(TAG, "Gateway started from foreground service on $host:$port")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        GatewayRepository.stop()
        Log.i(TAG, "Gateway foreground service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FreeLLM Gateway Router",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the LLM proxy running in the background"
            showBadge = false
        }
        manager.createNotificationChannel(channel)
    }
}