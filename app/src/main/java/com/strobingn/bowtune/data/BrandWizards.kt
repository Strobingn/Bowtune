package com.strobingn.bowtune.data

data class BrandWizardStep(
    val id: String,
    val title: String,
    val instructions: String,
    val fields: List<LiftVerticalTunePlan.LogField>
)

data class BrandWizard(
    val id: String,
    val brand: String,
    val title: String,
    val summary: String,
    val steps: List<BrandWizardStep>
)

object BrandWizardCatalog {
    val all: List<BrandWizard> = listOf(
        BrandWizard(
            id = "pse_ez220_short",
            brand = "PSE",
            title = "PSE EZ.220 short tune",
            summary = "Timing → paper at 4-6 ft → EZ.220 / rest → bare → walk-back.",
            steps = listOf(
                BrandWizardStep(
                    "sync",
                    "Sync and D-loop",
                    "Set draw length. Confirm cable-stop contact / cam sync. Install D-loop before nock-height chasing.",
                    listOf(
                        LiftVerticalTunePlan.LogField("dl", "Draw length (in)"),
                        LiftVerticalTunePlan.LogField("sync", "Cam sync notes")
                    )
                ),
                BrandWizardStep(
                    "paper",
                    "Paper at 4-6 ft",
                    "Relaxed grip. Vertical first. RH left tear: rest right or EZ.220 to chase left. RH right tear: rest left (in toward the riser); check vanes.",
                    listOf(
                        LiftVerticalTunePlan.LogField("tear", "Tear type"),
                        LiftVerticalTunePlan.LogField("grip", "Grip retest Y/N")
                    )
                ),
                BrandWizardStep(
                    "ez",
                    "EZ.220 / rest",
                    "One click or a tiny EZ.220 move — not both. Re-paper.",
                    listOf(
                        LiftVerticalTunePlan.LogField("move", "What changed"),
                        LiftVerticalTunePlan.LogField("result", "New tear")
                    )
                ),
                BrandWizardStep(
                    "finish",
                    "Bare + walk-back",
                    "20 yd bare for leftover vertical/horizontal. Walk-back for centershot. Log EZ.220 in Gear.",
                    listOf(
                        LiftVerticalTunePlan.LogField("bare", "Bare H/L + L/R @20"),
                        LiftVerticalTunePlan.LogField("walk", "Walk-back note")
                    )
                )
            )
        ),
        BrandWizard(
            id = "hoyt_xts_short",
            brand = "Hoyt",
            title = "Hoyt XTS short tune",
            summary = "Module/timing → paper → yoke → bare → walk-back.",
            steps = listOf(
                BrandWizardStep(
                    "module",
                    "Module and timing",
                    "Set module / draw length. Check XTS timing marks or cable-stop sync for your year.",
                    listOf(
                        LiftVerticalTunePlan.LogField("module", "Module / DL"),
                        LiftVerticalTunePlan.LogField("timing", "Timing notes")
                    )
                ),
                BrandWizardStep(
                    "paper",
                    "Paper at 4-6 ft",
                    "Fix nock-high/low first. Combo tears: vertical, then horizontal. One change at a time.",
                    listOf(
                        LiftVerticalTunePlan.LogField("tear", "Tear type"),
                        LiftVerticalTunePlan.LogField("nock", "Nock move")
                    )
                ),
                BrandWizardStep(
                    "yoke",
                    "Yoke / rest",
                    "Keep rest near recommended centershot. RH left: rest right or yoke that chases L. RH right: rest left (in toward the riser).",
                    listOf(
                        LiftVerticalTunePlan.LogField("yoke", "Yoke / rest change"),
                        LiftVerticalTunePlan.LogField("result", "New tear")
                    )
                ),
                BrandWizardStep(
                    "finish",
                    "Bare + walk-back",
                    "20 yd bare, then walk-back. Record yoke twists in Gear.",
                    listOf(
                        LiftVerticalTunePlan.LogField("bare", "Bare @20"),
                        LiftVerticalTunePlan.LogField("walk", "Walk-back")
                    )
                )
            )
        ),
        BrandWizard(
            id = "bowtech_deadlock_short",
            brand = "Bowtech",
            title = "Bowtech DeadLock short tune",
            summary = "Lock cams → paper → tiny DeadLock slide → re-lock → bare.",
            steps = listOf(
                BrandWizardStep(
                    "lock",
                    "Draw length and lock",
                    "Set DL. Confirm DeadLock screws are at spec before the first group. Mark a reference line.",
                    listOf(
                        LiftVerticalTunePlan.LogField("dl", "Draw length"),
                        LiftVerticalTunePlan.LogField("mark", "Reference mark")
                    )
                ),
                BrandWizardStep(
                    "paper",
                    "Paper at 4-6 ft",
                    "Diagnose tear. Vertical = nock/rest height — do not use DeadLock for nock-high/low.",
                    listOf(
                        LiftVerticalTunePlan.LogField("tear", "Tear type")
                    )
                ),
                BrandWizardStep(
                    "slide",
                    "DeadLock micro-slide",
                    "Unlock, slide a tiny amount to chase the horizontal tear (L for left, R for right), re-torque evenly, re-paper.",
                    listOf(
                        LiftVerticalTunePlan.LogField("slide", "Slide direction / amount"),
                        LiftVerticalTunePlan.LogField("torque", "Re-torque done Y/N"),
                        LiftVerticalTunePlan.LogField("result", "New tear")
                    )
                ),
                BrandWizardStep(
                    "finish",
                    "Bare + walk-back",
                    "Bare shaft then walk-back. Save DeadLock notes in Gear for the next string job.",
                    listOf(
                        LiftVerticalTunePlan.LogField("bare", "Bare @20"),
                        LiftVerticalTunePlan.LogField("walk", "Walk-back")
                    )
                )
            )
        ),
        BrandWizard(
            id = "elite_set_short",
            brand = "Elite",
            title = "Elite S.E.T. short tune",
            summary = "Timing → paper → S.E.T. clicks → re-lock → bare.",
            steps = listOf(
                BrandWizardStep(
                    "time",
                    "Draw length and timing",
                    "Set DL and cam timing first. S.E.T. is a horizontal tool, not a vertical one.",
                    listOf(
                        LiftVerticalTunePlan.LogField("dl", "Draw length"),
                        LiftVerticalTunePlan.LogField("timing", "Timing notes")
                    )
                ),
                BrandWizardStep(
                    "paper",
                    "Paper at 4-6 ft",
                    "Grip check. Vertical first. Then L/R.",
                    listOf(
                        LiftVerticalTunePlan.LogField("tear", "Tear type")
                    )
                ),
                BrandWizardStep(
                    "set",
                    "S.E.T. increment",
                    "Small S.E.T. move toward L to chase a left tear, toward R to chase a right tear. Re-lock / recheck torque. Re-paper.",
                    listOf(
                        LiftVerticalTunePlan.LogField("set", "S.E.T. change"),
                        LiftVerticalTunePlan.LogField("result", "New tear")
                    )
                ),
                BrandWizardStep(
                    "finish",
                    "Bare + walk-back",
                    "20 yd bare, walk-back, store S.E.T. setting on the setup card.",
                    listOf(
                        LiftVerticalTunePlan.LogField("bare", "Bare @20"),
                        LiftVerticalTunePlan.LogField("walk", "Walk-back")
                    )
                )
            )
        )
    )

    fun byId(id: String): BrandWizard? = all.firstOrNull { it.id == id }
}
