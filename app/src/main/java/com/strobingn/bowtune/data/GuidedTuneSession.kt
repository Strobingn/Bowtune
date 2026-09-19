package com.strobingn.bowtune.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists in-progress and completed guided tune sessions (e.g. LIFT 29.5 vertical).
 * Block field values live in [blockLogsJson] as a simple key→value map serialized to JSON.
 * Wrap-up four numbers are first-class columns for querying / summary.
 */
@Entity(tableName = "guided_tune_sessions")
data class GuidedTuneSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val protocolId: String,
    val title: String,
    val setupName: String = "",
    val startedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    /** Index into protocol blocks; equals blocks.size when on wrap-up / decision screen. */
    val currentBlockIndex: Int = 0,
    /** JSON object: "{ \"baseline.groupWxH\": \"2x3\", ... }" */
    val blockLogsJson: String = "{}",
    val status: String = STATUS_IN_PROGRESS,
    // Required four-number wrap-up
    val bareShaftHl20: String = "",
    val bareShaftLr20: String = "",
    val walkBackLr30: String = "",
    val final30GroupSize: String = "",
    val decisionNotes: String = ""
) {
    companion object {
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
