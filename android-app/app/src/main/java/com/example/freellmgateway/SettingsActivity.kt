package com.example.freellmgateway

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefsName = "gateway_prefs_encrypted"
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            prefsName,
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val etBase = findViewById<EditText>(R.id.etBaseUrl)
        val etApi = findViewById<EditText>(R.id.etApiKey)
        val etHost = findViewById<EditText>(R.id.etHost)
        val etPort = findViewById<EditText>(R.id.etPort)
        val swStreaming = findViewById<Switch>(R.id.swStreaming)
        val btnStart = findViewById<Button>(R.id.btnStartService)
        val btnStop = findViewById<Button>(R.id.btnStopService)

        // Load existing values
        etBase.setText(sharedPreferences.getString("baseUrl", "") ?: "")
        etApi.setText(sharedPreferences.getString("apiKey", "") ?: "")
        etHost.setText(sharedPreferences.getString("bindHost", "127.7.7.7") ?: "127.7.7.7")
        etPort.setText((sharedPreferences.getInt("bindPort", 8080)).toString())
        swStreaming.isChecked = sharedPreferences.getBoolean("streaming", true)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val host = etHost.text.toString().ifBlank { "127.7.7.7" }
            val port = etPort.text.toString().toIntOrNull()?.coerceIn(1, 65535) ?: 8080
            val base = etBase.text.toString().trim()
            val key = etApi.text.toString().trim()

            val editor = sharedPreferences.edit()
            editor.putString("baseUrl", base)
            editor.putString("apiKey", key)
            editor.putString("bindHost", host)
            editor.putInt("bindPort", port)
            editor.putBoolean("streaming", swStreaming.isChecked)
            editor.apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            GatewayForegroundService.start(
                context = this,
                host = etHost.text.toString().ifBlank { "127.7.7.7" },
                port = etPort.text.toString().toIntOrNull()?.coerceIn(1, 65535) ?: 8080,
                baseUrl = etBase.text.toString().trim(),
                apiKey = etApi.text.toString().trim(),
                streaming = swStreaming.isChecked
            )
            Toast.makeText(this, "Gateway service starting", Toast.LENGTH_SHORT).show()
        }

        btnStop.setOnClickListener {
            GatewayForegroundService.stop(this)
            Toast.makeText(this, "Gateway service stopping", Toast.LENGTH_SHORT).show()
        }
    }
}
