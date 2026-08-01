package com.example.freellmgateway

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SettingsStore(context: Context) {
    private val prefsName = "gateway_prefs_encrypted"
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        prefsName,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val baseUrl: String?
        get() = prefs.getString("baseUrl", null)

    val apiKey: String?
        get() = prefs.getString("apiKey", null)

    val bindHost: String?
        get() = prefs.getString("bindHost", "127.7.7.7")

    val bindPort: Int?
        get() = prefs.getInt("bindPort", 8080)

    val streaming: Boolean
        get() = prefs.getBoolean("streaming", true)
}
