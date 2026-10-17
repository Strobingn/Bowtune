package com.strobingn.bowtune.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun newColumnsSurviveExportImportAndOldBackupsStillLoad() = runBlocking {
        val name = "backup-test.db"
        context.deleteDatabase(name)
        val db = AppDatabase.build(context, name)
        val prefs = AppPreferences(context)
        val checklist = ChecklistStore(context)
        try {
            val metrics = TuneMetrics.encode(mapOf(TuneMetrics.BARE_V to 0.5, TuneMetrics.walkBackKey(30) to -1.0))
            db.tuneSessionDao().upsert(TuneSession(dateEpochMs = 10, distanceYd = "20", sessionKind = SessionKind.BARE_SHAFT, metricsJson = metrics))
            db.paperTearLogDao().upsert(PaperTearLog(dateEpochMs = 11, tearType = "LEFT", magnitudeIn = 0.625))
            db.paperTearLogDao().upsert(PaperTearLog(dateEpochMs = 12, tearType = "BULLET"))
            prefs.setTtsRate(1.4f)
            prefs.setHapticStrength(3)

            val json = BackupManager.exportJson(db, prefs, checklist)
            BackupManager.importJson(json, db, prefs, checklist)

            val session = db.tuneSessionDao().getAll().single()
            assertEquals(TuneMetrics.decode(metrics), TuneMetrics.decode(session.metricsJson))
            val tears = db.paperTearLogDao().getAll().sortedBy { it.dateEpochMs }
            assertEquals(0.625, tears[0].magnitudeIn!!, 1e-9)
            assertNull(tears[1].magnitudeIn)
            val snapshot = prefs.snapshot()
            assertEquals("1.4", snapshot[AppPreferences.KEY_TTS_RATE.name])
            assertEquals("3", snapshot[AppPreferences.KEY_HAPTIC_STRENGTH.name])

            val legacy = JSONObject(json)
            legacy.getJSONArray("tuneSessions").getJSONObject(0).remove("metricsJson")
            legacy.getJSONArray("paperTears").getJSONObject(0).remove("magnitudeIn")
            BackupManager.importJson(legacy.toString(), db, prefs, checklist)
            assertEquals("{}", db.tuneSessionDao().getAll().single().metricsJson)
            assertEquals(2, db.paperTearLogDao().getAll().size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
