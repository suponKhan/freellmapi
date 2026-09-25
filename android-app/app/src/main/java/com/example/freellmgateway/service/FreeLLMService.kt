package com.example.freellmgateway.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.freellmgateway.MainActivity
import com.example.freellmgateway.R
import com.example.freellmgateway.SettingsStore
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import android.util.Log

class FreeLLMService : Service() {
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private lateinit var client: HttpClient
    private val CHANNEL_ID = "FreeLLMServiceChannel"
    private val serviceStartTime = System.currentTimeMillis()
    private val connectionCount = java.util.concurrent.atomic.AtomicInteger(0)

    override fun onCreate() {
        super.onCreate()
        client = HttpClient(OkHttp) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FreeLLM Gateway")
            .setContentText("Proxy service is running")
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists
            .build()
        startForeground(1, notification)

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = SettingsStore(this@FreeLLMService)
            val host = prefs.bindHost ?: "127.7.7.7"
            val port = prefs.bindPort ?: 8080

            server = embeddedServer(CIO, host = host, port = port) {
                install(ContentNegotiation) { json() }
                routing {
                    post("/v1/chat/completions") {
                        val bodyText = call.receiveText()
                        val remoteUrl = prefs.baseUrl ?: ""
                        val apiKey = prefs.apiKey ?: ""
                        
                        if (remoteUrl.isBlank() || apiKey.isBlank()) {
                            call.respondText("{\"error\":\"not_configured\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
                            return@post
                        }

                        try {
                            val remoteResponseText: String = client.post(remoteUrl.trimEnd('/') + "/v1/chat/completions") {
                                contentType(ContentType.Application.Json)
                                header("Authorization", "Bearer $apiKey")
                                setBody(TextContent(bodyText, ContentType.Application.Json))
                            }.bodyAsText()
                            call.respondText(remoteResponseText, ContentType.Application.Json, HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respondText("{\"error\":\"forwarding_failed\"}", ContentType.Application.Json, HttpStatusCode.InternalServerError)
                        }
                    }
                    get("/health") { call.respond(mapOf("status" to "ok")) }
                    
                    get("/stats") {
                        val stats = mapOf(
                            "uptime" to (System.currentTimeMillis() - serviceStartTime),
                            "activeConnections" to connectionCount.get(),
                            "provider" to (prefs.baseUrl ?: "not_configured"),
                            "port" to port,
                            "host" to host
                        )
                        call.respond(stats)
                    }
                    
                    get("/dashboard") {
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head><title>FreeLLM Gateway Dashboard</title></head>
                            <body>
                            <h1>FreeLLM Gateway Dashboard</h1>
                            <p>Status: <strong>Running</strong></p>
                            <p>Bind: ${host}:${port}</p>
                            <p>Provider: ${prefs.baseUrl ?: "not_configured"}</p>
                            <p>API Key: ${if (prefs.apiKey?.isNotBlank() == true) "***" else "not_set"}</p>
                            <p>Uptime: ${System.currentTimeMillis() - serviceStartTime} ms</p>
                            <p>Active Connections: ${connectionCount.get()}</p>
                            <h2>Endpoints</h2>
                            <ul>
                            <li>POST /v1/chat/completions - Main proxy endpoint</li>
                            <li>GET /health - Health check</li>
                            <li>GET /stats - JSON stats</li>
                            </ul>
                            </body>
                            </html>
                        """.trimIndent()
                        call.respondText(html, ContentType.Text.Html)
                    }
                }
            }.start(wait = false)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop(1000, 2000)
        client.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "FreeLLM Gateway Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
