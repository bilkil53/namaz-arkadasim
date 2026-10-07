package com.example.data.network

import android.util.Log
import com.example.data.model.DailyPrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DiyanetPrayerService {

    private const val TAG = "DiyanetPrayerService"

    /**
     * Fetches official prayer times from Aladhan API using Method 13
     * (Method 13 = Diyanet İşleri Başkanlığı, Turkey calculation & calendar).
     */
    suspend fun fetchDiyanetPrayerTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance()
    ): DailyPrayerTimes? = withContext(Dispatchers.IO) {
        try {
            val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(calendar.time)
            // method 13 is officially "Diyanet İşleri Başkanlığı, Turkey"
            val urlString = "https://api.aladhan.com/v1/timings/$dateStr?latitude=$latitude&longitude=$longitude&method=13"
            Log.d(TAG, "Fetching official Diyanet prayer times from: $urlString")

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NamazArkadasim-Android/1.6")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val data = json.optJSONObject("data")
                val timings = data?.optJSONObject("timings")

                if (timings != null) {
                    val imsak = cleanTime(timings.optString("Imsak", timings.optString("Fajr", "05:30")))
                    val gunes = cleanTime(timings.optString("Sunrise", "06:55"))
                    val ogle = cleanTime(timings.optString("Dhuhr", "13:00"))
                    val ikindi = cleanTime(timings.optString("Asr", "16:20"))
                    val aksam = cleanTime(timings.optString("Maghrib", "18:55"))
                    val yatsi = cleanTime(timings.optString("Isha", "20:15"))

                    Log.d(TAG, "Official Diyanet times parsed: Imsak=$imsak, Güneş=$gunes, Öğle=$ogle, İkindi=$ikindi, Akşam=$aksam, Yatsı=$yatsi")

                    return@withContext buildPrayerTimesObject(
                        imsak = imsak,
                        gunes = gunes,
                        ogle = ogle,
                        ikindi = ikindi,
                        aksam = aksam,
                        yatsi = yatsi,
                        calendar = calendar
                    )
                }
            } else {
                Log.w(TAG, "Failed response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Diyanet prayer times: ${e.message}", e)
        }
        return@withContext null
    }

    private fun cleanTime(raw: String): String {
        // Strip timezone or parentheses like "05:43 (EEST)" -> "05:43"
        return raw.split(" ").firstOrNull()?.take(5) ?: "00:00"
    }

    private fun timeStrToSeconds(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 3600 + m * 60
    }

    private fun buildPrayerTimesObject(
        imsak: String,
        gunes: String,
        ogle: String,
        ikindi: String,
        aksam: String,
        yatsi: String,
        calendar: Calendar
    ): DailyPrayerTimes {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)
        val currentSec = calendar.get(Calendar.SECOND)
        val currentTotalSec = currentHour * 3600 + currentMin * 60 + currentSec

        val prayerList = listOf(
            Pair("İmsak", timeStrToSeconds(imsak)),
            Pair("Güneş", timeStrToSeconds(gunes)),
            Pair("Öğle", timeStrToSeconds(ogle)),
            Pair("İkindi", timeStrToSeconds(ikindi)),
            Pair("Akşam", timeStrToSeconds(aksam)),
            Pair("Yatsı", timeStrToSeconds(yatsi))
        )

        var nextPrayer = "İmsak"
        var remainingSec = (24 * 3600 - currentTotalSec) + prayerList[0].second

        for (item in prayerList) {
            if (item.second > currentTotalSec) {
                nextPrayer = item.first
                remainingSec = item.second - currentTotalSec
                break
            }
        }

        val remainingMillis = (remainingSec * 1000L).coerceAtLeast(0L)

        return DailyPrayerTimes(
            imsak = imsak,
            gunes = gunes,
            ogle = ogle,
            ikindi = ikindi,
            aksam = aksam,
            yatsi = yatsi,
            nextPrayerName = nextPrayer,
            nextPrayerRemainingMillis = remainingMillis
        )
    }
}
