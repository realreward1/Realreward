package com.example.core.security

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

data class NetworkSecuritySnapshot(
    val ipAddress: String,
    val countryCode: String,
    val locationLabel: String,
    val vpnDetected: Boolean
)

object NetworkSecurityMonitor {

    fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val active = cm.activeNetwork
            val caps = if (active != null) cm.getNetworkCapabilities(active) else null
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true
            }
            @Suppress("DEPRECATION")
            val allNetworks = cm.allNetworks
            for (net in allNetworks) {
                val netCaps = cm.getNetworkCapabilities(net)
                if (netCaps != null && netCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true
                }
            }
        }

        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                if (name.startsWith("tun") || name.startsWith("tap") ||
                    name.startsWith("ppp") || name.startsWith("wg") ||
                    name.startsWith("pptp") || name.startsWith("ipsec")
                ) {
                    return@runCatching true
                }
            }
            false
        }.getOrDefault(false)
    }

    suspend fun inspectNetwork(context: Context): NetworkSecuritySnapshot = withContext(Dispatchers.IO) {
        val vpn = isVpnActive(context)
        var ip = ""
        var countryCode = ""
        var locationLabel = ""

        // Primary lookup: ipwho.is (HTTPS, zero API key; captures city/region/country for admin audit only)
        runCatching {
            val json = fetchJson("https://ipwho.is/")
            if (json.optBoolean("success", true)) {
                ip = json.optString("ip", "").take(64)
                countryCode = json.optString("country_code", "").uppercase().take(8)
                val city = json.optString("city", "").trim()
                val region = json.optString("region", "").trim()
                val country = json.optString("country", "").trim()
                locationLabel = listOf(city, region, country)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .take(150)
            }
        }

        // Secondary fallback: api.country.is
        if (countryCode.isBlank()) {
            runCatching {
                val json = fetchJson("https://api.country.is/")
                if (ip.isBlank()) ip = json.optString("ip", "").take(64)
                countryCode = json.optString("country", "").uppercase().take(8)
                if (locationLabel.isBlank() && countryCode.isNotBlank()) {
                    locationLabel = "Country: $countryCode"
                }
            }
        }

        NetworkSecuritySnapshot(
            ipAddress = ip.ifBlank { "unknown" },
            countryCode = countryCode,
            locationLabel = locationLabel,
            vpnDetected = vpn
        )
    }

    private fun fetchJson(endpoint: String): JSONObject {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4500
            readTimeout = 4500
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val stream = conn.inputStream
            val body = stream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }
}
