package com.strobingn.bowtune.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object SessionKind {
    const val GROUP = "group"
    const val PAPER = "paper"
    const val BARE_SHAFT = "bareshaft"
    const val WALKBACK = "walkback"
    const val BROADHEAD = "broadhead"
    const val VISION = "vision"
    const val GUIDED = "guided"

    val all = listOf(GROUP, PAPER, BARE_SHAFT, WALKBACK, BROADHEAD, VISION, GUIDED)

    fun label(kind: String): String = when (kind) {
        GROUP -> "Group / score"
        PAPER -> "Paper tear"
        BARE_SHAFT -> "Bare shaft"
        WALKBACK -> "Walk-back"
        BROADHEAD -> "Broadhead vs field"
        VISION -> "Vision form"
        GUIDED -> "Guided wizard"
        else -> kind
    }
}

@Entity(tableName = "arrow_shafts")
data class ArrowShaft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setupId: Long? = null,
    val name: String,
    val brand: String = "",
    val spine: String = "",
    val lengthIn: String = "",
    val gpi: String = "",
    val pointWeightGr: String = "",
    val insertWeightGr: String = "",
    val nockWeightGr: String = "",
    val vaneWeightGr: String = "",
    val wrapWeightGr: String = "",
    val balanceFromNockIn: String = "",
    val quantity: String = "12",
    val notes: String = ""
)

@Entity(tableName = "paper_tear_logs")
data class PaperTearLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMs: Long,
    val tearType: String,
    val setupName: String = "",
    val distanceFt: String = "4-6",
    val notes: String = ""
)

@Entity(tableName = "adjustment_logs")
data class AdjustmentLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMs: Long,
    val setupName: String = "",
    val kind: String,
    val beforeValue: String = "",
    val afterValue: String = "",
    val notes: String = ""
) {
    companion object {
        const val LIMB_SHIFT = "limb_shift"
        const val CENTERSHOT = "centershot"
        const val REST = "rest"
        const val NOCK = "nock"
        const val YOKE = "yoke"

        fun label(kind: String): String = when (kind) {
            LIMB_SHIFT -> "Limb Shift"
            CENTERSHOT -> "Centershot"
            REST -> "Rest"
            NOCK -> "Nock height"
            YOKE -> "Yoke / cable"
            else -> kind
        }
    }
}

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setupName: String = "",
    val kind: String,
    val dateEpochMs: Long,
    val notes: String = "",
    val nextDueEpochMs: Long? = null
) {
    companion object {
        const val CAM_TIMING = "cam_timing"
        const val CABLE_STRETCH = "cable_stretch"

        fun label(kind: String): String = when (kind) {
            CAM_TIMING -> "Cam timing"
            CABLE_STRETCH -> "Cable / string stretch"
            else -> kind
        }
    }
}
