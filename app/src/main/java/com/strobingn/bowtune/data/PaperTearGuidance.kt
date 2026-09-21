package com.strobingn.bowtune.data

object PaperTearGuidance {
    const val DISTANCE_TIP =
        "Paper tune at 4-6 feet from the paper (Easton starting distance) — close enough that the arrow is still in the paradox window, far enough to clear the rest. After a clean hole, step back a few feet to confirm. Ten-plus feet often exaggerates tears and confuses diagnosis."

    const val GRIP_TORQUE_NOTE =
        "Grip torque false tears: a death-grip or torqueing the riser can invent left/right (and sometimes vertical) tears. Relax the bow hand, open palm pressure, and re-shoot before chasing hardware. Confirm with a second arrow and a second shooter if possible."

    const val GENERAL_HARDWARE =
        "General order of attack: (1) form and grip, (2) rest micro-adjustment, (3) nock height for vertical, (4) yoke twists / cable guard / limb shift for horizontal, (5) centershot as a walk-back fine-tune. D-loop first — set it before you chase nock height forever. One change at a time; re-paper after each move."

    fun stepsFor(
        tear: TearType,
        level: CoachingLevel = CoachingLevel.STANDARD
    ): List<String> {
        val core = coreSteps(tear)
        return when (level) {
            CoachingLevel.BEGINNER -> beginnerPreface(tear) + core + beginnerClose(tear)
            CoachingLevel.STANDARD -> core
            CoachingLevel.PRO -> proOverlay(tear, core)
        }
    }

    private fun coreSteps(tear: TearType): List<String> = when (tear) {
        TearType.BULLET -> listOf(
            "Nice — you have a bullet hole. Do not chase perfection into a spiral.",
            "Verify with 2-3 more shots and a relaxed grip.",
            "Move outdoors to bare-shaft at ~20 yd, then walk-back.",
            "Later: broadheads vs field points before hunting."
        )
        TearType.NOCK_HIGH -> listOf(
            "Confirm grip is relaxed (false vertical tears happen).",
            "Lower nocking point / D-loop slightly (small moves: 1/32-1/16\").",
            "Or raise the rest a hair if nock height is already in a good range.",
            "Re-paper at 4-6 ft; then bare-shaft check vertical at 20 yd.",
            "If stubborn: check cam timing / tiller and rest vertical play."
        )
        TearType.NOCK_LOW -> listOf(
            "Confirm grip and that the arrow is not hitting the rest shelf on launch.",
            "Raise nocking point / D-loop slightly.",
            "Or lower the rest slightly if clearance allows.",
            "Re-paper at 4-6 ft; confirm with bare shaft.",
            "Check cam timing if vertical keeps flipping."
        )
        TearType.LEFT -> listOf(
            "RH release shooter left tear (nock left).",
            "Grip check first — torque can fake left tears. Relax the bow hand and re-shoot.",
            "Then move the rest right in small clicks / 1/64\".",
            "Or use Mathews Limb Shift toward L (chase the tear).",
            "If it persists: stiffer spine or reduce draw weight.",
            "Re-paper; one change at a time."
        )
        TearType.RIGHT -> listOf(
            "RH release shooter right tear (nock right) — uncommon with a mechanical release. Usually the rest is too far right, or vanes are contacting.",
            "Grip check first — torque can fake a right tear too.",
            "Then move the rest left (in toward the riser) in small clicks / 1/64\".",
            "Or use Mathews Limb Shift toward R (chase the tear).",
            "Also check cable and vane clearance.",
            "Re-paper; one change at a time."
        )
        TearType.HIGH_LEFT -> listOf(
            "Fix vertical first (nock-high steps), then horizontal.",
            "Lower nock point or raise rest, then re-paper.",
            "Then left: move rest right in small clicks, or Limb Shift toward L.",
            "One change at a time — do not fix both axes in one move.",
            "Grip check between every change."
        )
        TearType.HIGH_RIGHT -> listOf(
            "Fix vertical first (nock-high steps), then horizontal.",
            "Lower nock / raise rest, then re-paper.",
            "Then right: move rest left (in toward the riser), or Limb Shift toward R.",
            "Also check cable and vane clearance.",
            "One change at a time; grip check between every change."
        )
        TearType.LOW_LEFT -> listOf(
            "Fix vertical first (nock-low steps), then horizontal.",
            "Raise nock / lower rest, then re-paper.",
            "Then left: move rest right in small clicks, or Limb Shift toward L.",
            "One change at a time.",
            "Confirm at 4-6 ft, then 20 yd bare shaft."
        )
        TearType.LOW_RIGHT -> listOf(
            "Fix vertical first (nock-low steps), then horizontal.",
            "Raise nock / lower rest, then re-paper.",
            "Then right: move rest left (in toward the riser), or Limb Shift toward R.",
            "Watch that lowering the rest does not cause contact. Check vane/cable clearance.",
            "One change at a time; finish with walk-back once paper is near bullet."
        )
    }

    private fun beginnerPreface(tear: TearType): List<String> = when (tear) {
        TearType.BULLET -> emptyList()
        TearType.LEFT, TearType.HIGH_LEFT, TearType.LOW_LEFT -> listOf(
            "Read the tear from the nock end: left means the nock went left of the point. Stand at 4-6 ft."
        )
        TearType.RIGHT, TearType.HIGH_RIGHT, TearType.LOW_RIGHT -> listOf(
            "Read the tear from the nock end: right means the nock went right of the point. Stand at 4-6 ft."
        )
        TearType.NOCK_HIGH, TearType.NOCK_LOW -> listOf(
            "Vertical tears are nock-height problems first. Do not touch Limb Shift or centershot yet."
        )
    }

    private fun beginnerClose(tear: TearType): List<String> = when (tear) {
        TearType.BULLET -> emptyList()
        else -> listOf(
            "Write down what you changed. If the next hole is worse, undo that one move before trying something else."
        )
    }

    private fun proOverlay(tear: TearType, core: List<String>): List<String> {
        val extra = when (tear) {
            TearType.LEFT -> listOf(
                "Pro: if rest-right and Limb Shift L both stall, check serving clearance and whether the shaft is weak for this DW/point."
            )
            TearType.RIGHT -> listOf(
                "Pro: right tears on a release gun are almost never a “stiff arrow” first guess — prove clearance (cables, shelf, vane) before spine."
            )
            TearType.HIGH_LEFT, TearType.HIGH_RIGHT, TearType.LOW_LEFT, TearType.LOW_RIGHT -> listOf(
                "Pro: lock vertical with two matching holes, then one horizontal click. Walk-back only after paper is near bullet."
            )
            TearType.NOCK_HIGH, TearType.NOCK_LOW -> listOf(
                "Pro: flipping vertical between shots is timing/tiller or grip, not another 1/32\" of nock."
            )
            TearType.BULLET -> listOf(
                "Pro: confirm at a longer paper distance, then 20 yd bare and a walk-back line before you call it done."
            )
        }
        return core + extra
    }
}
