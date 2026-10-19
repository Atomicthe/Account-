package com.example.data.currency

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class CurrencyDef(
    val code: String,
    val symbol: String,
    val name: String,
    val flagEmoji: String,
    val defaultRateToTwd: Double // 1 unit of foreign currency = X TWD
)

object CurrencyManager {
    val supportedCurrencies = listOf(
        CurrencyDef("TWD", "NT$", "新台幣", "🇹🇼", 1.0),
        CurrencyDef("JPY", "¥", "日圓", "🇯🇵", 0.210),
        CurrencyDef("USD", "$", "美元", "🇺🇸", 32.20),
        CurrencyDef("EUR", "€", "歐元", "🇪🇺", 34.60),
        CurrencyDef("KRW", "₩", "韓元", "🇰🇷", 0.024),
        CurrencyDef("CNY", "¥", "人民幣", "🇨🇳", 4.45),
        CurrencyDef("HKD", "HK$", "港幣", "🇭🇰", 4.12),
        CurrencyDef("GBP", "£", "英鎊", "🇬🇧", 41.50),
        CurrencyDef("THB", "฿", "泰銖", "🇹🇭", 0.96)
    )

    // Current rates in memory (Foreign Currency -> TWD)
    private val currentRatesToTwd = mutableMapOf<String, Double>().apply {
        supportedCurrencies.forEach { put(it.code, it.defaultRateToTwd) }
    }

    var lastUpdatedTime: String = "預設匯率 (離線)"
        private set

    fun getCurrency(code: String): CurrencyDef {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies.first()
    }

    fun getRateToTwd(code: String): Double {
        if (code.equals("TWD", ignoreCase = true) || code.equals("NT$", ignoreCase = true)) {
            return 1.0
        }
        return currentRatesToTwd[code.uppercase()] ?: getCurrency(code).defaultRateToTwd
    }

    fun convertToTwd(amount: Double, fromCurrencyCode: String): Double {
        val rate = getRateToTwd(fromCurrencyCode)
        return amount * rate
    }

    fun convertFromTwd(twdAmount: Double, toCurrencyCode: String): Double {
        val rate = getRateToTwd(toCurrencyCode)
        return if (rate > 0) twdAmount / rate else twdAmount
    }

    suspend fun refreshLiveRates(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://open.er-api.com/v6/latest/TWD")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optString("result") == "success" && json.has("rates")) {
                    val ratesObj = json.getJSONObject("rates")
                    supportedCurrencies.forEach { curr ->
                        if (curr.code != "TWD" && ratesObj.has(curr.code)) {
                            val rateInTwdBase = ratesObj.getDouble(curr.code) // 1 TWD = X Foreign
                            if (rateInTwdBase > 0) {
                                currentRatesToTwd[curr.code] = 1.0 / rateInTwdBase
                            }
                        }
                    }
                    val now = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
                    lastUpdatedTime = "今日 $now 即時更新"
                    return@withContext true
                }
            }
        } catch (_: Exception) {
            // Keep using default baseline or cached rates
        }
        return@withContext false
    }
}
