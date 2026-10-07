package com.example.data.util

import com.example.data.model.DailyPrayerTimes
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

object PrayerCalculator {

    data class CityInfo(val name: String, val lat: Double, val lng: Double)

    val TURKEY_CITIES = listOf(
        CityInfo("İstanbul", 41.0082, 28.9784),
        CityInfo("Ankara", 39.9334, 32.8597),
        CityInfo("İzmir", 38.4237, 27.1428),
        CityInfo("Bursa", 40.1885, 29.0610),
        CityInfo("Antalya", 36.8969, 30.7133),
        CityInfo("Konya", 37.8746, 32.4932),
        CityInfo("Adana", 37.0000, 35.3213),
        CityInfo("Gaziantep", 37.0662, 37.3833),
        CityInfo("Şanlıurfa", 37.1674, 38.7955),
        CityInfo("Kocaeli", 40.7654, 29.9408),
        CityInfo("Mersin", 36.8121, 34.6415),
        CityInfo("Diyarbakır", 37.9144, 40.2306),
        CityInfo("Hatay", 36.2023, 36.1606),
        CityInfo("Manisa", 38.6140, 27.4296),
        CityInfo("Kayseri", 38.7205, 35.4826),
        CityInfo("Samsun", 41.2867, 36.3300),
        CityInfo("Balıkesir", 39.6484, 27.8826),
        CityInfo("Kahramanmaraş", 37.5753, 36.9228),
        CityInfo("Van", 38.5012, 43.3730),
        CityInfo("Aydın", 37.8380, 27.8456),
        CityInfo("Denizli", 37.7830, 29.0963),
        CityInfo("Sakarya", 40.7889, 30.4060),
        CityInfo("Erzurum", 39.9055, 41.2658),
        CityInfo("Muğla", 37.2153, 28.3636),
        CityInfo("Eskişehir", 39.7767, 30.5206),
        CityInfo("Mardin", 37.3129, 40.7339),
        CityInfo("Malatya", 38.3554, 38.3335),
        CityInfo("Trabzon", 41.0027, 39.7168),
        CityInfo("Rize", 41.0255, 40.5177),
        CityInfo("Sivas", 39.7505, 37.0150),
        CityInfo("Batman", 37.8874, 41.1294),
        CityInfo("Elazığ", 38.6748, 39.2225),
        CityInfo("Kütahya", 39.4242, 29.9833),
        CityInfo("Afyonkarahisar", 38.7569, 30.5387),
        CityInfo("Çorum", 40.5489, 34.9537),
        CityInfo("Zonguldak", 41.4564, 31.7987),
        CityInfo("Isparta", 37.7648, 30.5566),
        CityInfo("Giresun", 40.9175, 38.3927),
        CityInfo("Ordu", 40.9862, 37.8797),
        CityInfo("Edirne", 41.6771, 26.5557),
        CityInfo("Mekke (Kabe)", 21.4225, 39.8262),
        CityInfo("Medine", 24.4672, 39.6111),
        CityInfo("Kudüs", 31.7683, 35.2137)
    )

    fun calculatePrayerTimes(
        lat: Double,
        lng: Double,
        calendar: Calendar = Calendar.getInstance()
    ): DailyPrayerTimes {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val timeZoneOffset = 3.0 // Turkey is UTC+3 permanently

        // Approximate solar calculation (standard astronomical equations)
        val b = 2 * Math.PI * (dayOfYear - 81) / 365.0
        // Equation of time in minutes
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        // Solar declination in radians
        val declination = Math.toRadians(23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81))))

        val latRad = Math.toRadians(lat)

        // Solar noon in hours (UTC+3)
        val solarNoonUtcHours = 12.0 - (lng / 15.0) - (eot / 60.0)
        val solarNoon = solarNoonUtcHours + timeZoneOffset

        // Sun angles for Turkey Diyanet:
        // Fajr (İmsak): 18 degrees below horizon
        val fajrAngle = Math.toRadians(18.0)
        val sunriseAngle = Math.toRadians(0.833) // Atmospheric refraction & disk
        val ishaAngle = Math.toRadians(17.0) // 17 degrees

        // Hour angle helper
        fun hourAngle(altitudeRad: Double): Double {
            val cosHA = (sin(altitudeRad) - sin(latRad) * sin(declination)) / (cos(latRad) * cos(declination))
            val clamped = cosHA.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clamped)) / 15.0
        }

        val sunriseHA = hourAngle(-sunriseAngle)
        val fajrHA = hourAngle(-fajrAngle)
        val ishaHA = hourAngle(-ishaAngle)

        // Asr calculation (Shafi/Hanbali/Maliki standard shadow = 1)
        val asrAltitude = atan(1.0 / (1.0 + tan(abs(latRad - declination))))
        val asrHA = hourAngle(asrAltitude)

        val fajrTimeHours = solarNoon - fajrHA
        val sunriseTimeHours = solarNoon - sunriseHA
        val dhuhrTimeHours = solarNoon + (5.0 / 60.0) // Temkin ~5 mins
        val asrTimeHours = solarNoon + asrHA
        val maghribTimeHours = solarNoon + sunriseHA + (7.0 / 60.0) // Temkin ~7 mins
        val ishaTimeHours = solarNoon + ishaHA

        val imsakStr = formatHoursToTime(fajrTimeHours)
        val gunesStr = formatHoursToTime(sunriseTimeHours)
        val ogleStr = formatHoursToTime(dhuhrTimeHours)
        val ikindiStr = formatHoursToTime(asrTimeHours)
        val aksamStr = formatHoursToTime(maghribTimeHours)
        val yatsiStr = formatHoursToTime(ishaTimeHours)

        // Calculate next prayer and remaining millis
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)
        val currentSec = calendar.get(Calendar.SECOND)
        val currentTotalSec = currentHour * 3600 + currentMin * 60 + currentSec

        val prayerTimesInSec = listOf(
            Pair("İmsak", timeStrToSeconds(imsakStr)),
            Pair("Güneş", timeStrToSeconds(gunesStr)),
            Pair("Öğle", timeStrToSeconds(ogleStr)),
            Pair("İkindi", timeStrToSeconds(ikindiStr)),
            Pair("Akşam", timeStrToSeconds(aksamStr)),
            Pair("Yatsı", timeStrToSeconds(yatsiStr))
        )

        var nextPrayer = "İmsak (Yarın)"
        var remainingSec = (24 * 3600 - currentTotalSec) + prayerTimesInSec[0].second

        for (pt in prayerTimesInSec) {
            if (pt.second > currentTotalSec) {
                nextPrayer = pt.first
                remainingSec = pt.second - currentTotalSec
                break
            }
        }

        return DailyPrayerTimes(
            imsak = imsakStr,
            gunes = gunesStr,
            ogle = ogleStr,
            ikindi = ikindiStr,
            aksam = aksamStr,
            yatsi = yatsiStr,
            nextPrayerName = nextPrayer,
            nextPrayerRemainingMillis = remainingSec * 1000L
        )
    }

    private fun formatHoursToTime(hours: Double): String {
        var h = hours % 24.0
        if (h < 0) h += 24.0
        val totalMinutes = (h * 60.0).roundToInt()
        val hourPart = (totalMinutes / 60) % 24
        val minPart = totalMinutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hourPart, minPart)
    }

    private fun timeStrToSeconds(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 3600 + m * 60
    }

    /**
     * Calculates Qibla direction (bearing in degrees from true North 0..360)
     */
    fun calculateQiblaBearing(lat: Double, lng: Double): Double {
        val kaabaLat = Math.toRadians(21.4225)
        val kaabaLng = Math.toRadians(39.8262)
        val userLat = Math.toRadians(lat)
        val userLng = Math.toRadians(lng)

        val deltaLng = kaabaLng - userLng
        val y = sin(deltaLng) * cos(kaabaLat)
        val x = cos(userLat) * sin(kaabaLat) - sin(userLat) * cos(kaabaLat) * cos(deltaLng)
        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return (bearingDeg + 360.0) % 360.0
    }

    /**
     * Checks if a prayer time has arrived for the day.
     * If isToday is false (e.g. yesterday), returns true because past days are completed.
     */
    fun isPrayerTimeArrived(prayerType: String, prayerTimes: DailyPrayerTimes, isToday: Boolean): Boolean {
        if (!isToday) return true
        val cal = Calendar.getInstance()
        val currentSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
        val prayerTimeStr = when (prayerType.uppercase()) {
            "FAJR", "SABAH" -> prayerTimes.imsak
            "DHUHR", "OGLE", "ÖĞLE" -> prayerTimes.ogle
            "ASR", "IKINDI", "İKİNDİ" -> prayerTimes.ikindi
            "MAGHRIB", "AKSAM", "AKŞAM" -> prayerTimes.aksam
            "ISHA", "YATSI" -> prayerTimes.yatsi
            else -> return true
        }
        val targetSec = timeStrToSeconds(prayerTimeStr)
        return currentSec >= targetSec
    }
}
