package com.strobingn.bowtune.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 4,
    exportSchema = true
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
        const val NAME = "bowtune.db"

        /** 1.3.x → 1.4.0: additive columns only, so every existing row is kept. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `paper_tear_logs` ADD COLUMN `magnitudeIn` REAL")
                db.execSQL(
                    "ALTER TABLE `tune_sessions` ADD COLUMN `metricsJson` TEXT NOT NULL DEFAULT '{}'"
                )
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4)

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, NAME).also { instance = it }
            }

        fun build(context: Context, name: String): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(*ALL_MIGRATIONS)
                // v1/v2 pre-date the 1.2.0 release and were always rebuilt; anything newer must
                // migrate explicitly so a missing migration crashes instead of wiping user data.
                .fallbackToDestructiveMigrationFrom(1, 2)
                .build()
    }
}
