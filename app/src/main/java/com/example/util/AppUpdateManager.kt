package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeFormatted: String,
    val publishedAt: String,
    val assetUpdatedAtMillis: Long
)

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/bilkil53/aile-ile-secdeye/releases/latest"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_NOTIFIED_TAG = "last_notified_version_tag"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(context: Context, notifyIfFound: Boolean = true): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AileIleSecdeye-Android")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check failed with code: ${response.code}")
                    return@withContext null
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", "Aile ile Secdeye Güncellemesi")
                val releaseNotes = json.optString("body", "Yeni özellikler, performans geliştirmeleri ve aile havuzu güncellemeleri içerir.")
                val publishedAt = json.optString("published_at", "")

                var downloadUrl = ""
                var apkSizeBytes = 0L
                var assetUpdatedAtMillis = 0L

                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkSizeBytes = asset.optLong("size", 0L)
                            val updatedAtStr = asset.optString("updated_at", "")
                            assetUpdatedAtMillis = parseIsoDateToMillis(updatedAtStr)
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank()) {
                    downloadUrl = "https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk"
                }

                val formattedSize = if (apkSizeBytes > 0) {
                    String.format(Locale.getDefault(), "%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
                } else {
                    "25 MB"
                }

                // Check version differences (SemVer: only prompt if GitHub tag is strictly higher than installed app)
                val currentVersion = BuildConfig.VERSION_NAME
                val currentVersionTag = if (currentVersion.startsWith("v", ignoreCase = true)) currentVersion else "v$currentVersion"

                val isNewerVersionTag = tagName.isNotBlank() && isTagHigher(tagName, currentVersionTag)

                val hasUpdate = isNewerVersionTag

                val updateInfo = AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionTag = if (tagName.isNotBlank()) tagName else "v$currentVersion",
                    releaseTitle = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = downloadUrl,
                    apkSizeFormatted = formattedSize,
                    publishedAt = publishedAt,
                    assetUpdatedAtMillis = assetUpdatedAtMillis
                )

                if (hasUpdate && notifyIfFound) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_TAG, "")

                    if (lastNotified != tagName) {
                        prefs.edit().putString(KEY_LAST_NOTIFIED_TAG, tagName).apply()
                        NotificationHelper.showAppUpdateNotification(
                            context = context,
                            versionTag = updateInfo.latestVersionTag,
                            releaseNotes = releaseNotes.take(150)
                        )
                    }
                }

                updateInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}")
            null
        }
    }

    private fun parseIsoDateToMillis(isoDateStr: String): Long {
        if (isoDateStr.isBlank()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoDateStr)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun isTagHigher(latestTag: String, currentTag: String): Boolean {
        val cleanLatest = latestTag.trim()
            .removePrefix("v.")
            .removePrefix("V.")
            .removePrefix("v")
            .removePrefix("V")
            .trim()

        val cleanCurrent = currentTag.trim()
            .removePrefix("v.")
            .removePrefix("V.")
            .removePrefix("v")
            .removePrefix("V")
            .trim()

        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) {
            return false
        }

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Downloads the APK file from GitHub and triggers direct Android package installation.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "NamazArkadasim-Android")
                .get()
                .build()

            val downloadClient = httpClient.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("İndirme başarısız oldu (Hata kodu: ${response.code})")
                }
                return@withContext
            }

            val body = response.body
            if (body == null) {
                withContext(Dispatchers.Main) {
                    onError("İndirilecek APK dosyası bulunamadı.")
                }
                return@withContext
            }

            val totalBytes = body.contentLength()
            val targetDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(targetDir, "Namaz_Arkadasim_Update.apk")

            body.byteStream().use { inputStream ->
                FileOutputStream(apkFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val progress = bytesCopied.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(1.0f)
                onComplete(apkFile)
                triggerInstall(context, apkFile, downloadUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onError("İndirme sırasında hata oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}")
            }
        }
    }

    /**
     * Launches the Android native package installer for the downloaded APK.
     */
    fun triggerInstall(context: Context, apkFile: File, fallbackUrl: String) {
        try {
            // Check unknown sources installation permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed, opening browser: ${e.message}")
            openInBrowser(context, fallbackUrl)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) { }
    }
}
