package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    // Daily prayers
    @Query("SELECT * FROM daily_prayers WHERE date = :date LIMIT 1")
    fun getPrayerForDate(date: String): Flow<DailyPrayerEntity?>

    @Query("SELECT * FROM daily_prayers WHERE date = :date LIMIT 1")
    suspend fun getPrayerForDateDirect(date: String): DailyPrayerEntity?

    @Query("SELECT * FROM daily_prayers ORDER BY date DESC LIMIT 30")
    fun getRecentDailyPrayers(): Flow<List<DailyPrayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyPrayer(prayer: DailyPrayerEntity)

    @Query("UPDATE daily_prayers SET fajrStatus = :status, updatedAt = :now WHERE date = :date")
    suspend fun updateFajrStatus(date: String, status: String, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE daily_prayers SET dhuhrStatus = :status, updatedAt = :now WHERE date = :date")
    suspend fun updateDhuhrStatus(date: String, status: String, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE daily_prayers SET asrStatus = :status, updatedAt = :now WHERE date = :date")
    suspend fun updateAsrStatus(date: String, status: String, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE daily_prayers SET maghribStatus = :status, updatedAt = :now WHERE date = :date")
    suspend fun updateMaghribStatus(date: String, status: String, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE daily_prayers SET ishaStatus = :status, updatedAt = :now WHERE date = :date")
    suspend fun updateIshaStatus(date: String, status: String, now: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM daily_prayers")
    suspend fun clearAllDailyPrayers()

    // Kaza prayers
    @Query("SELECT * FROM kaza_prayers")
    fun getAllKazaPrayers(): Flow<List<KazaPrayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKaza(kaza: KazaPrayerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialKazalari(kazalar: List<KazaPrayerEntity>)

    @Query("UPDATE kaza_prayers SET owedCount = MAX(0, owedCount + :delta), updatedAt = :now WHERE prayerType = :type")
    suspend fun adjustKazaOwed(type: String, delta: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE kaza_prayers SET owedCount = MAX(0, owedCount - 1), completedCount = completedCount + 1, updatedAt = :now WHERE prayerType = :type AND owedCount > 0")
    suspend fun completeOneKaza(type: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE kaza_prayers SET owedCount = owedCount + 1, completedCount = MAX(0, completedCount - 1), updatedAt = :now WHERE prayerType = :type")
    suspend fun undoCompleteOneKaza(type: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE kaza_prayers SET owedCount = MAX(0, owedCount + :extra) WHERE prayerType IN ('FAJR', 'DHUHR', 'ASR', 'MAGHRIB', 'ISHA', 'WITR')")
    suspend fun addBulkKaza(extra: Int)

    // Partner
    @Query("SELECT * FROM partner_info WHERE id = 1 LIMIT 1")
    fun getPartnerInfo(): Flow<PartnerInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePartnerInfo(info: PartnerInfoEntity)

    @Query("UPDATE partner_info SET lastReceivedDua = :dua, lastSyncTime = :now WHERE id = 1")
    suspend fun updatePartnerReceivedDua(dua: String, now: Long = System.currentTimeMillis())

    // Settings
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}
