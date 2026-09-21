package com.strobingn.bowtune.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bow_setups")
data class BowSetup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val drawWeightLbs: String = "",
    val drawLengthIn: String = "",
    val rest: String = "",
    val sight: String = "",
    val arrows: String = "",
    val notes: String = "",
    // Mathews / PSE friendly optional fields
    val limbShiftSetting: String = "",
    val yokeTwistNotes: String = "",
    val camSystem: String = "",
    val nextTuneDueEpochMs: Long? = null,
    val iboSpeedFps: String = "",
    val arrowSpeedFps: String = "",
    val lastCamCheckEpochMs: Long? = null,
    val camTimingNotes: String = "",
    val lastCableCheckEpochMs: Long? = null,
    val cableStretchNotes: String = ""
)

@Entity(tableName = "tune_sessions")
data class TuneSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochMs: Long,
    val distanceYd: String,
    val scoreOrGroup: String = "",
    val notes: String = "",
    val setupName: String = "",
    val sessionKind: String = SessionKind.GROUP,
    val indoorOutdoor: String = "",
    val windNote: String = "",
    val tempNote: String = "",
    val tearType: String = ""
)

enum class TearType(
    val label: String,
    val shortDescription: String
) {
    BULLET("Bullet hole", "Clean tear - arrow flying true through paper"),
    NOCK_HIGH("Nock-high", "Nock end high / point low through paper"),
    NOCK_LOW("Nock-low", "Nock end low / point high through paper"),
    LEFT("Left tear", "Nock left / point right (RH shooter)"),
    RIGHT("Right tear", "Nock right / point left (RH shooter)"),
    HIGH_LEFT("High-left", "Nock high + left combo"),
    HIGH_RIGHT("High-right", "Nock high + right combo"),
    LOW_LEFT("Low-left", "Nock low + left combo"),
    LOW_RIGHT("Low-right", "Nock low + right combo")
}

data class ChecklistItem(
    val id: String,
    val phase: String,
    val title: String,
    val detail: String
)

object TuneChecklistCatalog {
    val items: List<ChecklistItem> = listOf(
        ChecklistItem(
            "dl_cam",
            "1. Draw length & cam timing",
            "Set draw length and sync cams",
            "Confirm draw length fits form. Check cam timing / cable stops so both cams roll over together. Mathews & PSE: verify timing marks or cable-stop contact before moving rest/sight."
        ),
        ChecklistItem(
            "centershot_nock",
            "2. Centershot & nock height",
            "Centershot + nock height (D-loop first)",
            "Install or verify D-loop BEFORE fine nock-height work. Set initial nock height ~1/8-1/4\" above square. Centershot: roughly berger-hole aligned or manufacturer start (often slightly outside). Mathews Limb Shift / PSE EZ.220 later for left-right."
        ),
        ChecklistItem(
            "paper",
            "3. Paper tune",
            "Paper tune at 4-6 feet",
            "Shoot through paper at 4-6 ft (Easton start; not 10+). Diagnose tear, apply ordered fixes. Watch for grip torque false tears. Prefer rest micro-moves, then yoke/limb-shift for L/R, nock height for H/L."
        ),
        ChecklistItem(
            "bareshaft",
            "4. Bare shaft 20 yd",
            "Bare shaft vs fletched at ~20 yd",
            "Compare bare shaft POI to fletched. Vertical: nock height / tiller. Horizontal: spine, centershot, yoke/limb. Confirm paper results outdoors before walk-back."
        ),
        ChecklistItem(
            "walkback",
            "5. Walk-back tune",
            "Walk-back / French tune",
            "Aim same pin at increasing distances; adjust centershot so impacts stay vertically stacked. Fine-tune after paper + bare shaft."
        ),
        ChecklistItem(
            "broadheads",
            "6. Broadheads vs field points",
            "Broadhead flight check",
            "Shoot fixed/mech broadheads vs field points at 20-40 yd. Remaining left-right often yoke twist or slight rest; vertical often tip weight / FOC / nock height. Confirm before hunting season."
        )
    )
}
