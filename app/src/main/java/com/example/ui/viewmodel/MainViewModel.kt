package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import com.example.data.model.DailyPrayerTimes
import com.example.data.repository.PrayerRepository
import com.example.data.network.DiyanetPrayerService
import com.example.data.util.DailyContentProvider
import com.example.data.util.PrayerCalculator
import com.example.util.GpsLocationHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrayerRepository

    // Date state
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("d MMMM EEEE", Locale("tr"))

    private val _selectedDateStr = MutableStateFlow(dateFormat.format(Date()))
    val selectedDateStr: StateFlow<String> = _selectedDateStr.asStateFlow()

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())

    private val _displayDateTitle = MutableStateFlow("Bugün, " + displayFormat.format(Date()))
    val displayDateTitle: StateFlow<String> = _displayDateTitle.asStateFlow()

    private val _isViewingToday = MutableStateFlow(true)
    val isViewingToday: StateFlow<Boolean> = _isViewingToday.asStateFlow()

    // Database Flows
    val settings: StateFlow<AppSettingsEntity>
    val partnerInfo: StateFlow<PartnerInfoEntity>
    val kazaPrayers: StateFlow<List<KazaPrayerEntity>>
    val recentDailyPrayers: StateFlow<List<DailyPrayerEntity>>

    // Today's Prayer flow
    private val _todayPrayer = MutableStateFlow(DailyPrayerEntity(date = dateFormat.format(Date())))
    val todayPrayer: StateFlow<DailyPrayerEntity> = _todayPrayer.asStateFlow()

    // Calculated Prayer Times for display
    private val _prayerTimes = MutableStateFlow(PrayerCalculator.calculatePrayerTimes(41.0082, 28.9784))
    val prayerTimes: StateFlow<DailyPrayerTimes> = _prayerTimes.asStateFlow()

    // Qibla direction bearing (degrees)
    private val _qiblaBearing = MutableStateFlow(PrayerCalculator.calculateQiblaBearing(41.0082, 28.9784))
    val qiblaBearing: StateFlow<Double> = _qiblaBearing.asStateFlow()

    // Partner refresh animation flag
    private val _isRefreshingPartner = MutableStateFlow(false)
    val isRefreshingPartner: StateFlow<Boolean> = _isRefreshingPartner.asStateFlow()

    // Status snackbar / banner message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // GPS location loading flag
    private val _isUpdatingLocation = MutableStateFlow(false)
    val isUpdatingLocation: StateFlow<Boolean> = _isUpdatingLocation.asStateFlow()

    // Pending email invites received from friends
    data class EmailInvite(
        val email: String,
        val inviteCode: String,
        val senderName: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _pendingEmailInvites = MutableStateFlow<List<EmailInvite>>(emptyList())
    val pendingEmailInvites: StateFlow<List<EmailInvite>> = _pendingEmailInvites.asStateFlow()

    private val _notificationDayCounter = MutableStateFlow(1)
    val notificationDayCounter: StateFlow<Int> = _notificationDayCounter.asStateFlow()

    private var lastNotifiedPrayerName = ""

    // In-App Updater States
    private val _availableUpdate = MutableStateFlow<com.example.util.AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<com.example.util.AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingForUpdate = MutableStateFlow(false)
    val isCheckingForUpdate: StateFlow<Boolean> = _isCheckingForUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _updateDownloadError = MutableStateFlow<String?>(null)
    val updateDownloadError: StateFlow<String?> = _updateDownloadError.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PrayerRepository(database.prayerDao(), application)

        NotificationHelper.createNotificationChannels(application)

        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureUniqueInviteCode()
        }

        settings = repository.getSettings()
            .map { it ?: AppSettingsEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppSettingsEntity()
            )

        partnerInfo = repository.getPartnerInfo()
            .map { it ?: PartnerInfoEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = PartnerInfoEntity()
            )

        kazaPrayers = repository.getAllKazaPrayers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        recentDailyPrayers = repository.getRecentDailyPrayers()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

        loadPrayerForCurrentDate()
        recalculateTimes()

        // Deliver approaching prayer notification with spiritual note to top bar
        viewModelScope.launch {
            delay(1500)
            val next = _prayerTimes.value
            val remainingMins = (next.nextPrayerRemainingMillis / (60 * 1000)).coerceAtLeast(15)
            NotificationHelper.showPrayerApproachingNotification(
                context = getApplication<Application>(),
                prayerName = next.nextPrayerName,
                minutesRemaining = remainingMins,
                customDay = _notificationDayCounter.value
            )
        }

        // Timer loop for remaining prayer time updates & approaching notification
        viewModelScope.launch {
            while (true) {
                delay(15000)
                recalculateTimes()
                checkAndNotifyApproachingPrayer()
            }
        }

        // Periodic partner cloud sync loop (every 5 seconds for real-time status and spouse notifications)
        viewModelScope.launch(Dispatchers.IO) {
            delay(1500)
            while (true) {
                try {
                    repository.refreshPartnerData()
                } catch (_: Exception) { }
                delay(5000)
            }
        }

        // Automatic GitHub Release Update Check (at startup and every 30 minutes)
        viewModelScope.launch(Dispatchers.IO) {
            delay(3500)
            checkForAppUpdate(silent = true)
            while (true) {
                delay(30 * 60 * 1000L)
                checkForAppUpdate(silent = true)
            }
        }
    }

    private fun checkAndNotifyApproachingPrayer() {
        val next = _prayerTimes.value
        val remainingMins = next.nextPrayerRemainingMillis / (60 * 1000)
        if (remainingMins in 1..45 && lastNotifiedPrayerName != next.nextPrayerName) {
            lastNotifiedPrayerName = next.nextPrayerName
            NotificationHelper.showPrayerApproachingNotification(
                context = getApplication<Application>(),
                prayerName = next.nextPrayerName,
                minutesRemaining = remainingMins,
                customDay = _notificationDayCounter.value
            )
        }
    }

    fun getDisplayDateTitle(): String {
        val todayStr = dateFormat.format(Date())
        val selected = _selectedDateStr.value
        return if (selected == todayStr) {
            "Bugün, " + displayFormat.format(_currentCalendar.value.time)
        } else {
            displayFormat.format(_currentCalendar.value.time)
        }
    }

    fun isToday(): Boolean {
        return _selectedDateStr.value == dateFormat.format(Date())
    }

    fun changeDateOffset(offsetDays: Int) {
        val newCal = Calendar.getInstance().apply {
            time = _currentCalendar.value.time
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        _currentCalendar.value = newCal
        _selectedDateStr.value = dateFormat.format(newCal.time)
        _displayDateTitle.value = getDisplayDateTitle()
        _isViewingToday.value = isToday()
        loadPrayerForCurrentDate()
        recalculateTimes()
    }

    fun resetToToday() {
        _currentCalendar.value = Calendar.getInstance()
        _selectedDateStr.value = dateFormat.format(Date())
        _displayDateTitle.value = getDisplayDateTitle()
        _isViewingToday.value = true
        loadPrayerForCurrentDate()
        recalculateTimes()
    }

    fun resetAllPrayerData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPrayerRecords()
            resetToToday()
            _statusMessage.value = "Namaz kayıtları sıfırlandı. Temiz şekilde yeniden girebilirsiniz."
            delay(3000)
            _statusMessage.value = null
        }
    }

    private fun loadPrayerForCurrentDate() {
        val dateStr = _selectedDateStr.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPrayerForDate(dateStr).collectLatest { prayer ->
                _todayPrayer.value = prayer ?: DailyPrayerEntity(date = dateStr)
            }
        }
    }

    fun setPrayerStatus(prayerType: String, status: String) {
        val dateStr = _selectedDateStr.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.setPrayerStatus(dateStr, prayerType, status)
            if (status == "PRAYED") {
                val trName = when (prayerType.uppercase()) {
                    "FAJR" -> "Sabah"
                    "DHUHR" -> "Öğle"
                    "ASR" -> "İkindi"
                    "MAGHRIB" -> "Akşam"
                    "ISHA" -> "Yatsı"
                    else -> prayerType
                }
                if (partnerInfo.value.isMatched) {
                    _statusMessage.value = "$trName namazı kaydedildi ve eşinize bildirildi! 🤲"
                } else {
                    _statusMessage.value = "$trName namazı kılındı olarak kaydedildi! Allah kabul etsin."
                }
                delay(2500)
                _statusMessage.value = null
            }
        }
    }

    // Kaza actions
    fun adjustKaza(type: String, delta: Int) {
        adjustKazaOwed(type, delta)
    }

    fun adjustKazaOwed(type: String, delta: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.adjustKazaOwed(type, delta)
        }
    }

    fun completeOneKaza(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeOneKaza(type)
            val trName = when (type.uppercase()) {
                "FAJR" -> "Sabah"
                "DHUHR" -> "Öğle"
                "ASR" -> "İkindi"
                "MAGHRIB" -> "Akşam"
                "ISHA" -> "Yatsı"
                "WITR" -> "Vitir"
                else -> type
            }
            if (partnerInfo.value.isMatched) {
                _statusMessage.value = "1 adet $trName kazası kılındı ve eşinize bildirildi! 🤲"
            } else {
                _statusMessage.value = "1 adet $trName kazası kılındı olarak kaydedildi!"
            }
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun addBulkKaza(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBulkKaza(amount)
            _statusMessage.value = "Her vakte +$amount kaza namazı borcu eklendi."
            delay(3000)
            _statusMessage.value = null
        }
    }

    // Partner actions
    fun refreshPartnerData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingPartner.value = true
            val updated = repository.refreshPartnerData()
            _isRefreshingPartner.value = false
            if (updated.isMatched) {
                _statusMessage.value = "${updated.partnerDisplayName} ile namaz durumları güncellendi!"
            } else {
                _statusMessage.value = "Davet kodu henüz eşleşmedi. Davet edilen kişinin kodu girmesi bekleniyor..."
            }
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun matchPartnerWithCode(code: String, partnerName: String = "", partnerEmail: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingPartner.value = true
            val success = repository.matchPartner(code, partnerName, partnerEmail)
            _isRefreshingPartner.value = false
            if (success) {
                val current = partnerInfo.value
                val name = if (current.partnerDisplayName.isNotBlank()) current.partnerDisplayName else "Namaz arkadaşınız"
                _statusMessage.value = "$name ile başarıyla eşleştiniz!"
            } else {
                _statusMessage.value = "Davet kodu doğrulanamadı. Lütfen kontrol edin."
            }
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun togglePrayerReminders(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(prayerRemindersEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun togglePartnerAlerts(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(partnerAlertsEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun toggleMotivationNotification(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(motivationNotificationEnabled = enabled)
            repository.saveSettings(updated)
        }
    }

    fun unmatchPartner() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unmatchPartner()
            _statusMessage.value = "Partner eşleşmesi sonlandırıldı."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun updatePartnerName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePartnerName(name)
            _statusMessage.value = "Eşinizin ismi güncellendi: $name"
            delay(2500)
            _statusMessage.value = null
        }
    }

    // User Login / Profile
    fun loginUser(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loginUser(name, email)
            _statusMessage.value = "Hoş geldiniz, $name! Profiliniz güncellendi."
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun loginWithGoogleBrowser(email: String = "e.bilkil5391@gmail.com", name: String = "") {
        val cleanName = if (name.isNotBlank() && name != "Mümin") {
            name.trim()
        } else {
            val prefix = email.substringBefore("@").replace(".", " ").replace("_", " ")
            val words = prefix.filter { it.isLetter() || it.isWhitespace() }.split(" ").filter { it.isNotBlank() }
            if (words.isNotEmpty()) words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } else "Mümin"
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.loginUser(cleanName, email.trim())
            _statusMessage.value = "Hoş geldiniz, $cleanName! ($email)"
            delay(3500)
            _statusMessage.value = null
        }
    }

    fun updateMyProfileName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMyProfileName(newName)
            _statusMessage.value = "Profil isminiz güncellendi: $newName"
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun logoutUser() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logoutUser()
            _statusMessage.value = "Çıkış yapıldı."
            delay(2500)
            _statusMessage.value = null
        }
    }

    // City & Location
    fun selectCity(city: PrayerCalculator.CityInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(
                cityName = city.name,
                latitude = city.lat,
                longitude = city.lng
            )
            repository.saveSettings(updated)
            recalculateTimes(city.lat, city.lng)
        }
    }

    fun acceptEmailInvite(invite: EmailInvite) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.matchPartner(
                invite.inviteCode,
                invite.senderName.ifBlank { invite.email.substringBefore("@") },
                invite.email
            )
            _pendingEmailInvites.value = _pendingEmailInvites.value.filter { it.inviteCode != invite.inviteCode }
            _statusMessage.value = "${invite.email} ile başarıyla eşleştiniz!"
            delay(3000)
            _statusMessage.value = null
        }
    }

    fun updateLocationGps() {
        viewModelScope.launch {
            _isUpdatingLocation.value = true
            val loc = GpsLocationHelper.getDeviceLocation(getApplication())

            val updated = settings.value.copy(
                cityName = loc.cityName,
                latitude = loc.latitude,
                longitude = loc.longitude
            )
            repository.saveSettings(updated)
            fetchOrCalculatePrayerTimes(loc.latitude, loc.longitude)
            _isUpdatingLocation.value = false
            _statusMessage.value = "Diyanet vakitleri GPS ile güncellendi: ${loc.cityName}"
            delay(3000)
            _statusMessage.value = null
        }
    }

    private fun recalculateTimes(
        lat: Double = settings.value.latitude,
        lng: Double = settings.value.longitude
    ) {
        viewModelScope.launch {
            fetchOrCalculatePrayerTimes(lat, lng)
        }
    }

    private suspend fun fetchOrCalculatePrayerTimes(lat: Double, lng: Double) {
        val diyanetTimes = DiyanetPrayerService.fetchDiyanetPrayerTimes(
            latitude = lat,
            longitude = lng,
            calendar = _currentCalendar.value
        )
        if (diyanetTimes != null) {
            _prayerTimes.value = diyanetTimes
        } else {
            _prayerTimes.value = PrayerCalculator.calculatePrayerTimes(lat, lng, _currentCalendar.value)
        }
        _qiblaBearing.value = PrayerCalculator.calculateQiblaBearing(lat, lng)
    }

    fun updateDriveShareUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = settings.value.copy(driveShareUrl = url.trim())
            repository.saveSettings(updated)
            _statusMessage.value = "Google Drive indirme linkiniz kaydedildi!"
            delay(2500)
            _statusMessage.value = null
        }
    }

    fun saveSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(newSettings)
        }
    }

    // App Updater methods
    fun checkForAppUpdate(silent: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!silent) _isCheckingForUpdate.value = true
            val update = com.example.util.AppUpdateManager.checkForUpdates(
                context = getApplication(),
                notifyIfFound = !silent
            )
            if (!silent) _isCheckingForUpdate.value = false

            if (update != null && update.hasUpdate) {
                val prefs = getApplication<Application>().getSharedPreferences("app_updater_prefs", android.content.Context.MODE_PRIVATE)
                val dismissedTag = prefs.getString("dismissed_update_tag", "")
                if (!silent || dismissedTag != update.latestVersionTag) {
                    _availableUpdate.value = update
                }
                if (!silent) {
                    _statusMessage.value = "Yeni güncelleme bulundu: ${update.latestVersionTag}"
                    delay(3000)
                    _statusMessage.value = null
                }
            } else if (!silent) {
                _statusMessage.value = "Uygulamanız güncel! (Sürüm: ${com.example.BuildConfig.VERSION_NAME})"
                delay(3000)
                _statusMessage.value = null
            }
        }
    }

    fun startApkUpdateDownload() {
        val update = _availableUpdate.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _updateDownloadError.value = null
            _downloadProgress.value = 0.01f

            com.example.util.AppUpdateManager.downloadAndInstallApk(
                context = getApplication(),
                downloadUrl = update.apkDownloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                },
                onComplete = {
                    _downloadProgress.value = null
                },
                onError = { error ->
                    _downloadProgress.value = null
                    _updateDownloadError.value = error
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        val currentTag = _availableUpdate.value?.latestVersionTag
        if (!currentTag.isNullOrBlank()) {
            val prefs = getApplication<Application>().getSharedPreferences("app_updater_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("dismissed_update_tag", currentTag).apply()
        }
        _availableUpdate.value = null
        _downloadProgress.value = null
        _updateDownloadError.value = null
    }
}
