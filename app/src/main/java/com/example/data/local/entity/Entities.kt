package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_prayers")
data class DailyPrayerEntity(
    @PrimaryKey val date: String, // Format: YYYY-MM-DD
    val fajrStatus: String = "NONE",
    val dhuhrStatus: String = "NONE",
    val asrStatus: String = "NONE",
    val maghribStatus: String = "NONE",
    val ishaStatus: String = "NONE",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "kaza_prayers")
data class KazaPrayerEntity(
    @PrimaryKey val prayerType: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA, WITR
    val owedCount: Int = 0,
    val completedCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "partner_info")
data class PartnerInfoEntity(
    @PrimaryKey val id: Int = 1,
    val myInviteCode: String = "EMB-5385",
    val myDisplayName: String = "",
    val myEmail: String = "",
    val isMatched: Boolean = false,
    val partnerDisplayName: String = "",
    val partnerEmail: String = "",
    val partnerInviteCode: String = "",
    val partnerFajr: String = "NONE",
    val partnerDhuhr: String = "NONE",
    val partnerAsr: String = "NONE",
    val partnerMaghrib: String = "NONE",
    val partnerIsha: String = "NONE",
    val partnerYesterdayFajr: String = "NONE",
    val partnerYesterdayDhuhr: String = "NONE",
    val partnerYesterdayAsr: String = "NONE",
    val partnerYesterdayMaghrib: String = "NONE",
    val partnerYesterdayIsha: String = "NONE",
    val partnerKazaCompleted: Int = 0,
    val partnerKazaOwed: Int = 0,
    val lastReceivedDua: String = "",
    val lastSyncTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val userEmail: String = "",
    val isLoggedIn: Boolean = false,
    val cityName: String = "İstanbul",
    val latitude: Double = 41.0082,
    val longitude: Double = 28.9784,
    val motivationNotificationEnabled: Boolean = true,
    val motivationNotificationTime: String = "11:00",
    val prayerRemindersEnabled: Boolean = true,
    val partnerAlertsEnabled: Boolean = true,
    val driveShareUrl: String = "https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk"
)
