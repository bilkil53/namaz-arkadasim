package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PrayerDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DailyPrayerEntity
import com.example.data.local.entity.KazaPrayerEntity
import com.example.data.local.entity.PartnerInfoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DailyPrayerEntity::class,
        KazaPrayerEntity::class,
        PartnerInfoEntity::class,
        AppSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "namaz_arkadasim_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.prayerDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: PrayerDao) {
            // Initial Kaza records for 6 prayers with zero counts
            val initialKazalar = listOf(
                KazaPrayerEntity("FAJR", owedCount = 0, completedCount = 0),
                KazaPrayerEntity("DHUHR", owedCount = 0, completedCount = 0),
                KazaPrayerEntity("ASR", owedCount = 0, completedCount = 0),
                KazaPrayerEntity("MAGHRIB", owedCount = 0, completedCount = 0),
                KazaPrayerEntity("ISHA", owedCount = 0, completedCount = 0),
                KazaPrayerEntity("WITR", owedCount = 0, completedCount = 0)
            )
            dao.insertInitialKazalari(initialKazalar)

            // Initial Partner Info (Unmatched, clean invite code, no hardcoded user)
            dao.savePartnerInfo(
                PartnerInfoEntity(
                    id = 1,
                    myInviteCode = "ARK-${(1000..9999).random()}",
                    myDisplayName = "",
                    myEmail = "",
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
                    partnerKazaOwed = 0,
                    lastReceivedDua = "",
                    lastSyncTime = System.currentTimeMillis()
                )
            )

            // Initial App Settings (User is NOT logged in by default)
            dao.saveSettings(
                AppSettingsEntity(
                    id = 1,
                    userName = "",
                    userEmail = "",
                    isLoggedIn = false,
                    cityName = "İstanbul",
                    latitude = 41.0082,
                    longitude = 28.9784,
                    motivationNotificationEnabled = true,
                    motivationNotificationTime = "11:00",
                    prayerRemindersEnabled = true,
                    partnerAlertsEnabled = true
                )
            )
        }
    }
}
