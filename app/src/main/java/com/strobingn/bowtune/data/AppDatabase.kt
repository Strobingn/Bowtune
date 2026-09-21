package com.strobingn.bowtune.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BowSetup::class,
        TuneSession::class,
        GuidedTuneSession::class,
        ArrowShaft::class,
        PaperTearLog::class,
        AdjustmentLog::class,
        MaintenanceLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bowSetupDao(): BowSetupDao
    abstract fun tuneSessionDao(): TuneSessionDao
    abstract fun guidedTuneSessionDao(): GuidedTuneSessionDao
    abstract fun arrowShaftDao(): ArrowShaftDao
    abstract fun paperTearLogDao(): PaperTearLogDao
    abstract fun adjustmentLogDao(): AdjustmentLogDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bowtune.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
