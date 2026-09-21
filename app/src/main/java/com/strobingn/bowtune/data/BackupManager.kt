package com.strobingn.bowtune.data

import org.json.JSONArray
import org.json.JSONObject

object BackupManager {
    const val FORMAT = "bowtune-backup-v1"

    suspend fun exportJson(
        db: AppDatabase,
        prefs: AppPreferences,
        checklist: ChecklistStore
    ): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("prefs", prefs.snapshot().toJsonObject())
        root.put("checklist", checklist.snapshot().toJsonObject())
        root.put("bowSetups", arrayOf(db.bowSetupDao().getAll()) { it.toJson() })
        root.put("tuneSessions", arrayOf(db.tuneSessionDao().getAll()) { it.toJson() })
        root.put("guidedSessions", arrayOf(db.guidedTuneSessionDao().getAll()) { it.toJson() })
        root.put("arrows", arrayOf(db.arrowShaftDao().getAll()) { it.toJson() })
        root.put("paperTears", arrayOf(db.paperTearLogDao().getAll()) { it.toJson() })
        root.put("adjustments", arrayOf(db.adjustmentLogDao().getAll()) { it.toJson() })
        root.put("maintenance", arrayOf(db.maintenanceLogDao().getAll()) { it.toJson() })
        return root.toString(2)
    }

    suspend fun importJson(
        json: String,
        db: AppDatabase,
        prefs: AppPreferences,
        checklist: ChecklistStore
    ) {
        val root = JSONObject(json)
        val format = root.optString("format")
        require(format == FORMAT) { "Not a Bow Tune backup (format=$format)" }

        db.bowSetupDao().deleteAll()
        db.tuneSessionDao().deleteAll()
        db.guidedTuneSessionDao().deleteAll()
        db.arrowShaftDao().deleteAll()
        db.paperTearLogDao().deleteAll()
        db.adjustmentLogDao().deleteAll()
        db.maintenanceLogDao().deleteAll()

        root.optJSONObject("prefs")?.let { obj ->
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { k -> map[k] = obj.optString(k, "") }
            prefs.restore(map)
        }
        root.optJSONObject("checklist")?.let { obj ->
            val map = mutableMapOf<String, Boolean>()
            obj.keys().forEach { k -> map[k] = obj.optBoolean(k, false) }
            checklist.restore(map)
        }

        root.optJSONArray("bowSetups")?.forEachObj { db.bowSetupDao().upsert(it.toBowSetup()) }
        root.optJSONArray("tuneSessions")?.forEachObj { db.tuneSessionDao().upsert(it.toTuneSession()) }
        root.optJSONArray("guidedSessions")?.forEachObj { db.guidedTuneSessionDao().upsert(it.toGuided()) }
        root.optJSONArray("arrows")?.forEachObj { db.arrowShaftDao().upsert(it.toArrow()) }
        root.optJSONArray("paperTears")?.forEachObj { db.paperTearLogDao().upsert(it.toPaperTear()) }
        root.optJSONArray("adjustments")?.forEachObj { db.adjustmentLogDao().upsert(it.toAdjustment()) }
        root.optJSONArray("maintenance")?.forEachObj { db.maintenanceLogDao().upsert(it.toMaintenance()) }
    }

    private fun <T> arrayOf(items: List<T>, toJson: (T) -> JSONObject): JSONArray {
        val arr = JSONArray()
        items.forEach { arr.put(toJson(it)) }
        return arr
    }

    private suspend fun JSONArray.forEachObj(block: suspend (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            block(getJSONObject(i))
        }
    }

    private fun BowSetup.toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("brand", brand)
        .put("model", model)
        .put("drawWeightLbs", drawWeightLbs)
        .put("drawLengthIn", drawLengthIn)
        .put("rest", rest)
        .put("sight", sight)
        .put("arrows", arrows)
        .put("notes", notes)
        .put("limbShiftSetting", limbShiftSetting)
        .put("yokeTwistNotes", yokeTwistNotes)
        .put("camSystem", camSystem)
        .put("nextTuneDueEpochMs", nextTuneDueEpochMs ?: JSONObject.NULL)
        .put("iboSpeedFps", iboSpeedFps)
        .put("arrowSpeedFps", arrowSpeedFps)
        .put("lastCamCheckEpochMs", lastCamCheckEpochMs ?: JSONObject.NULL)
        .put("camTimingNotes", camTimingNotes)
        .put("lastCableCheckEpochMs", lastCableCheckEpochMs ?: JSONObject.NULL)
        .put("cableStretchNotes", cableStretchNotes)

    private fun JSONObject.toBowSetup() = BowSetup(
        id = optLong("id"),
        name = optString("name"),
        brand = optString("brand"),
        model = optString("model"),
        drawWeightLbs = optString("drawWeightLbs"),
        drawLengthIn = optString("drawLengthIn"),
        rest = optString("rest"),
        sight = optString("sight"),
        arrows = optString("arrows"),
        notes = optString("notes"),
        limbShiftSetting = optString("limbShiftSetting"),
        yokeTwistNotes = optString("yokeTwistNotes"),
        camSystem = optString("camSystem"),
        nextTuneDueEpochMs = optLongOrNull("nextTuneDueEpochMs"),
        iboSpeedFps = optString("iboSpeedFps"),
        arrowSpeedFps = optString("arrowSpeedFps"),
        lastCamCheckEpochMs = optLongOrNull("lastCamCheckEpochMs"),
        camTimingNotes = optString("camTimingNotes"),
        lastCableCheckEpochMs = optLongOrNull("lastCableCheckEpochMs"),
        cableStretchNotes = optString("cableStretchNotes")
    )

    private fun TuneSession.toJson() = JSONObject()
        .put("id", id)
        .put("dateEpochMs", dateEpochMs)
        .put("distanceYd", distanceYd)
        .put("scoreOrGroup", scoreOrGroup)
        .put("notes", notes)
        .put("setupName", setupName)
        .put("sessionKind", sessionKind)
        .put("indoorOutdoor", indoorOutdoor)
        .put("windNote", windNote)
        .put("tempNote", tempNote)
        .put("tearType", tearType)

    private fun JSONObject.toTuneSession() = TuneSession(
        id = optLong("id"),
        dateEpochMs = optLong("dateEpochMs"),
        distanceYd = optString("distanceYd"),
        scoreOrGroup = optString("scoreOrGroup"),
        notes = optString("notes"),
        setupName = optString("setupName"),
        sessionKind = optString("sessionKind").ifBlank { SessionKind.GROUP },
        indoorOutdoor = optString("indoorOutdoor"),
        windNote = optString("windNote"),
        tempNote = optString("tempNote"),
        tearType = optString("tearType")
    )

    private fun GuidedTuneSession.toJson() = JSONObject()
        .put("id", id)
        .put("protocolId", protocolId)
        .put("title", title)
        .put("setupName", setupName)
        .put("startedAtEpochMs", startedAtEpochMs)
        .put("updatedAtEpochMs", updatedAtEpochMs)
        .put("completedAtEpochMs", completedAtEpochMs ?: JSONObject.NULL)
        .put("currentBlockIndex", currentBlockIndex)
        .put("blockLogsJson", blockLogsJson)
        .put("status", status)
        .put("bareShaftHl20", bareShaftHl20)
        .put("bareShaftLr20", bareShaftLr20)
        .put("walkBackLr30", walkBackLr30)
        .put("final30GroupSize", final30GroupSize)
        .put("decisionNotes", decisionNotes)

    private fun JSONObject.toGuided() = GuidedTuneSession(
        id = optLong("id"),
        protocolId = optString("protocolId"),
        title = optString("title"),
        setupName = optString("setupName"),
        startedAtEpochMs = optLong("startedAtEpochMs"),
        updatedAtEpochMs = optLong("updatedAtEpochMs"),
        completedAtEpochMs = optLongOrNull("completedAtEpochMs"),
        currentBlockIndex = optInt("currentBlockIndex"),
        blockLogsJson = optString("blockLogsJson").ifBlank { "{}" },
        status = optString("status").ifBlank { GuidedTuneSession.STATUS_IN_PROGRESS },
        bareShaftHl20 = optString("bareShaftHl20"),
        bareShaftLr20 = optString("bareShaftLr20"),
        walkBackLr30 = optString("walkBackLr30"),
        final30GroupSize = optString("final30GroupSize"),
        decisionNotes = optString("decisionNotes")
    )

    private fun ArrowShaft.toJson() = JSONObject()
        .put("id", id)
        .put("setupId", setupId ?: JSONObject.NULL)
        .put("name", name)
        .put("brand", brand)
        .put("spine", spine)
        .put("lengthIn", lengthIn)
        .put("gpi", gpi)
        .put("pointWeightGr", pointWeightGr)
        .put("insertWeightGr", insertWeightGr)
        .put("nockWeightGr", nockWeightGr)
        .put("vaneWeightGr", vaneWeightGr)
        .put("wrapWeightGr", wrapWeightGr)
        .put("balanceFromNockIn", balanceFromNockIn)
        .put("quantity", quantity)
        .put("notes", notes)

    private fun JSONObject.toArrow() = ArrowShaft(
        id = optLong("id"),
        setupId = optLongOrNull("setupId"),
        name = optString("name"),
        brand = optString("brand"),
        spine = optString("spine"),
        lengthIn = optString("lengthIn"),
        gpi = optString("gpi"),
        pointWeightGr = optString("pointWeightGr"),
        insertWeightGr = optString("insertWeightGr"),
        nockWeightGr = optString("nockWeightGr"),
        vaneWeightGr = optString("vaneWeightGr"),
        wrapWeightGr = optString("wrapWeightGr"),
        balanceFromNockIn = optString("balanceFromNockIn"),
        quantity = optString("quantity"),
        notes = optString("notes")
    )

    private fun PaperTearLog.toJson() = JSONObject()
        .put("id", id)
        .put("dateEpochMs", dateEpochMs)
        .put("tearType", tearType)
        .put("setupName", setupName)
        .put("distanceFt", distanceFt)
        .put("notes", notes)

    private fun JSONObject.toPaperTear() = PaperTearLog(
        id = optLong("id"),
        dateEpochMs = optLong("dateEpochMs"),
        tearType = optString("tearType"),
        setupName = optString("setupName"),
        distanceFt = optString("distanceFt"),
        notes = optString("notes")
    )

    private fun AdjustmentLog.toJson() = JSONObject()
        .put("id", id)
        .put("dateEpochMs", dateEpochMs)
        .put("setupName", setupName)
        .put("kind", kind)
        .put("beforeValue", beforeValue)
        .put("afterValue", afterValue)
        .put("notes", notes)

    private fun JSONObject.toAdjustment() = AdjustmentLog(
        id = optLong("id"),
        dateEpochMs = optLong("dateEpochMs"),
        setupName = optString("setupName"),
        kind = optString("kind"),
        beforeValue = optString("beforeValue"),
        afterValue = optString("afterValue"),
        notes = optString("notes")
    )

    private fun MaintenanceLog.toJson() = JSONObject()
        .put("id", id)
        .put("setupName", setupName)
        .put("kind", kind)
        .put("dateEpochMs", dateEpochMs)
        .put("notes", notes)
        .put("nextDueEpochMs", nextDueEpochMs ?: JSONObject.NULL)

    private fun JSONObject.toMaintenance() = MaintenanceLog(
        id = optLong("id"),
        setupName = optString("setupName"),
        kind = optString("kind"),
        dateEpochMs = optLong("dateEpochMs"),
        notes = optString("notes"),
        nextDueEpochMs = optLongOrNull("nextDueEpochMs")
    )

    private fun Map<String, *>.toJsonObject(): JSONObject {
        val o = JSONObject()
        forEach { (k, v) ->
            when (v) {
                null -> o.put(k, JSONObject.NULL)
                is Boolean -> o.put(k, v)
                is Number -> o.put(k, v)
                else -> o.put(k, v.toString())
            }
        }
        return o
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val v = optLong(key, Long.MIN_VALUE)
        return if (v == Long.MIN_VALUE) null else v
    }
}
