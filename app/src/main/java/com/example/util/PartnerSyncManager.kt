package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PartnerPrayerUpdate(
    val senderCode: String,
    val senderName: String,
    val senderEmail: String,
    val date: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val kazaCompleted: Int,
    val kazaOwed: Int,
    val timestamp: Long
)

data class PartnerRemoteEvent(
    val senderCode: String,
    val senderName: String,
    val eventType: String, // "PRAYED", "MISSED", "KAZA"
    val prayerName: String,
    val actionText: String,
    val timestamp: Long
)

data class IncomingMatchInfo(
    val partnerCode: String,
    val partnerName: String,
    val partnerEmail: String
)

data class SyncResult(
    val latestStatus: PartnerPrayerUpdate?,
    val yesterdayStatus: PartnerPrayerUpdate? = null,
    val newEvents: List<PartnerRemoteEvent>,
    val partnerUnmatched: Boolean
)

object PartnerSyncManager {

    private const val TAG = "PartnerSyncManager"
    private const val BASE_URL = "https://ntfy.sh"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    fun normalizeCode(code: String): String {
        val clean = code.trim().filter { it.isLetterOrDigit() }.uppercase()
        return if (clean.startsWith("ARK")) clean else "ARK$clean"
    }

    fun formatDisplayCode(code: String): String {
        val norm = normalizeCode(code)
        return if (norm.startsWith("ARK") && norm.length > 3) {
            "ARK-" + norm.substring(3)
        } else {
            norm
        }
    }

    fun getCanonicalPairTopic(codeA: String, codeB: String): String {
        val a = normalizeCode(codeA)
        val b = normalizeCode(codeB)
        val sorted = if (a < b) "${a}_${b}" else "${b}_${a}"
        return "na_pair_$sorted"
    }

    /**
     * Publishes my profile so that when someone enters my code, they can see my name and email.
     */
    suspend fun publishMyProfile(myCode: String, name: String, email: String) = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(myCode)
        if (cleaned.isBlank()) return@withContext

        try {
            val json = JSONObject().apply {
                put("type", "USER_PROFILE")
                put("inviteCode", formatDisplayCode(myCode))
                put("displayName", name.trim())
                put("email", email.trim())
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/na_usr_$cleaned")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to publish profile: ${e.message}")
        }
    }

    /**
     * Looks up user profile by invite code.
     */
    suspend fun lookupProfileByCode(targetCode: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(targetCode)
        if (cleaned.isBlank()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$BASE_URL/na_usr_$cleaned/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var lastProfile: Pair<String, String>? = null
                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                if (msgJson.optString("type") == "USER_PROFILE") {
                                    val name = msgJson.optString("displayName", "")
                                    val email = msgJson.optString("email", "")
                                    lastProfile = Pair(name, email)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                lastProfile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to lookup profile: ${e.message}")
            null
        }
    }

    /**
     * Called by User 2 when they enter User 1's code.
     * User 2 sends a PAIR_CLAIMED notification directly to User 1's inbox.
     */
    suspend fun sendPairClaim(
        targetCode: String,
        myCode: String,
        myName: String,
        myEmail: String
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanedTarget = normalizeCode(targetCode)
        if (cleanedTarget.isBlank()) return@withContext false

        try {
            val json = JSONObject().apply {
                put("type", "PAIR_CLAIMED")
                put("partnerInviteCode", formatDisplayCode(targetCode))
                put("claimerCode", formatDisplayCode(myCode))
                put("claimerName", myName.trim())
                put("claimerEmail", myEmail.trim())
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/na_inv_$cleanedTarget")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { resp ->
                resp.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send pair claim: ${e.message}")
            false
        }
    }

    /**
     * Called by User 1 to check if someone claimed their invite code.
     * When found, User 1 is automatically paired without ever entering a code!
     */
    suspend fun checkForIncomingMatch(myCode: String): IncomingMatchInfo? = withContext(Dispatchers.IO) {
        val cleaned = normalizeCode(myCode)
        if (cleaned.isBlank()) return@withContext null

        try {
            val request = Request.Builder()
                .url("$BASE_URL/na_inv_$cleaned/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null

                var lastClaim: IncomingMatchInfo? = null
                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                if (msgJson.optString("type") == "PAIR_CLAIMED") {
                                    val claimerCode = msgJson.optString("claimerCode", "")
                                    val claimerName = msgJson.optString("claimerName", "")
                                    val claimerEmail = msgJson.optString("claimerEmail", "")
                                    if (claimerCode.isNotBlank()) {
                                        lastClaim = IncomingMatchInfo(
                                            partnerCode = formatDisplayCode(claimerCode),
                                            partnerName = claimerName,
                                            partnerEmail = claimerEmail
                                        )
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
                lastClaim
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check incoming match: ${e.message}")
            null
        }
    }

    /**
     * Broadcasts a real-time prayer event (e.g. "Sabah namazını kıldı") so spouse receives an instant notification.
     */
    suspend fun broadcastPartnerEvent(
        codeA: String,
        codeB: String,
        myCode: String,
        myName: String,
        eventType: String,
        prayerName: String,
        actionText: String
    ) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        try {
            val json = JSONObject().apply {
                put("type", "PARTNER_EVENT")
                put("senderCode", normalizeCode(myCode))
                put("senderName", myName.trim())
                put("eventType", eventType)
                put("prayerName", prayerName)
                put("actionText", actionText)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast partner event: ${e.message}")
        }
    }

    /**
     * Broadcasts my current complete prayer & kaza status to the shared pair topic.
     */
    suspend fun broadcastPrayerStatus(
        codeA: String,
        codeB: String,
        myCode: String,
        myName: String,
        myEmail: String,
        date: String,
        fajr: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String,
        kazaCompleted: Int,
        kazaOwed: Int
    ) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        try {
            val json = JSONObject().apply {
                put("type", "PRAYER_UPDATE")
                put("senderCode", normalizeCode(myCode))
                put("senderName", myName.trim())
                put("senderEmail", myEmail.trim())
                put("date", date)
                put("fajr", fajr)
                put("dhuhr", dhuhr)
                put("asr", asr)
                put("maghrib", maghrib)
                put("isha", isha)
                put("kazaCompleted", kazaCompleted)
                put("kazaOwed", kazaOwed)
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast prayer status: ${e.message}")
        }
    }

    /**
     * Fetches both the latest prayer status and any new events sent by partner since lastTimestamp.
     */
    suspend fun fetchPartnerUpdatesAndEvents(
        codeA: String,
        codeB: String,
        partnerCode: String,
        myCode: String,
        todayDate: String,
        lastProcessedEventTime: Long
    ): SyncResult = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        val cleanPartner = normalizeCode(partnerCode)
        val cleanMyCode = normalizeCode(myCode)

        try {
            val request = Request.Builder()
                .url("$BASE_URL/$topic/json?poll=1")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext SyncResult(null, null, emptyList(), false)
                val body = response.body?.string() ?: return@withContext SyncResult(null, null, emptyList(), false)

                var latestStatus: PartnerPrayerUpdate? = null
                var yesterdayStatus: PartnerPrayerUpdate? = null
                val newEvents = mutableListOf<PartnerRemoteEvent>()
                var unmatchDetected = false

                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val yesterdayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(yesterdayCal.time)

                BufferedReader(StringReader(body)).useLines { lines ->
                    for (line in lines) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message", "")
                                val msgJson = JSONObject(msgStr)
                                val type = msgJson.optString("type", "")
                                val sender = normalizeCode(msgJson.optString("senderCode", ""))
                                val timestamp = msgJson.optLong("timestamp", 0L)

                                when (type) {
                                    "PRAYER_UPDATE" -> {
                                        // Take updates from partner
                                        if (sender == cleanPartner) {
                                            val date = msgJson.optString("date", "")
                                            if (date == todayDate) {
                                                if (latestStatus == null || timestamp >= latestStatus!!.timestamp) {
                                                    latestStatus = PartnerPrayerUpdate(
                                                        senderCode = formatDisplayCode(sender),
                                                        senderName = msgJson.optString("senderName", ""),
                                                        senderEmail = msgJson.optString("senderEmail", ""),
                                                        date = date,
                                                        fajr = msgJson.optString("fajr", "NONE"),
                                                        dhuhr = msgJson.optString("dhuhr", "NONE"),
                                                        asr = msgJson.optString("asr", "NONE"),
                                                        maghrib = msgJson.optString("maghrib", "NONE"),
                                                        isha = msgJson.optString("isha", "NONE"),
                                                        kazaCompleted = msgJson.optInt("kazaCompleted", 0),
                                                        kazaOwed = msgJson.optInt("kazaOwed", 0),
                                                        timestamp = timestamp
                                                    )
                                                }
                                            } else if (date == yesterdayDate) {
                                                if (yesterdayStatus == null || timestamp >= yesterdayStatus!!.timestamp) {
                                                    yesterdayStatus = PartnerPrayerUpdate(
                                                        senderCode = formatDisplayCode(sender),
                                                        senderName = msgJson.optString("senderName", ""),
                                                        senderEmail = msgJson.optString("senderEmail", ""),
                                                        date = date,
                                                        fajr = msgJson.optString("fajr", "NONE"),
                                                        dhuhr = msgJson.optString("dhuhr", "NONE"),
                                                        asr = msgJson.optString("asr", "NONE"),
                                                        maghrib = msgJson.optString("maghrib", "NONE"),
                                                        isha = msgJson.optString("isha", "NONE"),
                                                        kazaCompleted = msgJson.optInt("kazaCompleted", 0),
                                                        kazaOwed = msgJson.optInt("kazaOwed", 0),
                                                        timestamp = timestamp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "PARTNER_EVENT" -> {
                                        // New events from partner (not from me!)
                                        if (sender != cleanMyCode && timestamp > lastProcessedEventTime) {
                                            val rawAction = msgJson.optString("actionText", "")
                                            val cleanAction = rawAction
                                                .replace("kazaya bıraktı", "kılmadı")
                                                .replace("Kazaya bıraktı", "kılmadı")
                                            newEvents.add(
                                                PartnerRemoteEvent(
                                                    senderCode = formatDisplayCode(sender),
                                                    senderName = msgJson.optString("senderName", ""),
                                                    eventType = msgJson.optString("eventType", ""),
                                                    prayerName = msgJson.optString("prayerName", ""),
                                                    actionText = cleanAction,
                                                    timestamp = timestamp
                                                )
                                            )
                                        }
                                    }
                                    "UNMATCH" -> {
                                        if (sender == cleanPartner) {
                                            unmatchDetected = true
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }

                SyncResult(
                    latestStatus = latestStatus,
                    yesterdayStatus = yesterdayStatus,
                    newEvents = newEvents,
                    partnerUnmatched = unmatchDetected
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync with partner: ${e.message}")
            SyncResult(null, null, emptyList(), false)
        }
    }

    /**
     * Broadcasts unmatch event so the partner can also disconnect.
     */
    suspend fun broadcastUnmatch(codeA: String, codeB: String, myCode: String) = withContext(Dispatchers.IO) {
        val topic = getCanonicalPairTopic(codeA, codeB)
        try {
            val json = JSONObject().apply {
                put("type", "UNMATCH")
                put("senderCode", normalizeCode(myCode))
                put("timestamp", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$BASE_URL/$topic")
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast unmatch: ${e.message}")
        }
    }
}
