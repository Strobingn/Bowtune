package com.strobingn.bowtune.data.advanced

internal object FletchedAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.FLETCHED,
        title = "Fletched arrow tuning",
        summary = "How vanes mask launch error, when 20 yd groups lie, clearance, helical vs offset, and why you fletch-tune only after bare is close.",
        audience = "Advanced · Compound + release",
        relatedMethods = "Bare shaft, nock tune, paper, broadhead, planing",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Physics: restoring moment vs launch error",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "A vane is a small wing behind the center of mass. Any angle of attack creates a moment that rotates the shaft toward the velocity vector and a drag increment that bleeds speed. Helical and large offset increase both the moment and the drag. That is why a loud helical can make a mediocre launch look “tuned” at 20 yd and then throw a fixed blade at 40.",
                    "The fletched group is a convolution of launch dispersion, residual paradox, and vane steering. If you only ever watch fletched holes, you are looking at the steered result. Two wrongs (weak shaft + rest compensation, or contact that happens to kick the same way every time) can print a tight 20 yd group. Broadheads, wind, and a longer pin undo the conspiracy.",
                    "Spin (RPM) from helical/offset averages some manufacturing asymmetry and can stabilize a shaft that is nock-clocked poorly. It does not fix a 400 gr fixed head that is planing because the launch angle is still wrong. Spin also changes how a clearance rub presents — a once-per-rev tick becomes a spiral smear on powder."
                )
            ),
            AdvancedSection(
                id = "when_lie",
                heading = "When fletched groups lie",
                kind = AdvancedSectionKind.READ,
                paragraphs = listOf(
                    "Believe a fletched group only after you know what the bare is doing, or after a walk-back/French that does not fan. Otherwise you are scoring your vanes, not your bow."
                ),
                bullets = listOf(
                    "Tight 20, fan at 30–40: rest/centershot or dynamic spine. Vanes were hiding the angle.",
                    "Tight field points, fixed blades left/right: same story plus blade planing. See Broadhead.",
                    "Group tight but ugly paper: you are steering out a launch you have not fixed. Fine for a rainy league night; bad as a hunting tune.",
                    "One color vane always hits out: that shaft’s clock, weight, or a glue drip — not a rest click.",
                    "Indoor 18 m league groups can look finished on a weak/stiff pair because the vane has little time to show the miss and a lot of time to steer. Take one 30–40 yd fletched group before you call it."
                )
            ),
            AdvancedSection(
                id = "clearance",
                heading = "Vane clearance (the actual procedure)",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What clearance is",
                        what = "The vane, wrap, and nock must pass the rest, cables, and riser without a touch while the shaft is still bent. A touch is an unplanned steering input.",
                        why = "Contact reads as minnowing, ragged paper, random L/R, and “stiff” right tears on a release bow. Tear already tells you to prove clearance on a nock-right before you buy weaker spines.",
                        how = listOf(
                            "Dry: draw (or press, if you must inspect at brace+cam) and look at the vane path through the rest. Cock vane index is a clearance choice as much as a tradition.",
                            "Powder or lipstick on launcher, containment, cables, shelf. Shoot two fletched.",
                            "Any wipe on a vane or wrap = contact. Note which vane and which object."
                        ),
                        lookFor = listOf("Clean powder", "Repeatable wipe on the same vane = geometry", "Random wipes = nock pinch or a loose rest arm"),
                        changeNext = "Fix contact before any spine or rest story."
                    ),
                    TuneWalkStep(
                        title = "2. How to clear it (one change)",
                        what = "Rotate nock in 1/32-turn steps (Easton clearance move), change cock-vane index, shorten vane height, or reduce helical — not all at once.",
                        why = "A 1/32 nock rotation is nock-tuning’s little brother. It can also move you off the stiff-plane sweet spot. If groups change a lot, you just discovered a clock issue; open Nock tuning.",
                        how = listOf(
                            "1/32 turn, re-powder, re-shoot.",
                            "If you need more than ~1/8 turn, stop and set a proper index for this rest (drop-away vs blade vs containment).",
                            "Cable rub: rest in, or a yoke/cable-guard move — that is now a horizontal hardware change. Do it as its own group (see Yoke / Limb Shift).",
                            "Never “fix” clearance by throwing the rest 1/8 in outboard and calling it centershot."
                        ),
                        lookFor = listOf("Powder stays clean for three shots", "Paper tears get more repeatable even if not yet bullet"),
                        changeNext = "Then return to bare shaft. Fletched-only clearance is not a finished tune."
                    )
                )
            ),
            AdvancedSection(
                id = "helical",
                heading = "Helical vs offset",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Offset is a yaw of a still-flat vane. Helical is a wrap around the shaft. Both create spin; helical usually more, and with more rear drag. 3° offset on a 2 in vane is not the same steering as a 3° helical on a 4 in shield.",
                    "More steering: faster recovery, more speed loss, more wind drift, more ability to hide a 1/2 in bare miss at 20 yd. Less steering (straight fletch, small offset, tiny vanes): truer report of the launch, harsher if the launch is dirty.",
                    "If you change helical after a bare tune, re-bare. You changed FOC slightly (vane mass/position) and you changed how much error is hidden. Hunting vanes vs indoor feathers/vanes are different arrows — treat them as a second setup in Gear."
                )
            ),
            AdvancedSection(
                id = "after_bare",
                heading = "Fletched-only work — after bare is close",
                kind = AdvancedSectionKind.PROCEDURE,
                paragraphs = listOf(
                    "Once 20 yd bares share a group with fletched within today’s dispersion, fletched tuning is group shape, vane choice, and confirmation — not a new rest religion."
                ),
                bullets = listOf(
                    "Do: 20 and 40 yd fletched groups, walk-back, broadhead vs field, nock-clock the fletched set to the bare winner.",
                    "Do not: start over with 1/8 in rest moves because a 20 yd fletched group sat 1/2 in left in a breeze.",
                    "If you cannot bare (range rules), French/walk-back plus powder is the substitute. It is worse, not forbidden.",
                    "Log fletched groups in Sessions as Group, not as Bare shaft, or you will poison the hint parser."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.NOCK_TUNE,
                    AdvancedTuneIds.BROADHEAD,
                    AdvancedTuneIds.WALKBACK
                )
            )
        )
    )
}

internal object NockTuneAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.NOCK_TUNE,
        title = "Nock tuning (done with bare shafts)",
        summary = "Clocking/indexing on bares: stiff-plane reality, 90° procedure, reading POI shifts, when to replace a nock vs rotate, consistency checks.",
        audience = "Advanced · Compound + release. Works on any carbon; finger shooters use the same clocking, different paper chart.",
        relatedMethods = "Bare shaft, fletched clearance, paper, dynamic spine",
        sections = listOf(
            AdvancedSection(
                id = "what",
                heading = "What nock tuning is",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Carbon is not isotropic. Most shafts have a stiff plane and a weak plane (plus a seam, a spine index mark, or nothing). The nock’s clock sets which plane faces the bow at launch. Nock tuning is rotating that clock until this bow, this rest, and this release get the tightest, most repeatable bare flight.",
                    "It is not “turn the cock vane so it looks right.” Indexing for clearance and nock-tuning for spine plane can fight each other. If they do, clearance wins first, then you pick the nearest good clock, then you fletch to that clock.",
                    "Shop spine testers (RAM, Firenock PAPS, etc.) find a peak deflection axis. That axis is a starting mark, not automatically the best clock on your bow. The bow is the tester that matters. Use the shop mark as 0° so you can write numbers."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — clocking with bare shafts",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What you are about to do",
                        what = "You will shoot the same bare shaft (or a matched pair) at four nock clocks — 0°, 90°, 180°, 270° — and keep the clock with the best repeatable flight. Then you will transfer that clock to the dozen.",
                        why = "A 90° error can open a 20 yd bare group by more than a rest click. People then “tune” the rest to one shaft’s bad clock and the other eleven never match.",
                        how = listOf(
                            "Two to four matched bares. Number them. Same points.",
                            "Mark the nock and the shaft with a paint line at 0° (manufacturer index if it exists).",
                            "Dead air, ~18–20 yd, or paper at 6 ft if you cannot go outside — POI at 20 is the better teacher; paper is acceptable for a first sort."
                        ),
                        lookFor = listOf("Marks that survive shooting", "Nocks that actually hold position — a loose nock is a different problem"),
                        changeNext = "Do not move rest, nock height, or point weight during the clock test."
                    ),
                    TuneWalkStep(
                        title = "2. Why the group moves when you rotate",
                        what = "Each clock presents a different bending stiffness to the paradox. Weak-plane-into-the-bend over-flexes (CR: often more left bias on a RH release). Stiff-plane-into-the-bend under-flexes. You are not changing centershot; you are changing the shaft the bow thinks it has.",
                        why = "If POI jumps 3 in at 20 yd between clocks, that shaft is more variable than your tune. If POI barely moves, either the shaft is very round or you are not getting a clean launch (then clearance/nock fit first).",
                        how = listOf(
                            "Shoot 2–3 bares at 0°. Record group diameter and C-C vs a fletched control (or vs a drawn aim point if you have no fletched yet).",
                            "Rotate every test nock 90° the same direction. Re-shoot.",
                            "Repeat through 180° and 270°."
                        ),
                        lookFor = listOf(
                            "Best clock = tightest bare group AND most repeatable POI (two groups, not one)",
                            "If two clocks tie, pick the one closer to the fletched center and/or with less vertical scatter",
                            "A single flyer that ignores clock → weigh, spin, swap that nock, then cull"
                        ),
                        changeNext = "Winner clock only. Do not average 0° and 90°."
                    ),
                    TuneWalkStep(
                        title = "3. Reading impact shifts",
                        what = "Plot four POIs. A clean shaft walks in a small loop or a line as you clock. A damaged shaft jumps.",
                        why = "A systematic walk means the plane is real and you can pick a minimum. A jump means insert concentricity, a cracked nock, or a bend.",
                        how = listOf(
                            "Photograph the face at each clock with a label in the frame.",
                            "If you also paper: the cleanest repeatable tear is supporting evidence, not a veto of a tight 20 yd group. Distance POI wins for field work.",
                            "Optional refinement: from the winning 90° slot, try ±30–45° if you still have time. Diminishing returns for hunting; more useful for indoor 18 m."
                        ),
                        lookFor = listOf("Winner that repeats after you rotate away and back", "No new powder wipe at the winner — clock cannot buy contact"),
                        changeNext = "Mark the winner on the carbon, not only on the nock. Nocks get replaced."
                    ),
                    TuneWalkStep(
                        title = "4. When to change nocks vs rotate",
                        what = "Rotate first. Replace the nock when the nock is the defect. Cull the shaft when the shaft is the defect.",
                        why = "A new nock of a different throat or brand is a new tune variable (serving fit, pinch, mass). Match the set.",
                        how = listOf(
                            "Replace if: cracked ears, twisted throat, shiny smear from cable, loose on the serving, different model than the set, or it will not hold a clock.",
                            "After a nock swap, re-clock that one shaft. Do not assume the old paint line still maps.",
                            "If every clock is wild on one shaft and the nock is new: spin test, check insert (hot glue blobs, out-of-round), then discard. One lemon will train you to chase rest clicks forever.",
                            "Do not mix bushings / pin nocks / push-ins in one “matched” bare set."
                        ),
                        lookFor = listOf("Set of nocks from one bag", "Serving diameter that gives a light click, not a pry"),
                        changeNext = "Index fletched arrows to the winning clock, then powder-check vane path."
                    ),
                    TuneWalkStep(
                        title = "5. Consistency checks",
                        what = "Clocking is wasted if the dozen is not a dozen.",
                        why = "Weight, spine, and straightness scatter become “tune” in people’s notebooks.",
                        how = listOf(
                            "Weigh finished bares. Park anything > ~2–3 gr off the mean for hunting; tighter for indoor.",
                            "Spin on a tester or at least on your nails. Wobble at the point = insert; wobble mid = bend.",
                            "Re-check clocks after a pass-through or a hard miss — nocks rotate and carbon takes a memory.",
                            "Nock-tune in dead air. A 10 mph crosswind will pick a “winner” that is just drift."
                        ),
                        lookFor = listOf("Written clock for the setup in Gear notes", "Same cock-vane relationship on every fletched shaft"),
                        changeNext = "Return to bare-shaft vertical/horizontal. Clock is a shaft variable, not a rest variable."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.FLETCHED,
                    AdvancedTuneIds.PAPER,
                    AdvancedTuneIds.DYNAMIC_SPINE
                )
            )
        )
    )
}

internal object DynamicSpineAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.DYNAMIC_SPINE,
        title = "Dynamic spine laboratory",
        summary = "Levers that change how the shaft actually bends: point, length, DW, cam, nock fit, FOC. Chart spine is the opening bid.",
        audience = "Advanced · Compound + release",
        relatedMethods = "Bare shaft, nock tune, paper, broadhead, Home spine helper",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Static vs dynamic",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Static spine (ATA): deflection of a 28 in shaft under 1.94 lb at center. Useful for manufacturing and charts. It does not know your cam’s force curve, your 29.5 in LIFT, or a 150 gr head.",
                    "Dynamic spine is energy in versus stiffness in, plus front mass. More energy or more front mass → more bend → weaker dynamic. Less energy or less front mass → stiffer dynamic. Length is a huge stiffness lever (roughly a cubic-ish effect in simple beam terms — you will feel 1/2 in).",
                    "Nock fit and serving: a pinched nock holds the rear off-axis and can look stiff or just random. A sloppy nock lets the rear start late. Neither is a chart problem."
                )
            ),
            AdvancedSection(
                id = "levers",
                heading = "Walkthrough — change one lever",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. Inventory the current dynamic",
                        what = "Write DW, DL, IBO/real fps if you have it, cut length, point+insert grains, FOC, nock model, and the last 20 yd bare L/R.",
                        why = "Without the list you will change two levers and credit the wrong one.",
                        how = listOf(
                            "Home → Spine helper for a chart range. If you are already two spine groups off the chart, believe the chart before you invent a 1/4 in rest offset.",
                            "Home → FOC if the hunting arrow is a different front end than the field-point tune arrow."
                        ),
                        lookFor = listOf("Hunting arrow and practice arrow called out as same or different"),
                        changeNext = "Pick the cheapest reversible lever."
                    ),
                    TuneWalkStep(
                        title = "2. The lever table (one at a time)",
                        what = "Each line is a separate session.",
                        why = "See Bare shaft spine walkthrough for RH CR left=weak.",
                        how = listOf(
                            "Point/insert +25–50 gr → weaker dynamic. First lever for a persistent RH CR bare-left after rest is sane.",
                            "Point −25–50 gr → stiffer dynamic. Only after clearance and rest-left have lost.",
                            "Cut 1/2 in → stiffer, not reversible. Do this on one test shaft.",
                            "DW ±1–2 lb → weaker when you add weight. Recheck timing/stops; this is a bow change.",
                            "FOC up (more front, same shaft) → weaker dynamic plus more stability once flying. Broadheads like FOC; indoor speed does not.",
                            "Nock/serving: fit first, clock second. Do not call pinch “weak spine.”"
                        ),
                        lookFor = listOf("Bare L/R moves the direction you predicted", "If it does not, revert — you were not on a spine problem"),
                        changeNext = "Re-bare at 20, then a second distance (Planing) before you buy a new dozen."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.NOCK_TUNE,
                    AdvancedTuneIds.BROADHEAD,
                    AdvancedTuneIds.PAPER
                )
            )
        )
    )
}
