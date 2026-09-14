package com.strobingn.bowtune.data

object PaperTearGuidance {
    const val DISTANCE_TIP =
        "Paper tune at 6-8 feet from the paper - close enough that the arrow is still in the paradox window, far enough to clear the rest. Ten-plus feet often exaggerates tears and confuses diagnosis."

    const val GRIP_TORQUE_NOTE =
        "Grip torque false tears: a death-grip or torqueing the riser can invent left/right (and sometimes vertical) tears. Relax the bow hand, open palm pressure, and re-shoot before chasing hardware. Confirm with a second arrow and a second shooter if possible."

    const val GENERAL_HARDWARE =
        "General order of attack: (1) form and grip, (2) rest micro-adjustment, (3) nock height for vertical, (4) yoke twists / cable guard / limb shift for horizontal, (5) centershot as a walk-back fine-tune. D-loop first - set it before you chase nock height forever."

    fun stepsFor(tear: TearType): List<String> = when (tear) {
        TearType.BULLET -> listOf(
            "Nice - you have a bullet hole. Do not chase perfection into a spiral.",
            "Verify with 2-3 more shots and a relaxed grip.",
            "Move outdoors to bare-shaft at ~20 yd, then walk-back.",
            "Later: broadheads vs field points before hunting."
        )
        TearType.NOCK_HIGH -> listOf(
            "Confirm grip is relaxed (false vertical tears happen).",
            "Lower nocking point / D-loop slightly (small moves: 1/32-1/16\").",
            "Or raise the rest a hair if nock height is already in a good range.",
            "Re-paper at 6-8 ft; then bare-shaft check vertical at 20 yd.",
            "If stubborn: check cam timing / tiller and rest vertical play."
        )
        TearType.NOCK_LOW -> listOf(
            "Confirm grip and that the arrow is not hitting the rest shelf on launch.",
            "Raise nocking point / D-loop slightly.",
            "Or lower the rest slightly if clearance allows.",
            "Re-paper at 6-8 ft; confirm with bare shaft.",
            "Check cam timing if vertical keeps flipping."
        )
        TearType.LEFT -> listOf(
            "RH shooter left tear (nock left): often weak spine feel, centershot, or cable load.",
            "First: retest with neutral grip - torque often fakes left tears.",
            "Move rest slightly toward the riser (in) in small clicks / 1/64\".",
            "Or add a half-twist to the yoke/cable on the side that pulls the nock right (brand-specific).",
            "Mathews: small Limb Shift toward correcting left; PSE: EZ.220 / cable guard as appropriate.",
            "Re-paper, then walk-back before big spine changes."
        )
        TearType.RIGHT -> listOf(
            "RH shooter right tear (nock right): often stiff spine feel or rest too far in.",
            "Retest grip first.",
            "Move rest slightly away from riser (out).",
            "Or yoke/limb-shift opposite of a left correction.",
            "Mathews Limb Shift / PSE cable-side adjustments - tiny moves.",
            "Re-paper at 6-8 ft; finish with walk-back."
        )
        TearType.HIGH_LEFT -> listOf(
            "Fix vertical first (nock-high steps), then left.",
            "Lower nock point or raise rest then recheck paper.",
            "Then address left: rest in / yoke or limb-shift as for left tear.",
            "Avoid changing two axes in one big move - one change, re-shoot.",
            "Grip check between every change."
        )
        TearType.HIGH_RIGHT -> listOf(
            "Vertical first (nock-high), then right.",
            "Lower nock / raise rest then re-paper.",
            "Then rest out / yoke-limb opposite of left tear.",
            "One axis at a time; confirm grip.",
            "Bare shaft after paper cleans up."
        )
        TearType.LOW_LEFT -> listOf(
            "Vertical first (nock-low), then left.",
            "Raise nock / lower rest then re-paper.",
            "Then left corrections (rest in / yoke / limb shift).",
            "One change per group of shots.",
            "Confirm at 6-8 ft, then 20 yd bare shaft."
        )
        TearType.LOW_RIGHT -> listOf(
            "Vertical first (nock-low), then right.",
            "Raise nock / lower rest then re-paper.",
            "Then right corrections (rest out / yoke / limb).",
            "Watch clearance so lowering rest does not cause contact.",
            "Finish with walk-back once paper is near bullet."
        )
    }
}
