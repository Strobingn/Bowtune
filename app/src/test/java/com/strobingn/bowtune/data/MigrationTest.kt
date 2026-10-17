package com.strobingn.bowtune.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a database with the exact v3 schema Room generated for Bow Tune 1.3.0
 * (`bowtune_schema_v3.sql`), fills every table, then opens it with the current
 * [AppDatabase]. Room validates the migrated schema on open, so a bad migration fails here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun clean() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun schemaV3(): List<String> =
        javaClass.classLoader!!.getResource("bowtune_schema_v3.sql")!!.readText()
            .lines()
            .map { it.trim().removeSuffix(";") }
            .filter { it.isNotEmpty() }

    private fun createV3(seed: (SupportSQLiteDatabase) -> Unit) {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schemaV3().forEach(db::execSQL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        seed(helper.writableDatabase)
        helper.close()
    }

    @Test
    fun upgradeFrom130KeepsEveryRow() {
        createV3 { db ->
            db.execSQL(
                "INSERT INTO bow_setups (id, name, brand, model, drawWeightLbs, drawLengthIn, rest, sight, arrows, notes, " +
                    "limbShiftSetting, yokeTwistNotes, camSystem, nextTuneDueEpochMs, iboSpeedFps, arrowSpeedFps, " +
                    "lastCamCheckEpochMs, camTimingNotes, lastCableCheckEpochMs, cableStretchNotes) VALUES " +
                    "(1, 'LIFT 29.5', 'Mathews', 'LIFT', '70', '29.5', 'QAD', 'Spot Hogg', 'RIP TKO', 'n', 'C', 'y', 'Crosscentric', " +
                    "1700000000000, '343', '291', NULL, 'ok', NULL, 'ok')"
            )
            db.execSQL(
                "INSERT INTO tune_sessions (id, dateEpochMs, distanceYd, scoreOrGroup, notes, setupName, sessionKind, " +
                    "indoorOutdoor, windNote, tempNote, tearType) VALUES " +
                    "(7, 1700000000000, '20', '2.5×3.0', 'old note', 'LIFT 29.5', 'group', 'outdoor', 'calm', '60F', '')"
            )
            db.execSQL(
                "INSERT INTO guided_tune_sessions (id, protocolId, title, setupName, startedAtEpochMs, updatedAtEpochMs, " +
                    "completedAtEpochMs, currentBlockIndex, blockLogsJson, status, bareShaftHl20, bareShaftLr20, walkBackLr30, " +
                    "final30GroupSize, decisionNotes) VALUES (3, 'lift_29_5_vertical_90', 'LIFT', 'LIFT 29.5', 1, 2, NULL, 4, " +
                    "'{\"walkback.p1_10\":\"0.5\"}', 'IN_PROGRESS', '+0.5 high', '', '', '', '')"
            )
            db.execSQL(
                "INSERT INTO arrow_shafts (id, setupId, name, brand, spine, lengthIn, gpi, pointWeightGr, insertWeightGr, " +
                    "nockWeightGr, vaneWeightGr, wrapWeightGr, balanceFromNockIn, quantity, notes) VALUES " +
                    "(2, 1, 'RIP TKO', 'Victory', '300', '28.5', '270', '125', '16', '10', '18', '', '17.25', '12', '')"
            )
            db.execSQL(
                "INSERT INTO paper_tear_logs (id, dateEpochMs, tearType, setupName, distanceFt, notes) VALUES " +
                    "(5, 1700000000000, 'LEFT', 'LIFT 29.5', '4-6', 'grip?')"
            )
            db.execSQL(
                "INSERT INTO adjustment_logs (id, dateEpochMs, setupName, kind, beforeValue, afterValue, notes) VALUES " +
                    "(6, 1700000000000, 'LIFT 29.5', 'rest', '0', '1 click R', '')"
            )
            db.execSQL(
                "INSERT INTO maintenance_logs (id, setupName, kind, dateEpochMs, notes, nextDueEpochMs) VALUES " +
                    "(8, 'LIFT 29.5', 'cam_timing', 1700000000000, 'synced', 1702592000000)"
            )
        }

        val db = AppDatabase.build(context, name)
        try {
            runBlocking {
                val setup = db.bowSetupDao().getAll().single()
                assertEquals("LIFT 29.5", setup.name)
                assertEquals("70", setup.drawWeightLbs)
                assertEquals(1700000000000L, setup.nextTuneDueEpochMs)

                val session = db.tuneSessionDao().getAll().single()
                assertEquals(7L, session.id)
                assertEquals("2.5×3.0", session.scoreOrGroup)
                assertEquals("old note", session.notes)
                assertEquals("{}", session.metricsJson)

                val guided = db.guidedTuneSessionDao().getAll().single()
                assertEquals("+0.5 high", guided.bareShaftHl20)
                assertEquals(4, guided.currentBlockIndex)

                assertEquals("RIP TKO", db.arrowShaftDao().getAll().single().name)

                val tear = db.paperTearLogDao().getAll().single()
                assertEquals("LEFT", tear.tearType)
                assertEquals("grip?", tear.notes)
                assertNull(tear.magnitudeIn)

                assertEquals("1 click R", db.adjustmentLogDao().getAll().single().afterValue)
                assertEquals(1702592000000L, db.maintenanceLogDao().getAll().single().nextDueEpochMs)

                db.tuneSessionDao().upsert(
                    TuneSession(dateEpochMs = 1, distanceYd = "30", metricsJson = TuneMetrics.encode(mapOf(TuneMetrics.SCORE to 290.0)))
                )
                db.paperTearLogDao().upsert(PaperTearLog(dateEpochMs = 2, tearType = "NOCK_HIGH", magnitudeIn = 0.75))
                assertEquals(2, db.tuneSessionDao().getAll().size)
                assertEquals(0.75, db.paperTearLogDao().getAll().first { it.tearType == "NOCK_HIGH" }.magnitudeIn!!, 1e-9)
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun freshInstallCreatesV4() {
        val db = AppDatabase.build(context, name)
        try {
            runBlocking {
                db.paperTearLogDao().upsert(PaperTearLog(dateEpochMs = 1, tearType = "BULLET"))
                assertNull(db.paperTearLogDao().getAll().single().magnitudeIn)
                db.tuneSessionDao().upsert(TuneSession(dateEpochMs = 1, distanceYd = "20"))
                assertEquals("{}", db.tuneSessionDao().getAll().single().metricsJson)
            }
            assertEquals(4, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }
}
