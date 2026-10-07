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
        // If code is default placeholder or blank, generate a truly unique random code with EMB-53 prefix
        if (current.myInviteCode.isBlank() || current.myInviteCode.startsWith("ARK") || current.myInviteCode == "EMB-5385") {
            val randomSuffix = (1000..9999).random()
            val newCode = "EMB-53$randomSuffix"
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
        val rowsAffected = when (prayerType.uppercase()) {
            "FAJR", "SABAH" -> prayerDao.updateFajrStatus(date, status)
            "DHUHR", "OGLE", "ÖĞLE" -> prayerDao.updateDhuhrStatus(date, status)
            "ASR", "IKINDI", "İKİNDİ" -> prayerDao.updateAsrStatus(date, status)
            "MAGHRIB", "AKSAM", "AKŞAM" -> prayerDao.updateMaghribStatus(date, status)
            "ISHA", "YATSI" -> prayerDao.updateIshaStatus(date, status)
            else -> 0
        }

        val updated = if (rowsAffected == 0) {
            val newEntity = when (prayerType.uppercase()) {
                "FAJR", "SABAH" -> DailyPrayerEntity(date = date, fajrStatus = status)
                "DHUHR", "OGLE", "ÖĞLE" -> DailyPrayerEntity(date = date, dhuhrStatus = status)
                "ASR", "IKINDI", "İKİNDİ" -> DailyPrayerEntity(date = date, asrStatus = status)
                "MAGHRIB", "AKSAM", "AKŞAM" -> DailyPrayerEntity(date = date, maghribStatus = status)
                "ISHA", "YATSI" -> DailyPrayerEntity(date = date, ishaStatus = status)
                else -> DailyPrayerEntity(date = date)
            }
            prayerDao.insertOrUpdateDailyPrayer(newEntity)
            newEntity
        } else {
            prayerDao.getPrayerForDateDirect(date) ?: DailyPrayerEntity(date = date)
        }

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
                    delay(5000L) // Namaz arkadaşına 5 saniye sonra gitsin

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
                        val spouseName = partner.myDisplayName.ifBlank { "Arkadaşınız" }
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
        triggerAutoCloudBackup()
    }

    suspend fun completeOneKaza(type: String) {
        prayerDao.completeOneKaza(type.uppercase())
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()

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
                myName = partner.myDisplayName.ifBlank { "Arkadaşınız" },
                eventType = "KAZA",
                prayerName = trName,
                actionText = "1 vakit $trName kaza namazı kıldı. Tebrikler! 🤲"
            )
        }
    }

    suspend fun undoCompleteOneKaza(type: String) {
        prayerDao.undoCompleteOneKaza(type.uppercase())
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()
    }

    suspend fun addBulkKaza(amount: Int) {
        prayerDao.addBulkKaza(amount)
        broadcastCurrentStatusToPartner()
        triggerAutoCloudBackup()
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
        val lastSyncDay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(current.lastSyncTime))
        val isSameDay = (lastSyncDay == today)

        // If today is a new day and partner hasn't posted today's prayers yet, shift yesterday's prayers
        if (!isSameDay && current.partnerFajr != "NONE") {
            current = current.copy(
                partnerYesterdayFajr = current.partnerFajr,
                partnerYesterdayDhuhr = current.partnerDhuhr,
                partnerYesterdayAsr = current.partnerAsr,
                partnerYesterdayMaghrib = current.partnerMaghrib,
                partnerYesterdayIsha = current.partnerIsha,
                partnerFajr = "NONE",
                partnerDhuhr = "NONE",
                partnerAsr = "NONE",
                partnerMaghrib = "NONE",
                partnerIsha = "NONE"
            )
            prayerDao.savePartnerInfo(current)
        }

        // 1. If NOT matched yet: check if someone has claimed my invite code!
        if (!current.isMatched) {
            // Re-publish my profile so spouse can see my name/email upon entering code
            PartnerSyncManager.publishMyProfile(current.myInviteCode, current.myDisplayName, current.myEmail)

            val incomingMatch = PartnerSyncManager.checkForIncomingMatch(current.myInviteCode)
            if (incomingMatch != null && incomingMatch.partnerCode.isNotBlank()) {
                val matched = current.copy(
                    isMatched = true,
                    partnerDisplayName = incomingMatch.partnerName.ifBlank { "Namaz Arkadaşım" },
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
                            partnerName = current.partnerDisplayName.ifBlank { event.senderName.ifBlank { "Arkadaşınız" } },
                            actionText = sanitized
                        )
                    }
                }
            }
        }

        // Update partner prayer status in Room
        val todayUpdate = syncResult.latestStatus
        val yesterdayUpdate = syncResult.yesterdayStatus

        if (todayUpdate != null || yesterdayUpdate != null) {
            val activeUpdate = todayUpdate ?: yesterdayUpdate!!
            val updated = current.copy(
                lastSyncTime = if (activeUpdate.timestamp > 0) activeUpdate.timestamp else System.currentTimeMillis(),
                partnerDisplayName = if (current.partnerDisplayName.isNotBlank()) current.partnerDisplayName else activeUpdate.senderName.ifBlank { "Namaz Arkadaşım" },
                partnerEmail = if (activeUpdate.senderEmail.isNotBlank()) activeUpdate.senderEmail else current.partnerEmail,
                partnerFajr = todayUpdate?.fajr ?: current.partnerFajr,
                partnerDhuhr = todayUpdate?.dhuhr ?: current.partnerDhuhr,
                partnerAsr = todayUpdate?.asr ?: current.partnerAsr,
                partnerMaghrib = todayUpdate?.maghrib ?: current.partnerMaghrib,
                partnerIsha = todayUpdate?.isha ?: current.partnerIsha,
                partnerYesterdayFajr = yesterdayUpdate?.fajr ?: current.partnerYesterdayFajr,
                partnerYesterdayDhuhr = yesterdayUpdate?.dhuhr ?: current.partnerYesterdayDhuhr,
                partnerYesterdayAsr = yesterdayUpdate?.asr ?: current.partnerYesterdayAsr,
                partnerYesterdayMaghrib = yesterdayUpdate?.maghrib ?: current.partnerYesterdayMaghrib,
                partnerYesterdayIsha = yesterdayUpdate?.isha ?: current.partnerYesterdayIsha,
                partnerKazaCompleted = activeUpdate.kazaCompleted,
                partnerKazaOwed = activeUpdate.kazaOwed
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
            else -> "Namaz Arkadaşım"
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

    suspend fun triggerAutoCloudBackup() {
        try {
            val settings = prayerDao.getSettings().firstOrNull() ?: return
            val email = settings.userEmail.trim().lowercase()
            if (!settings.isLoggedIn || email.isBlank()) return

            val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
            val kazas = prayerDao.getAllKazaPrayersDirect()
            val kazasArray = org.json.JSONArray()
            kazas.forEach { k ->
                kazasArray.put(
                    org.json.JSONObject().apply {
                        put("prayerType", k.prayerType)
                        put("owedCount", k.owedCount)
                        put("completedCount", k.completedCount)
                    }
                )
            }

            PartnerSyncManager.backupUserDataToCloud(
                email = email,
                name = settings.userName,
                inviteCode = partner.myInviteCode,
                kazalarJson = kazasArray.toString(),
                isMatched = partner.isMatched,
                partnerCode = partner.partnerInviteCode,
                partnerName = partner.partnerDisplayName
            )
        } catch (_: Exception) { }
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

        // Otomatik Bulut Kurtarma: Yeni telefonda veya yeniden kurulumda Google oturumu açılınca
        // kullanıcının kayıtlı kaza ve aile bilgilerini buluttan otomatik geri yükle
        try {
            val cloudBackup = PartnerSyncManager.restoreUserDataFromCloud(email.trim())
            if (cloudBackup != null) {
                val kazalarStr = cloudBackup.optString("kazalarJson", "[]")
                val kazasArr = org.json.JSONArray(kazalarStr)
                val list = mutableListOf<KazaPrayerEntity>()
                for (i in 0 until kazasArr.length()) {
                    val item = kazasArr.getJSONObject(i)
                    list.add(
                        KazaPrayerEntity(
                            prayerType = item.getString("prayerType"),
                            owedCount = item.optInt("owedCount", 0),
                            completedCount = item.optInt("completedCount", 0)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    prayerDao.insertAllKazaPrayers(list)
                }

                val savedPartnerCode = cloudBackup.optString("partnerCode", "")
                val savedPartnerName = cloudBackup.optString("partnerName", "")
                val savedIsMatched = cloudBackup.optBoolean("isMatched", false)
                if (savedIsMatched && savedPartnerCode.isNotBlank()) {
                    prayerDao.savePartnerInfo(
                        updatedPartner.copy(
                            isMatched = true,
                            partnerInviteCode = savedPartnerCode,
                            partnerDisplayName = savedPartnerName
                        )
                    )
                }
            } else {
                // Eğer bulutta henüz yedek yoksa mevcut verileri ilk kez buluta yedekle
                triggerAutoCloudBackup()
            }
        } catch (_: Exception) { }
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

    // =========================================================================
    // VERİ DIŞA / İÇE AKTARMA (MİGRASYON SİHİRBAZI: Namaz Arkadaşım -> Aile ile Secdeye)
    // =========================================================================
    suspend fun exportDataToJson(): String {
        val settings = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
        val partner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
        val dailyPrayers = prayerDao.getAllDailyPrayersDirect()
        val kazaPrayers = prayerDao.getAllKazaPrayersDirect()

        val root = org.json.JSONObject()
        root.put("sourceApp", "NamazArkadasim")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settingsObj = org.json.JSONObject().apply {
            put("userName", settings.userName)
            put("userEmail", settings.userEmail)
            put("cityName", settings.cityName)
            put("isLoggedIn", settings.isLoggedIn)
        }
        root.put("settings", settingsObj)

        val partnerObj = org.json.JSONObject().apply {
            put("myInviteCode", partner.myInviteCode)
            put("myDisplayName", partner.myDisplayName)
            put("isMatched", partner.isMatched)
            put("partnerDisplayName", partner.partnerDisplayName)
            put("partnerEmail", partner.partnerEmail)
            put("partnerInviteCode", partner.partnerInviteCode)
            put("partnerKazaOwed", partner.partnerKazaOwed)
            put("partnerKazaCompleted", partner.partnerKazaCompleted)
        }
        root.put("partner", partnerObj)

        val prayersArray = org.json.JSONArray()
        dailyPrayers.forEach { p ->
            val pObj = org.json.JSONObject().apply {
                put("date", p.date)
                put("fajrStatus", p.fajrStatus)
                put("dhuhrStatus", p.dhuhrStatus)
                put("asrStatus", p.asrStatus)
                put("maghribStatus", p.maghribStatus)
                put("ishaStatus", p.ishaStatus)
                put("note", p.note)
            }
            prayersArray.put(pObj)
        }
        root.put("dailyPrayers", prayersArray)

        val kazasArray = org.json.JSONArray()
        kazaPrayers.forEach { k ->
            val kObj = org.json.JSONObject().apply {
                put("prayerType", k.prayerType)
                put("owedCount", k.owedCount)
                put("completedCount", k.completedCount)
            }
            kazasArray.put(kObj)
        }
        root.put("kazaPrayers", kazasArray)

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonStr.trim())

            // 1. Ayarları Geri Yükle
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val current = prayerDao.getSettings().firstOrNull() ?: AppSettingsEntity()
                prayerDao.saveSettings(
                    current.copy(
                        userName = sObj.optString("userName", current.userName),
                        userEmail = sObj.optString("userEmail", current.userEmail),
                        cityName = sObj.optString("cityName", current.cityName),
                        isLoggedIn = sObj.optBoolean("isLoggedIn", true)
                    )
                )
            }

            // 2. Partner (Eş) Bilgilerini Geri Yükle ve Aile Havuzu 2. Kişisi Yap
            if (root.has("partner")) {
                val pObj = root.getJSONObject("partner")
                val currentPartner = prayerDao.getPartnerInfo().firstOrNull() ?: PartnerInfoEntity()
                val updatedPartner = currentPartner.copy(
                    myInviteCode = pObj.optString("myInviteCode", currentPartner.myInviteCode),
                    isMatched = pObj.optBoolean("isMatched", currentPartner.isMatched),
                    partnerDisplayName = pObj.optString("partnerDisplayName", currentPartner.partnerDisplayName),
                    partnerEmail = pObj.optString("partnerEmail", currentPartner.partnerEmail),
                    partnerInviteCode = pObj.optString("partnerInviteCode", currentPartner.partnerInviteCode),
                    partnerKazaOwed = pObj.optInt("partnerKazaOwed", currentPartner.partnerKazaOwed),
                    partnerKazaCompleted = pObj.optInt("partnerKazaCompleted", currentPartner.partnerKazaCompleted)
                )
                prayerDao.savePartnerInfo(updatedPartner)
            }

            // 3. Günlük Namaz Kayıtlarını Aktar
            if (root.has("dailyPrayers")) {
                val prayersArr = root.getJSONArray("dailyPrayers")
                val list = mutableListOf<DailyPrayerEntity>()
                for (i in 0 until prayersArr.length()) {
                    val item = prayersArr.getJSONObject(i)
                    list.add(
                        DailyPrayerEntity(
                            date = item.getString("date"),
                            fajrStatus = item.optString("fajrStatus", "NONE"),
                            dhuhrStatus = item.optString("dhuhrStatus", "NONE"),
                            asrStatus = item.optString("asrStatus", "NONE"),
                            maghribStatus = item.optString("maghribStatus", "NONE"),
                            ishaStatus = item.optString("ishaStatus", "NONE"),
                            note = item.optString("note", "")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    prayerDao.insertAllDailyPrayers(list)
                }
            }

            // 4. Kaza Namazlarını Aktar
            if (root.has("kazaPrayers")) {
                val kazasArr = root.getJSONArray("kazaPrayers")
                val kList = mutableListOf<KazaPrayerEntity>()
                for (i in 0 until kazasArr.length()) {
                    val item = kazasArr.getJSONObject(i)
                    kList.add(
                        KazaPrayerEntity(
                            prayerType = item.getString("prayerType"),
                            owedCount = item.optInt("owedCount", 0),
                            completedCount = item.optInt("completedCount", 0)
                        )
                    )
                }
                if (kList.isNotEmpty()) {
                    prayerDao.insertAllKazaPrayers(kList)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
