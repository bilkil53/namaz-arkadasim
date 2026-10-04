package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.PrayerDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import com.example.util.NotificationHelper
import com.example.util.PartnerSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PrayerRepository(
    private val prayerDao: PrayerDao,
    private val context: Context? = null
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingPrayerEventJobs = ConcurrentHashMap<String, Job>()

    private var lastProcessedEventTime: Long = System.currentTimeMillis()

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun ensureUniqueInviteCode(): PartnerInfoEntity {
        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        // If code is default placeholder or blank, generate a truly unique random code
        if (current.myInviteCode.isBlank() || current.myInviteCode == "ARK-8521") {
            val newCode = "ARK-${(1000..9999).random()}"
            val updated = current.copy(myInviteCode = newCode)
            prayerDao.savePartnerInfo(updated)
            return updated
        }
        return current
    }

    fun getPrayerForDate(date: String): Flow<DailyPrayerEntity?> {
        return prayerDao.getPrayerForDate(date)
    }

    fun getRecentDailyPrayers(): Flow<List<DailyPrayerEntity>> {
        return prayerDao.getRecentDailyPrayers()
    }

    suspend fun setPrayerStatus(date: String, prayerType: String, status: String) {
        val existing = prayerDao.getPrayerForDate(date).firstOrNull() ?: DailyPrayerEntity(date = date)
        val updated = when (prayerType.uppercase()) {
            "FAJR", "SABAH" -> existing.copy(fajrStatus = status, updatedAt = System.currentTimeMillis())
            "DHUHR", "OGLE", "ÖĞLE" -> existing.copy(dhuhrStatus = status, updatedAt = System.currentTimeMillis())
            "ASR", "IKINDI", "İKİNDİ" -> existing.copy(asrStatus = status, updatedAt = System.currentTimeMillis())
            "MAGHRIB", "AKSAM", "AKŞAM" -> existing.copy(maghribStatus = status, updatedAt = System.currentTimeMillis())
            "ISHA", "YATSI", "YATSI" -> existing.copy(ishaStatus = status, updatedAt = System.currentTimeMillis())
            else -> existing
        }
        prayerDao.insertOrUpdateDailyPrayer(updated)

        // Broadcast to partner if matched
        broadcastCurrentStatusToPartner(todayDate = date, dailyPrayer = updated)

        // Cancel previous pending notification job for this prayer type
        // This prevents double notifications if user taps through or changes their mind (e.g. kıldı then kılınmadı)
        val prayerKey = prayerType.uppercase()
        pendingPrayerEventJobs[prayerKey]?.cancel()

        val trName = when (prayerKey) {
            "FAJR", "SABAH" -> "Sabah"
            "DHUHR", "OGLE", "ÖĞLE" -> "Öğle"
            "ASR", "IKINDI", "İKİNDİ" -> "İkindi"
            "MAGHRIB", "AKSAM", "AKŞAM" -> "Akşam"
            "ISHA", "YATSI" -> "Yatsı"
            else -> prayerType
        }

        // Send prayer notification to partner after a 5-second delay (debounce)
        if (status == "PRAYED" || status == "MISSED") {
            pendingPrayerEventJobs[prayerKey] = repositoryScope.launch {
                try {
                    delay(5000L) // Eşlere 5 saniye sonra gitsin

                    // Verify latest status from database after 5 seconds to ensure user didn't change it again
                    val latest = prayerDao.getPrayerForDate(date).firstOrNull() ?: return@launch
                    val currentStatus = when (prayerKey) {
                        "FAJR", "SABAH" -> latest.fajrStatus
                        "DHUHR", "OGLE", "ÖĞLE" -> latest.dhuhrStatus
                        "ASR", "IKINDI", "İKİNDİ" -> latest.asrStatus
                        "MAGHRIB", "AKSAM", "AKŞAM" -> latest.maghribStatus
                        "ISHA", "YATSI" -> latest.ishaStatus
                        else -> status
                    }

                    val partner = prayerDao.getPartnerInfo().firstOrNull()
                    if (partner != null && partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
                        val spouseName = partner.myDisplayName.ifBlank { "Eşiniz" }
                        if (currentStatus == "PRAYED") {
                            PartnerSyncManager.broadcastPartnerEvent(
                                codeA = partner.myInviteCode,
                                codeB = partner.partnerInviteCode,
                                myCode = partner.myInviteCode,
                                myName = spouseName,
                                eventType = "PRAYED",
                                prayerName = trName,
                                actionText = "$trName namazını kıldı. Allah kabul etsin! 🤲"
                            )
                        } else if (currentStatus == "MISSED") {
                            PartnerSyncManager.broadcastPartnerEvent(
                                codeA = partner.myInviteCode,
                                codeB = partner.partnerInviteCode,
                                myCode = partner.myInviteCode,
                                myName = spouseName,
                                eventType = "MISSED",
                                prayerName = trName,
                                actionText = "$trName namazını kılmadı."
                            )
                        }
                    }
                } catch (_: CancellationException) {
                    // Job was cancelled by a newer status change within 5 seconds, do nothing
                }
            }
        }
    }

    // Kaza
    fun getAllKazaPrayers(): Flow<List<KazaPrayerEntity>> {
        return prayerDao.getAllKazaPrayers()
    }

    suspend fun adjustKazaOwed(type: String, delta: Int) {
        prayerDao.adjustKazaOwed(type.uppercase(), delta)
        broadcastCurrentStatusToPartner()
    }

    suspend fun completeOneKaza(type: String) {
        prayerDao.completeOneKaza(type.uppercase())
        broadcastCurrentStatusToPartner()

        val partner = prayerDao.getPartnerInfo().firstOrNull()
        if (partner != null && partner.isMatched && partner.partnerInviteCode.isNotBlank()) {
            val trName = when (type.uppercase()) {
                "FAJR", "SABAH" -> "Sabah"
                "DHUHR", "OGLE", "ÖĞLE" -> "Öğle"
                "ASR", "IKINDI", "İKİNDİ" -> "İkindi"
                "MAGHRIB", "AKSAM", "AKŞAM" -> "Akşam"
                "ISHA", "YATSI" -> "Yatsı"
                "WITR", "VITIR" -> "Vitir"
                else -> type
            }
            PartnerSyncManager.broadcastPartnerEvent(
                codeA = partner.myInviteCode,
                codeB = partner.partnerInviteCode,
                myCode = partner.myInviteCode,
                myName = partner.myDisplayName.ifBlank { "Eşiniz" },
                eventType = "KAZA",
                prayerName = trName,
                actionText = "1 vakit $trName kaza namazı kıldı. Tebrikler! 🤲"
            )
        }
    }

    suspend fun addBulkKaza(amount: Int) {
        prayerDao.addBulkKaza(amount)
        broadcastCurrentStatusToPartner()
    }

    // Partner
    fun getPartnerInfo(): Flow<PartnerInfoEntity?> {
        return prayerDao.getPartnerInfo()
    }

    private suspend fun broadcastCurrentStatusToPartner(
        todayDate: String = getTodayDateString(),
        dailyPrayer: DailyPrayerEntity? = null
    ) {
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: return
        if (!partner.isMatched || partner.partnerInviteCode.isBlank()) return

        val prayers = dailyPrayer ?: prayerDao.getPrayerForDate(todayDate).firstOrNull() ?: DailyPrayerEntity(date = todayDate)
        val kazas = prayerDao.getAllKazaPrayers().firstOrNull() ?: emptyList()
        val totalCompleted = kazas.sumOf { it.completedCount }
        val totalOwed = kazas.sumOf { it.owedCount }

        PartnerSyncManager.broadcastPrayerStatus(
            codeA = partner.myInviteCode,
            codeB = partner.partnerInviteCode,
            myCode = partner.myInviteCode,
            myName = partner.myDisplayName.ifBlank { "Namaz Arkadaşım" },
            myEmail = partner.myEmail,
            date = todayDate,
            fajr = prayers.fajrStatus,
            dhuhr = prayers.dhuhrStatus,
            asr = prayers.asrStatus,
            maghrib = prayers.maghribStatus,
            isha = prayers.ishaStatus,
            kazaCompleted = totalCompleted,
            kazaOwed = totalOwed
        )
    }

    suspend fun refreshPartnerData(): PartnerInfoEntity {
        var current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        if (current.myInviteCode.isBlank() || current.myInviteCode == "ARK-8521") {
            current = ensureUniqueInviteCode()
        }

        val today = getTodayDateString()

        // 1. If NOT matched yet: check if someone has claimed my invite code!
        if (!current.isMatched) {
            // Re-publish my profile so spouse can see my name/email upon entering code
            PartnerSyncManager.publishMyProfile(current.myInviteCode, current.myDisplayName, current.myEmail)

            val incomingMatch = PartnerSyncManager.checkForIncomingMatch(current.myInviteCode)
            if (incomingMatch != null && incomingMatch.partnerCode.isNotBlank()) {
                val matched = current.copy(
                    isMatched = true,
                    partnerDisplayName = incomingMatch.partnerName.ifBlank { "Eşim / Namaz Arkadaşım" },
                    partnerEmail = incomingMatch.partnerEmail,
                    partnerInviteCode = PartnerSyncManager.formatDisplayCode(incomingMatch.partnerCode),
                    lastSyncTime = System.currentTimeMillis()
                )
                prayerDao.savePartnerInfo(matched)

                // Show notification that match happened!
                if (context != null) {
                    NotificationHelper.showPartnerAlertNotification(
                        context,
                        matched.partnerDisplayName,
                        "Eşleşme sağlandı! Artık namaz ve kaza durumlarınızı anlık paylaşabilirsiniz. 🤝"
                    )
                }

                // Immediately broadcast my status back to spouse
                broadcastCurrentStatusToPartner(todayDate = today)
                return matched
            }
            return current
        }

        // 2. If MATCHED: Fetch updates, events and check unmatch in single call
        val syncResult = PartnerSyncManager.fetchPartnerUpdatesAndEvents(
            codeA = current.myInviteCode,
            codeB = current.partnerInviteCode,
            partnerCode = current.partnerInviteCode,
            myCode = current.myInviteCode,
            todayDate = today,
            lastProcessedEventTime = lastProcessedEventTime
        )

        // Handle unmatch from partner
        if (syncResult.partnerUnmatched) {
            val unmatchEntity = current.copy(
                isMatched = false,
                partnerDisplayName = "",
                partnerEmail = "",
                partnerInviteCode = "",
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE",
                partnerKazaCompleted = 0,
                partnerKazaOwed = 0
            )
            prayerDao.savePartnerInfo(unmatchEntity)
            return unmatchEntity
        }

        // Trigger notifications for new events from spouse!
        if (syncResult.newEvents.isNotEmpty()) {
            val validEvents = syncResult.newEvents.filter { it.timestamp > lastProcessedEventTime }
            if (validEvents.isNotEmpty()) {
                // Group by prayerName so only the latest status per prayer is notified (prevents both kıldı & kılmadı)
                val latestPerPrayer = validEvents
                    .groupBy { it.prayerName.ifBlank { it.eventType } }
                    .mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }
                    .values
                    .filterNotNull()
                    .sortedBy { it.timestamp }

                for (event in latestPerPrayer) {
                    if (event.timestamp > lastProcessedEventTime) {
                        lastProcessedEventTime = event.timestamp
                    }
                    if (context != null) {
                        val sanitized = event.actionText
                            .replace("kazaya bıraktı", "kılmadı")
                            .replace("Kazaya bıraktı", "kılmadı")
                        NotificationHelper.showPartnerAlertNotification(
                            context = context,
                            partnerName = current.partnerDisplayName.ifBlank { event.senderName.ifBlank { "Eşiniz" } },
                            actionText = sanitized
                        )
                    }
                }
            }
        }

        // Update partner prayer status in Room
        if (syncResult.latestStatus != null) {
            val update = syncResult.latestStatus
            val updated = current.copy(
                lastSyncTime = System.currentTimeMillis(),
                partnerDisplayName = if (current.partnerDisplayName.isNotBlank()) current.partnerDisplayName else update.senderName.ifBlank { "Eşim" },
                partnerEmail = if (update.senderEmail.isNotBlank()) update.senderEmail else current.partnerEmail,
                partnerFajr = update.fajr,
                partnerDhuhr = update.dhuhr,
                partnerAsr = update.asr,
                partnerMaghrib = update.maghrib,
                partnerIsha = update.isha,
                partnerKazaCompleted = update.kazaCompleted,
                partnerKazaOwed = update.kazaOwed
            )
            prayerDao.savePartnerInfo(updated)
            return updated
        }

        return current
    }

    suspend fun matchPartner(code: String, partnerName: String = "", partnerEmail: String = ""): Boolean {
        var current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        if (current.myInviteCode.isBlank() || current.myInviteCode == "ARK-8521") {
            current = ensureUniqueInviteCode()
        }

        val targetCode = PartnerSyncManager.normalizeCode(code)
        val myCleanCode = PartnerSyncManager.normalizeCode(current.myInviteCode)
        if (targetCode.isBlank() || targetCode == myCleanCode) {
            return false
        }

        val displayTargetCode = PartnerSyncManager.formatDisplayCode(code)

        // Look up partner's profile from cloud
        val cloudProfile = PartnerSyncManager.lookupProfileByCode(targetCode)

        val determinedName = when {
            partnerName.isNotBlank() -> partnerName.trim()
            !cloudProfile?.first.isNullOrBlank() -> cloudProfile!!.first.trim()
            !cloudProfile?.second.isNullOrBlank() -> cloudProfile!!.second.substringBefore("@")
            else -> "Eşim / Namaz Arkadaşım"
        }

        val determinedEmail = when {
            !cloudProfile?.second.isNullOrBlank() -> cloudProfile!!.second.trim()
            partnerEmail.isNotBlank() -> partnerEmail.trim()
            else -> ""
        }

        // Send claim to partner's inbox so they get matched automatically
        PartnerSyncManager.sendPairClaim(
            targetCode = displayTargetCode,
            myCode = current.myInviteCode,
            myName = current.myDisplayName.ifBlank { "Namaz Arkadaşım" },
            myEmail = current.myEmail
        )

        val updated = current.copy(
            isMatched = true,
            partnerDisplayName = determinedName,
            partnerEmail = determinedEmail,
            partnerInviteCode = displayTargetCode,
            lastSyncTime = System.currentTimeMillis()
        )
        prayerDao.savePartnerInfo(updated)

        // Immediately broadcast our status to spouse
        broadcastCurrentStatusToPartner()
        return true
    }

    suspend fun unmatchPartner() {
        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        if (current.isMatched && current.partnerInviteCode.isNotBlank()) {
            PartnerSyncManager.broadcastUnmatch(
                codeA = current.myInviteCode,
                codeB = current.partnerInviteCode,
                myCode = current.myInviteCode
            )
        }
        prayerDao.savePartnerInfo(
            current.copy(
                isMatched = false,
                partnerDisplayName = "",
                partnerEmail = "",
                partnerInviteCode = "",
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE",
                partnerKazaCompleted = 0,
                partnerKazaOwed = 0
            )
        )
    }

    suspend fun updatePartnerName(name: String) {
        val current = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        prayerDao.savePartnerInfo(current.copy(partnerDisplayName = name.trim()))
    }

    // Settings & Login
    fun getSettings(): Flow<AppSettingsEntity?> {
        return prayerDao.getSettings()
    }

    suspend fun saveSettings(settings: AppSettingsEntity) {
        prayerDao.saveSettings(settings)
    }

    suspend fun loginUser(name: String, email: String) {
        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val updated = current.copy(
            userName = name.trim(),
            userEmail = email.trim(),
            isLoggedIn = true
        )
        prayerDao.saveSettings(updated)

        // Also update myDisplayName and myEmail in partner_info so partner sees it
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val updatedPartner = partner.copy(
            myDisplayName = name.trim(),
            myEmail = email.trim()
        )
        prayerDao.savePartnerInfo(updatedPartner)

        // Publish my profile to cloud
        PartnerSyncManager.publishMyProfile(
            myCode = updatedPartner.myInviteCode,
            name = name.trim(),
            email = email.trim()
        )
    }

    suspend fun updateMyProfileName(newName: String) {
        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val updated = current.copy(
            userName = newName.trim(),
            isLoggedIn = true
        )
        prayerDao.saveSettings(updated)

        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val updatedPartner = partner.copy(myDisplayName = newName.trim())
        prayerDao.savePartnerInfo(updatedPartner)

        PartnerSyncManager.publishMyProfile(
            myCode = updatedPartner.myInviteCode,
            name = newName.trim(),
            email = updatedPartner.myEmail
        )
    }

    suspend fun clearAllPrayerRecords() {
        prayerDao.clearAllDailyPrayers()
    }

    suspend fun logoutUser() {
        val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        prayerDao.saveSettings(
            current.copy(
                isLoggedIn = false
            )
        )
    }
}
