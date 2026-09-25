package com.example.freellmgateway

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface

class NetworkMonitor(private val context: Context) {

    fun currentType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "None"
        val caps = cm.getNetworkCapabilities(network) ?: return "None"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            else -> "Other"
        }
    }

    fun localIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (netInt in interfaces) {
                if (!netInt.isUp || netInt.isLoopback) continue
                val addresses = netInt.inetAddresses
                for (addr in addresses) {
                    val s = addr.hostAddress ?: continue
                    if (s.contains(':')) continue // skip IPv6
                    return s
                }
            }
        } catch (_: Exception) {
            // sandboxed / no access — fall back to WiFi info when available
        }
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.wifiConnectionInfo ?: return null
            val ip = info.ipAddress
            if (ip != 0) {
                return "%d.%d.%d.%d".format(
                    ip and 0xFF,
                    (ip shr 8) and 0xFF,
                    (ip shr 16) and 0xFF,
                    (ip shr 24) and 0xFF
                )
            }
        } catch (_: Exception) {
            // no WiFi access
        }
        return null
    }
}