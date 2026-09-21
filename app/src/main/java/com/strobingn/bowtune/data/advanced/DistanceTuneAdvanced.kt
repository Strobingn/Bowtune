package com.strobingn.bowtune.data.advanced

internal object WalkBackAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.WALKBACK,
        title = "Walk-back tuning",
        summary = "Vertical line, same pin, 10/20/30 (and out). Fan vs parallel offset. Centershot vs windage. Rest / Limb Shift. How this differs from French.",
        audience = "Advanced · Compound + release (works with any sighted bow)",
        relatedMethods = "French, bare shaft, yoke/Limb Shift, paper, LIFT walk-back block",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Physics: angle vs parallel miss",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "A rest that is laterally wrong launches the shaft with a yaw angle. Impact error then grows with distance — a fan. A sight that is laterally wrong aims the whole system at the wrong place; the miss is roughly the same angular sight error at every distance, which on a vertical line looks like a parallel column sitting left or right of the line.",
                    "That is the whole method. You are not “tuning groups.” You are separating a launch-angle problem (rest, cam lean, leftover spine) from an aiming-reference problem (windage, second axis, a canted bow).",
                    "Second-axis error on a multi-pin or a slider will also walk holes off a vertical line as the pin moves down the housing. Level the sight’s 2nd axis before you accuse the rest. A canted bubble does the same crime."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — the line",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What you set up",
                        what = "A truly vertical line (plumb tape or a hung string) and one aim mark high on that line. You will use ONE pin — usually your 20 yd pin — at every distance. Arrows will hit lower as you walk back. You only care about left/right versus the line.",
                        why = "If the line is not plumb, you will “tune” a lean. If you switch pins, you mix sight-tape error into centershot.",
                        how = listOf(
                            "Plumb the line. Mark a high aim point.",
                            "Confirm 2nd axis and that you can see bubble without canting.",
                            "Start with a rest already near brand centershot and a bare vertical that is already done. Walk-back is a horizontal finisher, not a first-day tool.",
                            "Dead-enough air. Log wind in Sessions if it is not dead."
                        ),
                        lookFor = listOf("Plumb line", "One pin", "Nock height not in play today"),
                        changeNext = "Shoot before you touch the rest. People pre-click."
                    ),
                    TuneWalkStep(
                        title = "2. Distances and how to shoot",
                        what = "10 / 20 / 30 yd is enough to see a fan (LIFT uses this). 40–50 if you have the range and the form. Two passes.",
                        why = "One pass is weather and a punched shot. Two passes that rhyme are a tune signal.",
                        how = listOf(
                            "Aim the same mark with the same pin at 10, 20, 30 (then 40 if you want).",
                            "One or three arrows per distance per pass. Do not sight between distances.",
                            "Photograph or mark holes. Measure horizontal displacement from the line at each distance."
                        ),
                        lookFor = listOf("A pattern, not a hero arrow at 30"),
                        changeNext = "Classify fan vs parallel before any click."
                    ),
                    TuneWalkStep(
                        title = "3. What the pattern tells you",
                        what = "Fan / error grows with distance = rest / centershot / leftover horizontal launch (or cam lean). Parallel offset = sight windage (or a consistent cant).",
                        why = "This is the same split as Logs → Walk-back chips and the LIFT decision tree. Do not invent a third category for a 1 in wobble — that is dispersion.",
                        how = listOf(
                            "Sketch the column. If 10 is 0, 20 is 1 in left, 30 is 2 in left → fan left.",
                            "If 10/20/30 are all ~1 in left → parallel. Move the sight, not the rest.",
                            "A backwards fan (error shrinks) usually means you already over-corrected the rest or you mixed pins."
                        ),
                        lookFor = listOf("Fan left (RH): candidate rest RIGHT — same CR map as paper nock-left / bare left", "Fan right: candidate rest LEFT (in) — same as paper nock-right"),
                        changeNext = "One tiny rest move OR one Limb Shift click, not both. Then a second full walk-back."
                    ),
                    TuneWalkStep(
                        title = "4. Correction procedure",
                        what = "Rest micro for a fan. Sight windage for a parallel. Then resight distances after the rest is done — the groups will have moved.",
                        why = "If you move sight first on a fan, every pin will be “right” at one distance and wrong at the others. That is how tapes get blamed.",
                        how = listOf(
                            "Fan: 1/64 in or one rest click in the CR direction above. Re-walk 10–20–30.",
                            "If you would rather use Limb Shift (Mathews) / EZ.220 / yoke / DeadLock / S.E.T.: one indexed click that chases a left fan toward L, etc. Rest stays put. See Yoke / Limb Shift and Brand Guides.",
                            "Parallel: windage only. Confirm the bubble is still centered — a cant looks parallel-ish.",
                            "When the column sits on the line, stop. Do not chase 1/4 in at 40 yd in a breeze."
                        ),
                        lookFor = listOf("Column on the line within group width", "Bare at 20 still sharing vertical — you did not bump nock by accident"),
                        changeNext = "Broadheads or a French two-distance confirm if you want a faster re-check next week."
                    )
                )
            ),
            AdvancedSection(
                id = "vs_french",
                heading = "Walk-back vs French vs bare",
                kind = AdvancedSectionKind.CROSSLINK,
                paragraphs = listOf(
                    "Walk-back uses three-plus distances and shows the shape of the error (fan vs parallel). French uses a very-near + far pair and isolates sight (near) vs rest (far) with less walking. Bare shaft at one distance cannot separate rest angle from spine or wind; multi-distance planing can. Use bare to get close, walk-back or French to finish centershot, planing to see if leftover is spine."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.FRENCH,
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.PLANING,
                    AdvancedTuneIds.YOKE
                )
            )
        )
    )
}

internal object FrenchTuneAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.FRENCH,
        title = "French tuning",
        summary = "Standard compound close/far (near isolates sight, far isolates rest). What the split means. Recurve point-weight and crawl variants labeled.",
        audience = "Advanced · Compound + release first. Finger/barebow variants are labeled.",
        relatedMethods = "Walk-back, bare shaft, planing, yoke/Limb Shift",
        sections = listOf(
            AdvancedSection(
                id = "what",
                heading = "What French tuning is (and is not)",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "In compound shop talk, French tuning is a two-distance centershot method: a very close shot that has not had time to react to a rest error, and a far shot that has. You set SIGHT windage on the close shot, REST on the far shot, and iterate. It is a compressed walk-back, not a different physics.",
                    "It is not paper. It is not bare-vs-fletched by itself (that variant exists — labeled below). It is not “French” as in a national federation exam. Several US hunting articles treat French and walk-back as synonyms; they are not. Walk-back = a column of distances. French = near/far isolation.",
                    "Why the close shot isolates sight: at ~3 yd (or 6–12 ft) a small rest offset has almost no distance to grow into a miss, so left/right is mostly where the pin/peep system is pointed. Why the far shot isolates rest: once the pin is known-good at close range, leftover far miss is launch angle (rest, cam lean) growing with yards."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — compound close / far",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. Setup",
                        what = "Vertical line or a small bull. A long pin (commonly 40–50 yd) so that at 3 yd the arrow still lands on the face instead of over the bale. 2nd axis already level. Rest near brand start. Bare vertical already done if you have time.",
                        why = "A 20 yd pin at 3 yd often hits very high. The long pin brings POI down so you can actually see left/right. If 2nd axis is off, the long pin is a liar.",
                        how = listOf(
                            "Plumb line. Safety: 3 yd is close — know your target and your backstop.",
                            "Pick far distance: 30–50 yd typical; some field shooters use 15–20 if that is all they have (weaker signal).",
                            "Write rest clicks and Limb Shift / yoke marks. You will move at most one of those families per far-distance group."
                        ),
                        lookFor = listOf("Long pin chosen", "Close distance that actually centers that pin on the mark"),
                        changeNext = "Close range first. Do not pre-move the rest."
                    ),
                    TuneWalkStep(
                        title = "2. Close range — sight only",
                        what = "From ~3 yd (or 6–12 ft if 3 yd is unsafe/awkward), aim the mark with the long pin. Adjust SIGHT WINDAGE until the group is on the line. Do not touch the rest.",
                        why = "This zeros the aiming reference. If you move the rest here, you are mixing the two isolations and French becomes a worse walk-back.",
                        how = listOf(
                            "Two or three arrows. Be picky — this is the reference.",
                            "If close groups are huge, you do not have a French signal (grip, peep, or you are too close to be stable). Fix that or move to 5–6 yd."
                        ),
                        lookFor = listOf("Close group on the line"),
                        changeNext = "Walk back. Sight stays frozen."
                    ),
                    TuneWalkStep(
                        title = "3. Far range — rest (or cam-lean) only",
                        what = "Without touching the sight, shoot the same mark with the same pin at the far distance. Left/right versus the line is a rest/centershot (or Limb Shift) error.",
                        why = "The pin is already known at close range. The new miss is distance × launch angle.",
                        how = listOf(
                            "Several arrows. Ignore vertical (you are using a long pin; they will hit low of a 20 yd expect — that is fine).",
                            "Far miss LEFT (RH): rest RIGHT in tiny clicks — same CR map as paper nock-left and bare-left. Or Limb Shift toward L if you are using cam lean instead of rest.",
                            "Far miss RIGHT: rest LEFT (in toward the riser). Or Limb Shift toward R. Powder-check if this appeared suddenly.",
                            "Some magazine write-ups say “move the rest opposite the miss,” which is the same sentence as above (hit left, rest right). If you find an article that chases the far impact with the rest the other way, do not import it — it will fight Tear."
                        ),
                        lookFor = listOf("A repeatable far miss, not one punched shot"),
                        changeNext = "One rest family click only. Then back to 3 yd."
                    ),
                    TuneWalkStep(
                        title = "4. Iterate and resight",
                        what = "A rest move will throw the close-range group off the line. That is expected. Return to 3 yd and reset SIGHT only. Then far again. Stop when both distances sit on the line.",
                        why = "You are converging two linear errors. If you resight all hunting pins before the rest has settled, you will do it twice.",
                        how = listOf(
                            "Close: sight. Far: rest. Repeat.",
                            "When both are on, sight your real distances/pins/tape as a separate job.",
                            "If it will not converge in three iterations, you do not have a French problem — you have grip, 2nd axis, or a spine leftover. Open Torque or Bare shaft."
                        ),
                        lookFor = listOf("Close and far on the same vertical line", "Walk-back, if you spot-check, no longer fans"),
                        changeNext = "Broadhead confirm if this is a hunting bow."
                    )
                )
            ),
            AdvancedSection(
                id = "variants",
                heading = "Labeled variants (not the compound default)",
                kind = AdvancedSectionKind.CALLOUT,
                paragraphs = listOf(
                    "Bare-vs-fletched two-distance (some target compounds): shoot bares and fletched at 20 and at 40. If the C-C gap grows, you still have a flight/spine/launch issue (see Planing). If both miss the same amount at both distances, that is sight. This is diagnostic, not a rest recipe by itself.",
                    "Point-weight “French” (Olympic recurve / some barebow): change point mass so bare and fletched converge at two distances. Finger paradox rules apply (RH bare left ≈ stiff). Do not run that chart on a release compound.",
                    "Crawl French (barebow string-walking): different crawls change nock height and face contact. Out of scope for Bow Tune’s compound+release core; listed so you do not confuse it with 3 yd/50 yd compound French.",
                    "“Modified French” in some hunting blogs: 3 yd sight-in then 10 yd rest move. Same idea, weaker far signal. Prefer 30–50 yd when form allows."
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "When to French vs walk-back vs bare",
                kind = AdvancedSectionKind.CROSSLINK,
                paragraphs = listOf(
                    "Bare first to get vertical and a sane horizontal. French when you want a fast centershot confirm and you have a safe 3 yd and a far lane. Walk-back when you want to see fan vs parallel across many distances or you cannot stand at 3 yd. If French and walk-back disagree, believe the multi-distance shape and re-check 2nd axis / cant."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.PLANING,
                    AdvancedTuneIds.YOKE,
                    AdvancedTuneIds.TORQUE
                )
            )
        )
    )
}

internal object PlaningAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.PLANING,
        title = "Bare-shaft planing at multiple distances",
        summary = "10/20/30/40 bare vs fletched. Growing error vs constant offset. Spine vs rest vs nock vs wind.",
        audience = "Advanced · Compound + release",
        relatedMethods = "Bare shaft, walk-back, French, dynamic spine, broadhead",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Why distance unmasks the launch",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "A constant angular rate (leftover paradox or a yawed launch) integrates to a miss that grows with yards. A pure sight/POA error does not change the bare-versus-fletched gap — both arrow types miss together. So: growing bare-fletched C-C = flight. Constant C-C = you measured the same aim error twice.",
                    "Fletching reduces the residual rate. That is why the fletched column can look walk-back-clean while the bare column still planes. If you only walk-back fletched, you can ship a hunting bow that comes apart with the vanes off or the blades on."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — multi-distance bares",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What the test is",
                        what = "At 10, 20, 30, and 40 yd if you have them, shoot a small fletched group and two matched bares at the same aim point. Plot C-C (H and V) versus distance.",
                        why = "One-distance bare cannot tell spine from rest from a 20 yd gust. The slope of the plot can.",
                        how = listOf(
                            "Dead air. Same pin or a known tape — you are measuring C-C, not score.",
                            "Number the bares. If one always planes more, that is a shaft/nock, not the bow."
                        ),
                        lookFor = listOf("A table of inches, not vibes"),
                        changeNext = "Do not move hardware during the first plot."
                    ),
                    TuneWalkStep(
                        title = "2. Reading the plot",
                        what = "Vertical C-C that grows → leftover pitch (nock / timing). Horizontal C-C that grows → leftover yaw (rest, cam lean, dynamic spine). Flat C-C → measurement or sight, not flight.",
                        why = "This is walk-back’s cousin with the fletching stripped off.",
                        how = listOf(
                            "RH CR H-gap growing left → weak family or rest-left; same map as Bare shaft / Tear.",
                            "H-gap growing right → rest-right / clearance family first.",
                            "V-gap growing high → nock up (Easton bare-high), not rest height, not sight.",
                            "Wild at 10, prettier at 30 → you are still in paradox at 10 or you have contact. Do not over-read 10 yd bares."
                        ),
                        lookFor = listOf("Slope, not a single 40 yd hero"),
                        changeNext = "One variable from the family the slope names."
                    ),
                    TuneWalkStep(
                        title = "3. Correct, then re-plot",
                        what = "Pick nock or rest or point-weight — one. Re-run 20 and 30 at minimum.",
                        why = "If the slope dies, you were right. If the intercept dies but the slope stays, you moved sight or you cheated with a parallel rest shove that did not fix the angle.",
                        how = listOf(
                            "Re-plot. Keep the old photo so you cannot gaslight yourself.",
                            "When H and V slopes are inside group size, stop and go to walk-back/French on fletched for sight vs rest polish."
                        ),
                        lookFor = listOf("Flatter C-C vs yards"),
                        changeNext = "Broadhead if this is a hunting setup — blades amplify leftover slope."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.FRENCH,
                    AdvancedTuneIds.DYNAMIC_SPINE,
                    AdvancedTuneIds.BROADHEAD
                )
            )
        )
    )
}

internal object BroadheadAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.BROADHEAD,
        title = "Broadhead tuning",
        summary = "Fixed blades are fletching that can fight you. Same point weight, 20–40 yd, one variable. Mech vs fixed.",
        audience = "Advanced · Hunting compound + release",
        relatedMethods = "Bare shaft, fletched, walk-back, planing, paper, Logs broadhead hint",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Why blades move POI",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "A fixed blade is a set of wings at the front. Any yaw or pitch creates a moment at the point, not only at the vanes. If the launch is still dirty, the head steers. That is the opposite of a field point, which is mostly a mass.",
                    "Mechanical heads behave closer to field points in flight (blades folded) and then become a different animal on impact. Tune them for POI if you want, but do not use a mech group to bless a fixed-blade setup.",
                    "Weight: a 150 gr head on a shaft tuned with 100 gr field points is a different dynamic spine and a different FOC. Same grains, or you are testing two arrows."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — BH vs field",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What you compare",
                        what = "Same aim, same distance (start 20, then 30–40), field-point group vs broadhead group. Measure C-C. Logs: +left, +high.",
                        why = "A single BH flyer is a blade or a nock, not a rest. You need a group.",
                        how = listOf(
                            "Same total front mass. Spin the BH; wobble = ferrule/insert, not tune.",
                            "Dead air. Fixed blades first if you hunt fixed.",
                            "Do this after bare + walk-back, not instead of them. A BH session is a proof, not a first tune."
                        ),
                        lookFor = listOf("Heads that spin true", "Matching grains"),
                        changeNext = "Shoot before you loosen a rest screw."
                    ),
                    TuneWalkStep(
                        title = "2. What the miss means",
                        what = "BH left/right is the same horizontal family as bare/paper (RH left → rest right or Limb Shift L; right → rest left / clearance). BH high/low is nock / FOC / tip weight — tiny, and only after you confirm the field point was the same grains.",
                        why = "Logs already encodes those directions. Do not invert them for “blades steer opposite.” The blade amplifies the launch you already had.",
                        how = listOf(
                            "H > ~1 in at 20–30: rest / Limb Shift in the CR direction, one click, or re-check paper if you never had a bullet.",
                            "V > ~1 in: tiny nock move or confirm you did not swap a 100 for a 125. Blade planing that is only vertical is often a bent ferrule — spin it.",
                            "Mech matches field, fixed does not: you still have a launch angle. Go back to bare/planing, do not grind the ferrule."
                        ),
                        lookFor = listOf("Repeatable C-C, not one cut-on-contact flyer"),
                        changeNext = "One change. Re-shoot both field and BH. Field will move too — that is honest."
                    ),
                    TuneWalkStep(
                        title = "3. Stop criteria",
                        what = "They share a group at the distance you hunt. Then leave it alone.",
                        why = "Chasing 1/2 in at 40 with a three-blade is how people unsight the bow the week of the opener.",
                        how = listOf(
                            "Log the comparison in Sessions → Broadhead so the hint is attached to the setup.",
                            "If you change vanes or helical after this, re-do BH. You changed steering."
                        ),
                        lookFor = listOf("Shared group within hunting-acceptable, not indoor-X"),
                        changeNext = "Set a next cam/cable due date in Gear. Stretch reopens this file."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.PLANING,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.FLETCHED,
                    AdvancedTuneIds.PAPER
                )
            )
        )
    )
}
