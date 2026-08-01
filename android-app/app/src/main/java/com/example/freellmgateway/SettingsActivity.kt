package com.example.freellmgateway

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
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
        val btnSave = findViewById<Button>(R.id.btnSave)

        etBase.setText(sharedPreferences.getString("baseUrl", ""))
        etApi.setText(sharedPreferences.getString("apiKey", ""))
        etHost.setText(sharedPreferences.getString("bindHost", "127.7.7.7"))
        etPort.setText(sharedPreferences.getInt("bindPort", 8080).toString())
        swStreaming.isChecked = sharedPreferences.getBoolean("streaming", true)

        btnSave.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putString("baseUrl", etBase.text.toString())
            editor.putString("apiKey", etApi.text.toString())
            editor.putString("bindHost", etHost.text.toString())
            editor.putInt("bindPort", etPort.text.toString().toIntOrNull() ?: 8080)
            editor.putBoolean("streaming", swStreaming.isChecked)
            editor.apply()
            finish()
        }
    }
}
