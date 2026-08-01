package com.example.freellmgateway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.util.Log

class MainActivity : ComponentActivity() {
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private lateinit var client: HttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = HttpClient(OkHttp) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
        }

        // Start embedded Ktor server on background coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = SettingsStore(this@MainActivity)
            val host = prefs.bindHost ?: "127.7.7.7"
            val port = prefs.bindPort ?: 8080

            server = embeddedServer(CIO, host = host, port = port) {
                install(ContentNegotiation) { json() }
                routing {
                    post("/v1/chat/completions") {
                        val bodyText = call.receiveText()
                        try {
                            val remoteUrl = prefs.baseUrl ?: ""
                            val apiKey = prefs.apiKey ?: ""
                            if (remoteUrl.isBlank() || apiKey.isBlank()) {
                                call.respondText("{\"error\":\"not_configured\",\"message\":\"Set base URL and API key in Settings\"}", ContentType.Application.Json, HttpStatusCode.BadRequest)
                                return@post
                            }

                            val remoteResponseText: String = client.post(remoteUrl.trimEnd('/') + "/v1/chat/completions") {
                                contentType(ContentType.Application.Json)
                                header("Authorization", "Bearer $apiKey")
                                setBody(TextContent(bodyText, ContentType.Application.Json))
                            }.bodyAsText()

                            call.respondText(remoteResponseText, ContentType.Application.Json, HttpStatusCode.OK)
                        } catch (e: Exception) {
                            Log.e("gateway", "forward failed", e)
                            call.respondText("{\"error\":\"forwarding_failed\",\"message\":\"\"}", ContentType.Application.Json, HttpStatusCode.InternalServerError)
                        }
                    }

                    get("/v1/models") {
                        val prefs = SettingsStore(this@MainActivity)
                        val remoteUrl = prefs.baseUrl ?: ""
                        val apiKey = prefs.apiKey ?: ""
                        if (remoteUrl.isBlank() || apiKey.isBlank()) {
                            call.respond(mapOf("models" to listOf("not-configured")))
                            return@get
                        }
                        try {
                            val remoteResponseText: String = client.get(remoteUrl.trimEnd('/') + "/v1/models") {
                                header("Authorization", "Bearer $apiKey")
                                accept(ContentType.Application.Json)
                            }.bodyAsText()
                            call.respondText(remoteResponseText, ContentType.Application.Json)
                        } catch (e: Exception) {
                            call.respond(mapOf("models" to listOf("remote-unavailable")))
                        }
                    }

                    get("/health") {
                        call.respond(mapOf("status" to "ok"))
                    }
                }
            }.start(wait = false)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ctx = LocalContext.current
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("FreeLLM Gateway running (open Settings to configure)")
                        Button(onClick = {
                            val i = Intent(this@MainActivity, SettingsActivity::class.java)
                            startActivity(i)
                        }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            server?.stop(1000, 2000)
            client.close()
        } finally {
            super.onDestroy()
        }
    }
}
