package com.example.freellmgateway

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GatewayRepository {
    private const val TAG = "GatewayRepository"
    var server: ApplicationEngine? = null
    var client: HttpClient? = null

    var lastHost: String = "0.0.0.0"
        private set
    var lastPort: Int = 8080
        private set

    fun start(host: String, port: Int, baseUrl: String, apiKey: *** streaming: Boolean) {
        if (server != null) return
        lastHost = host
        lastPort = port

        client = HttpClient(OkHttp) {
            install(ContentNegotiation) { json() }
            defaultRequest { timeout(60_000) }
        }
        StatsStore.reset()
        StatsStore.setPort(port)

        CoroutineScope(Dispatchers.IO).launch {
            server = embeddedServer(CIO, host = host, port = port) {
                install(ContentNegotiation) { json() }
                installRouting(baseUrl, apiKey, streaming)
            }.start(wait = false)
            Log.i(TAG, "Gateway server started on $host:$port")
        }
    }

    private fun Application.installRouting(baseUrl: String, apiKey: *** streaming: Boolean) {
        routing {
            post("/v1/chat/completions") {
                val bodyText = call.receiveText()
                StatsStore.bumpRequest()
                try {
                    val c = client ?: throw IllegalStateException("client not ready")
                    val remoteUrl = baseUrl.trimEnd('/')
                    val remoteResponseText: String = c.post(remoteUrl + "/v1/chat/completions") {
                        contentType(ContentType.Application.Json)
                        header("Authorization", "Bearer $apiKey")
                        setBody(TextContent(bodyText, ContentType.Application.Json))
                    }.bodyAsText()
                    call.respondText(remoteResponseText, ContentType.Application.Json, HttpStatusCode.OK)
                } catch (e: Exception) {
                    StatsStore.bumpError()
                    Log.e(TAG, "forward failed", e)
                    call.respondText(
                        "{\"error\":\"forwarding_failed\",\"message\":\"${e.message?.take(200)}\"}",
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            get("/v1/models") {
                StatsStore.bumpRequest()
                val remoteUrl = baseUrl.trimEnd('/')
                try {
                    val c = client ?: throw IllegalStateException("client not ready")
                    val remoteResponseText: String = c.get(remoteUrl + "/v1/models") {
                        header("Authorization", "Bearer $apiKey")
                        accept(ContentType.Application.Json)
                    }.bodyAsText()
                    call.respondText(remoteResponseText, ContentType.Application.Json)
                    StatsStore.bumpModel()
                } catch (e: Exception) {
                    StatsStore.bumpError()
                    call.respond(mapOf("models" to listOf("remote-unavailable"), "error" to (e.message ?: "")))
                }
            }

            get("/health") {
                call.respond(
                    mapOf(
                        "status" to "ok",
                        "uptime_ms" to (System.currentTimeMillis() - 0),
                        "host" to host,
                        "port" to port,
                        "streaming" to streaming
                    )
                )
            }

            get("/stats") {
                call.respond(
                    mapOf(
                        "requests" to StatsStore.flow.value.requests,
                        "models" to StatsStore.flow.value.models,
                        "errors" to StatsStore.flow.value.errors,
                        "uptime_ms" to StatsStore.flow.value.uptimeMs,
                        "port" to port,
                        "host" to host
                    )
                )
            }
        }
    }

    fun stop() {
        try { server?.stop(1000, 2000) } catch (_: Exception) {}
        try { client?.close() } catch (_: Exception) {}
        server = null
        client = null
    }
}