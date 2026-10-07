package com.example.data.model

enum class PrayerStatus(val titleTr: String) {
    NONE("Belirtilmedi"),
    PRAYED("Kıldı"),
    MISSED("Kılmadı"),
    EXCUSED("Muaf")
}

enum class PrayerType(val displayNameTr: String, val rakats: Int) {
    FAJR("Sabah", 4),
    DHUHR("Öğle", 10),
    ASR("İkindi", 8),
    MAGHRIB("Akşam", 5),
    ISHA("Yatsı", 13),
    WITR("Vitir", 3)
}

data class DailyPrayerTimes(
    val imsak: String,
    val gunes: String,
    val ogle: String,
    val ikindi: String,
    val aksam: String,
    val yatsi: String,
    val nextPrayerName: String,
    val nextPrayerRemainingMillis: Long
)
