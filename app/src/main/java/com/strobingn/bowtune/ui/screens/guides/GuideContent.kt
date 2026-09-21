package com.strobingn.bowtune.ui.screens.guides

data class Guide(
    val id: String,
    val title: String,
    val brand: String,
    val summary: String,
    val sections: List<GuideSection>
)

data class GuideSection(
    val heading: String,
    val bullets: List<String>
)

object GuideCatalog {
    val all = listOf(
        Guide(
            id = "mathews_limb_shift",
            title = "Mathews Limb Shift Technology",
            brand = "Mathews",
            summary = "Dial left/right paper and walk-back with Limb Shift instead of large rest moves.",
            sections = listOf(
                GuideSection(
                    "What it does",
                    listOf(
                        "Limb Shift changes cam lean / limb pocket orientation in tiny indexed steps.",
                        "Primary use: horizontal (left/right) tears and walk-back after centershot is close.",
                        "Prefer Limb Shift over big rest swings so your rest stays near berger-hole / manufacturer start."
                    )
                ),
                GuideSection(
                    "Setup order",
                    listOf(
                        "Set draw length and cam timing / cable stops first.",
                        "Install D-loop; set nock height ~1/8-1/4\" above square.",
                        "Start centershot near manufacturer recommendation.",
                        "Paper at 4-6 ft with relaxed grip before touching Limb Shift."
                    )
                ),
                GuideSection(
                    "Left / right corrections (RH)",
                    listOf(
                        "Nock-left tear (RH release): Limb Shift toward L (chase the tear). Rest moves right in small clicks — do not move the rest in for a left tear.",
                        "Nock-right tear (RH release): Limb Shift toward R. Rest moves left (in toward the riser). Check vane/cable clearance; do not treat this as a stiff-spine first guess.",
                        "One click (or half-index), re-paper, then bare shaft at ~20 yd.",
                        "Finish with walk-back; use rest only for micro centershot."
                    )
                ),
                GuideSection(
                    "Tips",
                    listOf(
                        "Record Limb Shift setting in Gear so you can return after a string change.",
                        "Do not chase vertical tears with Limb Shift - use nock height / rest height.",
                        "Always recheck grip torque before another Limb Shift click."
                    )
                )
            )
        ),
        Guide(
            id = "pse_ez220",
            title = "PSE EZ.220 Tuning",
            brand = "PSE",
            summary = "Use EZ.220 / cable-guard style lateral adjustments for PSE horizontal tune.",
            sections = listOf(
                GuideSection(
                    "Overview",
                    listOf(
                        "PSE EZ.220 (and related cable-guard / roller systems) lets you bias cable load for left-right flight.",
                        "Combine with rest micro-moves; avoid stacking huge rest and EZ moves in one shot."
                    )
                ),
                GuideSection(
                    "Baseline",
                    listOf(
                        "Sync cams / set draw length and confirm cable-stop contact.",
                        "D-loop first, then nock height for vertical.",
                        "Paper tune at 4-6 ft; confirm grip is neutral."
                    )
                ),
                GuideSection(
                    "Horizontal workflow",
                    listOf(
                        "RH nock-left: move rest right in small clicks, or EZ.220 / Limb-style chase toward L.",
                        "RH nock-right: move rest left (in toward the riser), or the opposite EZ.220 move. Check vane clearance.",
                        "Tiny moves only; re-paper after each change.",
                        "Verify with bare shaft, then walk-back centershot."
                    )
                ),
                GuideSection(
                    "Notes",
                    listOf(
                        "Log EZ.220 position in Gear notes after a good tune.",
                        "Vertical still = nock height / rest height / timing - not EZ.220."
                    )
                )
            )
        ),
        Guide(
            id = "hoyt_xts",
            title = "Hoyt XTS Cam Tuning",
            brand = "Hoyt",
            summary = "XTS timing, yoke, and rest workflow for clean paper and bare-shaft results.",
            sections = listOf(
                GuideSection(
                    "Cam timing",
                    listOf(
                        "Set module / draw length correctly before paper.",
                        "Check top/bottom cam timing marks or cable-stop sync per your XTS year.",
                        "Uneven timing often shows as stubborn vertical or flipping tears."
                    )
                ),
                GuideSection(
                    "Yoke and centershot",
                    listOf(
                        "Hoyt yoke twists are a powerful left-right tool after rest is close.",
                        "RH left tear: typically add twist(s) on the side that moves the nock right (confirm for your cam).",
                        "Keep rest near recommended centershot; finish with walk-back."
                    )
                ),
                GuideSection(
                    "Paper protocol",
                    listOf(
                        "4-6 ft paper, relaxed grip.",
                        "Fix vertical (nock height) before yoke for pure L/R.",
                        "Combo tears: vertical axis first, then horizontal."
                    )
                ),
                GuideSection(
                    "Checklist",
                    listOf(
                        "D-loop installed and serving protected.",
                        "Rest clearance confirmed (no shelf slap).",
                        "Bare shaft at 20 yd before broadheads."
                    )
                )
            )
        ),
        Guide(
            id = "bowtech_deadlock",
            title = "Bowtech DeadLock Tuning",
            brand = "Bowtech",
            summary = "DeadLock cam micro-adjust for fast, repeatable left-right tune.",
            sections = listOf(
                GuideSection(
                    "DeadLock idea",
                    listOf(
                        "DeadLock lets you slide/lock the cam laterally, then lock set screws.",
                        "Goal: dial horizontal tune without changing rest drastically."
                    )
                ),
                GuideSection(
                    "Process",
                    listOf(
                        "Set draw length and confirm DeadLock cams are locked before shooting.",
                        "Establish nock height and a sensible centershot.",
                        "Paper at 4-6 ft; diagnose tear.",
                        "Unlock, slide cam a tiny amount toward correcting L/R, re-lock evenly, re-paper."
                    )
                ),
                GuideSection(
                    "Safety and consistency",
                    listOf(
                        "Always re-torque DeadLock screws to spec after moves.",
                        "Mark a reference line so you can return to a known good position.",
                        "Do not use DeadLock to fix nock-high/low - use nock point / rest height."
                    )
                ),
                GuideSection(
                    "Finish",
                    listOf(
                        "Bare shaft then walk-back then broadheads vs field points.",
                        "Save DeadLock notes in Gear for string/cable replacements."
                    )
                )
            )
        ),
        Guide(
            id = "elite_set",
            title = "Elite S.E.T. Technology",
            brand = "Elite",
            summary = "S.E.T. (Simplified Exact Tuning) for cam lean / limb-pocket style horizontal tune.",
            sections = listOf(
                GuideSection(
                    "What S.E.T. is",
                    listOf(
                        "Elite S.E.T. adjusts cam lean via limb pocket / set screws for left-right flight.",
                        "Similar role to Limb Shift / DeadLock: keep rest sane while fixing paper L/R."
                    )
                ),
                GuideSection(
                    "Tune sequence",
                    listOf(
                        "Draw length and timing first.",
                        "D-loop + nock height for vertical tears.",
                        "Paper 4-6 ft; confirm grip.",
                        "Adjust S.E.T. in small increments for nock-left / nock-right.",
                        "Re-lock / recheck torque; re-paper."
                    )
                ),
                GuideSection(
                    "RH shooter hints",
                    listOf(
                        "Nock-left: S.E.T. move that brings the nock right (see your model's direction chart).",
                        "Nock-right: opposite S.E.T. move.",
                        "One change per group; then bare shaft and walk-back."
                    )
                ),
                GuideSection(
                    "Record keeping",
                    listOf(
                        "Store S.E.T. setting and rest position in Gear.",
                        "After a cable change, start from your last known good S.E.T. mark."
                    )
                )
            )
        )
    )

    fun byId(id: String): Guide? = all.firstOrNull { it.id == id }
}
