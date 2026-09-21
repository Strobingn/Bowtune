package com.strobingn.bowtune.data.advanced

internal object PaperAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.PAPER,
        title = "Paper tuning — advanced CR",
        summary = "Paradox window, 4–6 ft, combo tears, minnowing, false tears. Live diagnoser stays on the Tear tab — this is depth and edge cases. CR left/right rest map is not reversed.",
        audience = "Advanced · Compound + release. Finger paper charts labeled.",
        relatedMethods = "Tear tab, bare shaft, fletched clearance, torque, tiller",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "What paper actually photographs",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Paper at Easton’s 4–6 ft CR start is a snapshot of nock path versus point path while the shaft is still bent. It is not a 20 yd group and it is not a sight-in. Ten-plus feet lets the shaft begin to recover and exaggerates tears; people then chase a hole that will not exist at 20 yd.",
                    "Read the tear from the nock end: nock-left means the nock went left of the point. That is a launch/flex story, not “the arrow hit left on the target.” Mixing tear language with POI language is how rest directions get reversed.",
                    AdvancedTuneCatalog.CR_REST_MAP,
                    "Finger / recurve paper (labeled): the horizontal spine story often inverts versus CR. If you shoot fingers, use Easton’s finger chart and ignore Tear’s CR rest sentences. If you shoot a release, do not “correct” Tear to the finger chart."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — advanced paper protocol",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. Distance, paper, and a second hole",
                        what = "Stand 4–6 ft from a taut sheet. After a candidate bullet, step back a few feet and confirm. One hole is weather.",
                        why = "A floppy sheet adds its own tear. A 10 ft “to make it obvious” session is a different test.",
                        how = listOf(
                            "Taut paper, known 4–6 ft (toes or a mark on the floor).",
                            "Open palm, relaxed bow hand. If the next hole changes a lot, you do not have a hardware signal.",
                            "Use the Tear tab to pick the tear and read the shipped steps — this library does not replace that map."
                        ),
                        lookFor = listOf("Repeatable tear shape", "No shred from a loose sheet"),
                        changeNext = "Grip check before the first rest click. Always."
                    ),
                    TuneWalkStep(
                        title = "2. Vertical first, then horizontal",
                        what = "Nock-high/low or the vertical component of a combo is nock height or rest height — one of those, not Limb Shift. Then L/R.",
                        why = "A diagonal tear is two numbers. If you click rest and nock together you will rotate around a random axis.",
                        how = listOf(
                            "Combo: apply Tear’s vertical steps, re-paper, then horizontal steps.",
                            "Nock-high (paper): lower nock 1/32–1/16 in or raise rest — not both. See Bare shaft for why 20 yd bare-high uses nock UP (different photograph).",
                            "Nock-low: raise nock or lower rest if clearance survives.",
                            "Flipping vertical shot-to-shot: timing/tiller or grip, not another 1/32 (Tear Pro overlay)."
                        ),
                        lookFor = listOf("Two holes that agree on vertical before you touch L/R"),
                        changeNext = "Horizontal only after vertical is stable."
                    ),
                    TuneWalkStep(
                        title = "3. Horizontal — keep the shipped CR map",
                        what = "RH nock-left: rest RIGHT, or Limb Shift toward L. If it persists: stiffer dynamic spine. RH nock-right: rest LEFT (in), or Limb Shift toward R; prove clearance. Uncommon on a clean release — do not start with “stiff arrow.”",
                        why = "This is the map already on main in PaperTearGuidance and brand Guides. Reversing it is a defect, not a preference.",
                        how = listOf(
                            "One click or 1/64 in. Re-paper.",
                            "If you use Limb Shift / EZ / yoke / DeadLock / S.E.T., that is the horizontal tool for this group — rest stays.",
                            "Open Brand Guides for the brand’s click direction. They already match this CR map."
                        ),
                        lookFor = listOf("Tear shrinks on the same axis you treated"),
                        changeNext = "Bullet-ish, then 20 yd bare. Do not call paper the finish."
                    )
                )
            ),
            AdvancedSection(
                id = "minnow",
                heading = "Clearance, minnowing, and false tears",
                kind = AdvancedSectionKind.READ,
                paragraphs = listOf(
                    "Minnowing is a swim: ragged or changing tears, powder wipes, a “fish” hole. Easton’s clearance move is 1/32-turn nock rotation until the vane misses. Lipstick on the rest is faster than guessing spine.",
                    "Grip torque invents L/R (and sometimes vertical). A second shooter or a deliberately worse grip that suddenly “fixes” the hole is a diagnosis — see Torque.",
                    "Nock pinch, a walking D-loop, and a drop-away that slaps the shaft after the paradox window all print paper that will not match 20 yd bares. When paper and bare disagree, LIFT’s rule: do not chase paper alone."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.FLETCHED,
                    AdvancedTuneIds.TORQUE,
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.TILLER
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                paragraphs = listOf(
                    "Tear tab = tap-zone diagnoser + TTS + history. This page is the why and the failure modes. After a bullet: bare at ~20, then walk-back or French."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.PLANING,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.YOKE
                )
            )
        )
    )
}

internal object TorqueAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.TORQUE,
        title = "Torque and grip diagnostics",
        summary = "How bow-hand torque fakes paper and bare L/R. LIFT-style A/B/C test. When grip is the tune.",
        audience = "Advanced · Compound + release",
        relatedMethods = "Paper, bare shaft, LIFT torque block, walk-back, French",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Physics: you are steering the riser",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Heel pressure, a wrapped thumb, or a death grip rotates the riser about the grip. The rest and the sight both move. Paper sees a new nock path. Bares see a new launch angle. The “tune” follows your hand to the next bow.",
                    "Side pressure (into or away from the riser) is a horizontal bias. Heeling is often vertical plus a bit of cam lean change at the pocket. None of this is Limb Shift."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — A / B / C torque check",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What the test is",
                        what = "At 20 yd, three 3-arrow groups: A = your normal grip, B = tiny clockwise torque, C = tiny counter-clockwise. Same aim. Measure horizontal POI of B and C versus A. This is the LIFT 29.5 torque block, usable on any compound.",
                        why = "If a few degrees of hand changes POI more than your last rest click, the rest click was theatre.",
                        how = listOf(
                            "Write what “normal” is (open palm, pressure point, thumb).",
                            "B and C are small on purpose. You are not trying to miss the bale.",
                            "Optional: repeat A through paper. If paper L/R tracks the torque, Tear’s grip warning just became data."
                        ),
                        lookFor = listOf("Inches of H shift, not “it felt worse”"),
                        changeNext = "Do not move the rest during A/B/C."
                    ),
                    TuneWalkStep(
                        title = "2. What the shifts mean",
                        what = "Large H shift → grip is priority. Rebuild a neutral pressure (bone through the grip, fingers relaxed, bow into the lifeline — whatever your coach already taught) until B and C shrink. Small/normal shift → note it for walk-back; do not “fix” with rest.",
                        why = "A rest click that cancels today’s death grip will not cancel tomorrow’s.",
                        how = listOf(
                            "If B/C are large: stop horizontal hardware. Film the grip (Vision tab is a form assist, not a grip lab).",
                            "Re-run A until two A groups agree, then one more B/C.",
                            "Only then return to bare or paper."
                        ),
                        lookFor = listOf("A repeatable, B/C small relative to group width"),
                        changeNext = "Bare shaft or walk-back, not a 1/8 in rest “compensation.”"
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.PAPER,
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.FRENCH
                )
            )
        )
    )
}

internal object YokeHorizontalAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.YOKE,
        title = "Yoke, cable, Limb Shift (horizontal)",
        summary = "Cam lean and cable load as the horizontal tool that lets the rest stay near the book. When to use which. Brand Guides have the click recipes.",
        audience = "Advanced · Compound + release (Mathews / PSE / Hoyt / Bowtech / Elite)",
        relatedMethods = "Brand Guides + wizards, paper, walk-back, French, bare shaft",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Why cam lean is a tune axis",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Twisting a yoke, sliding a DeadLock cam, clicking Limb Shift or Elite S.E.T., or biasing a cable-guard/EZ.220 changes how the limb/cam system loads left vs right. The nock’s path through paradox moves without dragging the rest into a clearance nightmare.",
                    "Rest is still a centershot tool. If you have already walked the rest 3/16 in outboard, you are using the rest as a cam-lean substitute. Put the rest back near berger/brand start and use the system the bow was sold with.",
                    "Vertical tears are not a cam-lean job. Brand Guides already say this; it is repeated because people Limb-Shift a nock-high hole."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — pick one horizontal system",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What you are choosing",
                        what = "One family per group: rest clicks, or Limb Shift, or yoke twists, or DeadLock slide, or S.E.T., or EZ.220. Write the starting mark.",
                        why = "Two families in one shot is un-debugable. The brand short wizards on Logs exist so you do not freehand this.",
                        how = listOf(
                            "Mathews: Limb Shift after rest is close. Chase the tear (L for left).",
                            "Hoyt XTS: yoke after rest is close. Confirm direction for your year — Guides say so.",
                            "Bowtech: DeadLock micro-slide, re-torque to spec, mark a witness line.",
                            "Elite: S.E.T. clicks, re-lock.",
                            "PSE: EZ.220 / cable-guard style bias plus rest micro — not a stack of both."
                        ),
                        lookFor = listOf("A written starting number"),
                        changeNext = "Paper or 20 yd bare, not both interpretations in one change."
                    ),
                    TuneWalkStep(
                        title = "2. Direction (stay on the CR map)",
                        what = "Left problem (paper nock-left / RH bare-left / walk-back fan left): Limb Shift / cam-lean toward L (chase). Rest, if you are using rest instead, moves RIGHT. Right problem: cam-lean toward R; rest LEFT (in). Clearance first on rights.",
                        why = "Chase-the-tear on cam lean and opposite-rest on the launcher is already how Guides + Tear are written. Mixing the metaphors is how people reverse a year of notes.",
                        how = listOf(
                            "One index / half-index / one twist. Re-paper or re-bare.",
                            "If worse, revert that one mark. Then check grip and powder.",
                            "Finish with walk-back or French. Cam lean that kills paper can still fan at 40."
                        ),
                        lookFor = listOf("Tear or bare L/R shrinks", "Rest still near the book"),
                        changeNext = "Log the setting on the Gear setup card. String changes reset this."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                paragraphs = listOf(
                    "Open Guides for the brand page and the short wizard. This page is the physics and the “one family” rule, not a second copy of every click chart."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.PAPER,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.FRENCH,
                    AdvancedTuneIds.BARE_SHAFT
                )
            )
        )
    )
}

internal object TillerTimingAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.TILLER,
        title = "Tiller and cam timing",
        summary = "Nock travel, sync, and the vertical that will not die. Compound tiller notes. When to stop moving nock.",
        audience = "Advanced · Compound (dual cam / hybrid / binary). Recurve tiller labeled.",
        relatedMethods = "Bare shaft nock point, paper vertical, LIFT, brand timing notes",
        sections = listOf(
            AdvancedSection(
                id = "physics",
                heading = "Nock travel is a path, not a point",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "The nocking point does not move in a perfect straight line. Cams that are out of time (one stop hitting early, marks split, a stretched cable) put a vertical kick into the last inch of the power stroke. You can chase that kick with nock height forever and it will flip.",
                    "Tiller on a compound is often even, or a small positive, set at the factory. Changing limb-bolt tiller on a modern twin-cam to “fix paper” is a last resort and a new bow. On a single-cam / binary the story is cable length and module, not recurve-style tiller bars.",
                    "Recurve / ILF (labeled): tiller is a real vertical and hold tool (often 0–1/4 in positive). Do not copy ILF tiller recipes onto a LIFT 29.5."
                )
            ),
            AdvancedSection(
                id = "walk",
                heading = "Walkthrough — when vertical will not settle",
                kind = AdvancedSectionKind.WALKTHROUGH,
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What you check before another 1/32 nock",
                        what = "Cam sync / cable-stop contact / timing marks for this exact bow. Cable stretch notes in Gear. A D-loop that is not walking.",
                        why = "Tear’s Pro overlay already says flipping vertical is timing/tiller or grip. Believe it after two failed nock moves.",
                        how = listOf(
                            "At full draw (or in a press, per the manual): do both cams roll to the stops together? Photograph marks.",
                            "If a cable was recently replaced, expect to re-time after a few hundred shots. That is creep, not your nock.",
                            "Drop-away cord timing: a late rest slaps the shaft and prints nock-high/low that is not a nock-set."
                        ),
                        lookFor = listOf("Marks or stop contact written down", "Loop serving not melting"),
                        changeNext = "Time the bow (or a shop) as its own job. Then re-bare vertical from scratch."
                    ),
                    TuneWalkStep(
                        title = "2. After timing is honest",
                        what = "Return to the nock-point walkthrough. If vertical still flips, it is the shooter or the rest cord — not a third nock-set.",
                        why = "You cannot nock-tune a cam that is still walking in.",
                        how = listOf(
                            "Two bare passes at 20. If they agree, you are back in the tune. If they do not, Vision/form and grip before hardware.",
                            "Log last cam check on the setup. Set a due date."
                        ),
                        lookFor = listOf("Vertical C-C stable across two days, not two minutes"),
                        changeNext = "Horizontal methods only after this is boring."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                relatedIds = listOf(
                    AdvancedTuneIds.BARE_SHAFT,
                    AdvancedTuneIds.PAPER,
                    AdvancedTuneIds.TORQUE
                )
            )
        )
    )
}
