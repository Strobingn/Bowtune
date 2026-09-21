package com.strobingn.bowtune.data

data class ChecklistTemplate(
    val id: String,
    val name: String,
    val blurb: String,
    val items: List<ChecklistItem>
)

object ChecklistTemplates {
    val FULL = ChecklistTemplate(
        id = "full",
        name = "Full tune",
        blurb = "Six-phase shop sequence from timing through broadheads.",
        items = TuneChecklistCatalog.items
    )

    val PAPER_ONLY = ChecklistTemplate(
        id = "paper",
        name = "Paper only",
        blurb = "Short indoor paper session.",
        items = listOf(
            ChecklistItem(
                "p_dist",
                "1. Distance",
                "Stand 4-6 feet from the paper",
                "Easton starting distance for a mechanical release. After a clean hole, step back to confirm."
            ),
            ChecklistItem(
                "p_grip",
                "2. Grip",
                "Neutral grip, two confirmation arrows",
                "Torque fakes left/right tears. Relax the bow hand before any rest click."
            ),
            ChecklistItem(
                "p_vert",
                "3. Vertical",
                "Fix nock-high / nock-low first",
                "Nock height or rest height only. One change, re-paper."
            ),
            ChecklistItem(
                "p_horiz",
                "4. Horizontal",
                "Rest right for left tear; rest left for right tear",
                "RH release: left tear → rest right or Limb Shift L. Right tear → rest left (in toward the riser) or Limb Shift R. Check vane clearance on right tears."
            ),
            ChecklistItem(
                "p_log",
                "5. Log",
                "Save the tear and the change",
                "Paper Tear history + one adjustment log row. Do not stack two hardware moves."
            )
        )
    )

    val HUNTING = ChecklistTemplate(
        id = "hunting",
        name = "Hunting season",
        blurb = "Pre-season proof before you leave the shop.",
        items = listOf(
            ChecklistItem(
                "h_paper",
                "1. Paper",
                "Bullet at 4-6 ft with hunting arrow",
                "Same point weight you will hunt. Confirm grip is not inventing the hole."
            ),
            ChecklistItem(
                "h_bare",
                "2. Bare shaft",
                "Bare vs fletched at 20 yd",
                "Vertical first (nock), then leftover left/right."
            ),
            ChecklistItem(
                "h_walk",
                "3. Walk-back",
                "10 / 20 / 30 line",
                "Fan = rest. Parallel offset = sight."
            ),
            ChecklistItem(
                "h_bh",
                "4. Broadheads",
                "Fixed or mech vs field points 20–40 yd",
                "Log the comparison. One change if they do not share a group."
            ),
            ChecklistItem(
                "h_due",
                "5. Next due",
                "Set the next cam / cable check date",
                "Gear → Service. Stretch shows up as timing and leftover tears."
            )
        )
    )

    val BARE_SHAFT = ChecklistTemplate(
        id = "bareshaft",
        name = "Bare shaft",
        blurb = "Field-first sequence from the Advanced Tuning library.",
        items = listOf(
            ChecklistItem(
                "bs_setup",
                "1. Setup",
                "DL, timing, D-loop, nock 1/8–1/4 in, rest at brand start",
                "Do not start with a random rest. One variable later."
            ),
            ChecklistItem(
                "bs_optional_paper",
                "2. Optional paper",
                "One 4–6 ft hole if you need a contact check",
                "Ugly changing tears = clearance/grip, not a rest religion. Tear tab keeps the CR map."
            ),
            ChecklistItem(
                "bs_nock",
                "3. Nock point",
                "Two bare passes at 20 yd; high → nock up (Easton)",
                "Do not move the rest while vertical is open. Guides → Advanced → Bare shaft."
            ),
            ChecklistItem(
                "bs_spine_rest",
                "4. Horizontal",
                "Spine vs centershot after grip/powder",
                "RH CR bare left → rest right or weak. Bare right → rest left / clearance."
            ),
            ChecklistItem(
                "bs_confirm",
                "5. Confirm",
                "Walk-back or French, then broadheads",
                "Fan = rest. Parallel = sight. Same CR directions as Tear."
            )
        )
    )

    val INDOOR = ChecklistTemplate(
        id = "indoor",
        name = "Indoor league",
        blurb = "18–20 yd group focus, no broadheads.",
        items = listOf(
            ChecklistItem(
                "i_setup",
                "1. Active setup",
                "Confirm the indoor bow is active",
                "Home / Gear — switch profiles so logs attach to the right bow."
            ),
            ChecklistItem(
                "i_paper",
                "2. Paper",
                "4-6 ft bullet with league arrows",
                "Same spine and point you will score with."
            ),
            ChecklistItem(
                "i_group",
                "3. Groups",
                "Log 20 yd group size and conditions",
                "Sessions → Group / score. Indoor, note lighting and face."
            ),
            ChecklistItem(
                "i_form",
                "4. Form",
                "Optional Vision freeze + auto notes",
                "Shoulder line and bow-arm tension before you chase rest clicks."
            )
        )
    )

    val all: List<ChecklistTemplate> = listOf(FULL, PAPER_ONLY, HUNTING, BARE_SHAFT, INDOOR)

    fun byId(id: String): ChecklistTemplate =
        all.firstOrNull { it.id == id } ?: FULL
}
