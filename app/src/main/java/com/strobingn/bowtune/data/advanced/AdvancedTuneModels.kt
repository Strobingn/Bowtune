package com.strobingn.bowtune.data.advanced

/**
 * Structured Advanced Tuning library. Content is educational and compound+release
 * first; finger/recurve-only rules are labeled in the copy.
 */
data class TuneWalkStep(
    val title: String,
    val what: String,
    val why: String,
    val how: List<String>,
    val lookFor: List<String> = emptyList(),
    val changeNext: String = ""
)

enum class AdvancedSectionKind {
    PHYSICS,
    PROCEDURE,
    READ,
    CORRECT,
    WALKTHROUGH,
    CROSSLINK,
    CALLOUT
}

data class AdvancedSection(
    val id: String,
    val heading: String,
    val kind: AdvancedSectionKind,
    val paragraphs: List<String> = emptyList(),
    val bullets: List<String> = emptyList(),
    val steps: List<TuneWalkStep> = emptyList(),
    val relatedIds: List<String> = emptyList()
)

data class AdvancedGuide(
    val id: String,
    val title: String,
    val summary: String,
    val audience: String,
    val relatedMethods: String,
    val sections: List<AdvancedSection>
)

data class AdvancedGroup(
    val id: String,
    val title: String,
    val blurb: String,
    val guideIds: List<String>
)

object AdvancedTuneIds {
    const val BARE_SHAFT = "bare_shaft"
    const val FLETCHED = "fletched"
    const val NOCK_TUNE = "nock_tune"
    const val WALKBACK = "walkback"
    const val FRENCH = "french"
    const val PAPER = "paper"
    const val PLANING = "planing"
    const val BROADHEAD = "broadhead"
    const val TORQUE = "torque"
    const val YOKE = "yoke_horizontal"
    const val TILLER = "tiller_timing"
    const val DYNAMIC_SPINE = "dynamic_spine"
}

object AdvancedTuneCatalog {
    const val DISCLAIMER =
        "Educational only — not a coach, press, or manufacturer manual. These methods assume you already group, paper-tear, and log. Follow your bow’s manual and safe shop practice. One hardware change per group. Confirm outdoors before hunting. Bow Tune stays grayscale and does not invent chromatic “fix” colors."

    const val ONE_VARIABLE =
        "One variable at a time. Re-shoot at least two matched arrows (bares: two matched bares) after every move. If the next group is worse, revert that one change before touching anything else."

    const val CR_REST_MAP =
        "Easton CR rest map already shipped in Paper Tear (do not invert): RH nock-left tear → rest RIGHT in small clicks, or Limb Shift toward L (chase the tear). RH nock-right tear → rest LEFT (in toward the riser), or Limb Shift toward R. Right tears: prove vane/cable clearance before guessing stiff spine."

    val groups: List<AdvancedGroup> = listOf(
        AdvancedGroup(
            id = "flight",
            title = "Core flight",
            blurb = "What the shaft is doing after the string. Start here if you came for bare shaft.",
            guideIds = listOf(
                AdvancedTuneIds.BARE_SHAFT,
                AdvancedTuneIds.FLETCHED,
                AdvancedTuneIds.NOCK_TUNE,
                AdvancedTuneIds.DYNAMIC_SPINE
            )
        ),
        AdvancedGroup(
            id = "distance",
            title = "Distance confirmation",
            blurb = "Field-true checks once launch is close. French is the two-distance cut of walk-back.",
            guideIds = listOf(
                AdvancedTuneIds.WALKBACK,
                AdvancedTuneIds.FRENCH,
                AdvancedTuneIds.PLANING,
                AdvancedTuneIds.BROADHEAD
            )
        ),
        AdvancedGroup(
            id = "launch",
            title = "Launch & hardware",
            blurb = "Paper, grip, cam lean, timing. Use Tear / LIFT / brand Guides for the live tools; this is the theory and edge cases.",
            guideIds = listOf(
                AdvancedTuneIds.PAPER,
                AdvancedTuneIds.TORQUE,
                AdvancedTuneIds.YOKE,
                AdvancedTuneIds.TILLER
            )
        )
    )

    val all: List<AdvancedGuide> by lazy {
        listOf(
            BareShaftAdvanced.guide,
            FletchedAdvanced.guide,
            NockTuneAdvanced.guide,
            DynamicSpineAdvanced.guide,
            WalkBackAdvanced.guide,
            FrenchTuneAdvanced.guide,
            PlaningAdvanced.guide,
            BroadheadAdvanced.guide,
            PaperAdvanced.guide,
            TorqueAdvanced.guide,
            YokeHorizontalAdvanced.guide,
            TillerTimingAdvanced.guide
        )
    }

    fun byId(id: String): AdvancedGuide? = all.firstOrNull { it.id == id }

    fun groupFor(id: String): AdvancedGroup? = groups.firstOrNull { id in it.guideIds }

    fun titleOf(id: String): String = byId(id)?.title ?: id
}
