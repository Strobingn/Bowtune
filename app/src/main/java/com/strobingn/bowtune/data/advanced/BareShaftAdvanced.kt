package com.strobingn.bowtune.data.advanced

internal object BareShaftAdvanced {
    val guide = AdvancedGuide(
        id = AdvancedTuneIds.BARE_SHAFT,
        title = "Bare shaft tuning",
        summary = "Follow-along: nock point, rest height, spine, centershot. Paradox, dynamic spine, RH/LH, clearance false reads, paper relationship, and why you start here for field relevance.",
        audience = "Advanced · Compound + release (CR). Finger/recurve inversions are labeled.",
        relatedMethods = "Paper, planing, nock tune, walk-back, French, fletched, dynamic spine, LIFT 29.5",
        sections = listOf(
            AdvancedSection(
                id = "disclaimer",
                heading = "How to use this (and what it is not)",
                kind = AdvancedSectionKind.CALLOUT,
                paragraphs = listOf(
                    AdvancedTuneCatalog.DISCLAIMER,
                    AdvancedTuneCatalog.ONE_VARIABLE,
                    "This is not the Logs bare-vs-fletched hint. Logs records inches. This section is the procedure those inches come from."
                )
            ),
            AdvancedSection(
                id = "physics",
                heading = "Physics: paradox, recovery, and planing",
                kind = AdvancedSectionKind.PHYSICS,
                paragraphs = listOf(
                    "Archer’s paradox is not a slogan. The nock is constrained to the string path; the point is constrained to the rest. At release the shaft must flex around that offset, then recover. The first few feet — the paradox window — are when the shaft is still bent. Paper at 4–6 ft (Easton CR start) reads that window. Bare shaft at 20 yd reads what is left after recovery: residual angular rate plus any launch-angle error. That leftover is planing (horizontal) or porpoising (vertical).",
                    "Fletching applies a restoring moment and damps oscillation. A bare shaft has almost none of that, so it reports the launch condition instead of the fletched compromise. That is why two setups can share a 20 yd fletched group and still be different bows at 40 yd or with a fixed blade.",
                    "Dynamic spine is not the box number. Static spine is deflection under a standard load. Dynamic spine is how much this shaft actually bends for this draw weight, cam ramp, point/insert mass, length, nock fit, and your release. A 300 spine that is long, heavy-pointed, and shot from an aggressive cam can behave weaker than a 340 that is short and light-pointed from a smooth cam. Home → Spine helper is a starting chart, not a tune."
                ),
                bullets = listOf(
                    "Fishtail = leftover horizontal oscillation. Porpoise = leftover vertical. Plane = the shaft’s mean path drifting off the fletched line as distance grows.",
                    "Minnow = continuous swim, usually clearance or nock fit, not a 1/64 in rest click.",
                    "Matched bares means same spine, cut, insert, point, nock model, and weight within ~2 gr. One odd bare is a shaft problem, not a bow problem."
                )
            ),
            AdvancedSection(
                id = "start_bare",
                heading = "Workflow: shoot bare shaft to start (field relevance)",
                kind = AdvancedSectionKind.PROCEDURE,
                paragraphs = listOf(
                    "You will end up bare-shafting or field-confirming anyway. Paper is a shop microscope. The animal, the 3D target, and the 50 yd pin live in recovered flight. If time is short, prioritize bare at ~20 yd over a long paper chase. Paper is optional insurance and a clearance detector — not the finish line."
                ),
                bullets = listOf(
                    "1. Setup: draw length and cam sync/timing, D-loop installed, nock ~1/8–1/4 in above square, rest near manufacturer / berger start, grip rehearsed. Do not start with a random rest.",
                    "2. Optional short paper at 4–6 ft — one or two holes to catch a huge tear or obvious contact. If it is ugly and changing every shot, stop and fix clearance / grip before you interpret bares.",
                    "3. Bare shaft at ~20 yd (calm air). Vertical first (nock), then leftover horizontal (spine vs rest). Two matched bares, two passes.",
                    "4. Walk-back or French for centershot vs sight windage. Limb Shift / yoke / DeadLock / S.E.T. only after rest is near a sane start.",
                    "5. Broadheads vs field points at 20–40 yd, same point weight when you can.",
                    "Why this order: paper can be clean while the shaft still planes at distance. Bare + walk-back is what hunting and target scoring actually feel. LIFT 29.5 Vertical is the timed version of “bare high first.”"
                )
            ),
            AdvancedSection(
                id = "vs_paper",
                heading = "Relationship to paper tuning",
                kind = AdvancedSectionKind.READ,
                paragraphs = listOf(
                    "Paper is close-range launch diagnosis. It tells you how the nock is tracking relative to the point while the shaft is still in paradox. Bare shaft confirms flight at distance after paradox has mostly damped. They should usually agree on the axis (high/low, left/right). When they disagree, believe the shooter and clearance first — do not invent a third rest position.",
                    AdvancedTuneCatalog.CR_REST_MAP,
                    "Easton-aligned sequencing (same as Tear + LIFT): fix vertical first, then horizontal. Combo paper tears are two problems. Bare high/low is nock point (do not move the rest while you are on vertical). Leftover bare left/right is rest micro, Limb Shift, or spine — after grip and clearance."
                ),
                bullets = listOf(
                    "Paper first when: new string/cables, unknown contact, indoor only, or the bow has never been through paper. One clean-ish hole, then go outside.",
                    "Bare first (or bare only) when: you already have a known setup, you are outdoors, or you keep chasing paper holes that do not show up at 20 yd. Field relevance wins.",
                    "Paper agrees with bare vertical → leftover paper L/R is often clearance, grip, or a small rest/Limb Shift click. See Tear tab.",
                    "Paper disagrees with bare → do not chase paper alone (LIFT rule). Powder the rest and vanes; re-check face pressure and bow-hand torque; then re-paper.",
                    "RH CR consistency with this app: paper nock-left and bare-left both map to rest RIGHT (or dynamically weak). Paper nock-right and bare-right map to rest LEFT / clearance — not a stiff-spine first guess. Finger/recurve charts invert the horizontal spine story; do not import those onto a release gun."
                ),
                relatedIds = listOf(AdvancedTuneIds.PAPER, AdvancedTuneIds.PLANING, AdvancedTuneIds.TILLER)
            ),
            AdvancedSection(
                id = "rh_lh",
                heading = "RH / LH interpretation (CR vs finger)",
                kind = AdvancedSectionKind.READ,
                paragraphs = listOf(
                    "Read impact from behind the bow, archer’s left/right. Logs use +left from the archer. Invert the horizontal words for a left-handed bow (mirror the riser).",
                    "Compound + mechanical release (this app’s default): the string leaves cleanly. Residual flex is still real, but the classic finger “pluck” is gone. This library and Paper Tear treat RH bare-left / nock-left as the weak / rest-too-far-left family (rest right; if it persists after a sane rest and a clean grip, stiffen the dynamic spine). RH bare-right / nock-right is rest-too-far-right or contact until proven otherwise — Tear already refuses a stiff-spine first guess on a right tear.",
                    "Finger / recurve (labeled, do not mix into CR): Easton’s finger bare-shaft chart is the inversion many people memorized — RH bare left of fletched ≈ stiff; bare right ≈ weak. If you shoot fingers, use that chart and ignore the CR rest map in Tear. If you shoot a release, do not “correct” Tear to the finger chart. That is how people reverse the shipped Easton CR left/right rest directions."
                )
            ),
            AdvancedSection(
                id = "false_reads",
                heading = "Clearance vs spine: false reads",
                kind = AdvancedSectionKind.CALLOUT,
                paragraphs = listOf(
                    "A weak-looking bare left that flips shot-to-shot is not spine. Spine is a bias. Contact, a loose nock, and grip torque are noise. Prove repeatability before you buy shafts."
                ),
                bullets = listOf(
                    "Lipstick, chalk, or foot powder on vanes, rest launcher, cables, and shelf. Any wipe = clearance. Rotate nock 1/32 turn or change vane index before you touch spine.",
                    "Nock fit: the shaft should clip on and off the serving with a light click, not a pry and not a rattle. Pinch at full draw looks like random L/R.",
                    "One bare flies, the other does not → weigh both, spin both, swap nocks, then nock-tune. Do not average two different arrows into a rest click.",
                    "Wind > about 5 mph at 20 yd will plane a bare more than your 1/64 in rest. Indoor or dead air for the diagnosis groups.",
                    "Death grip and heel pressure invent left/right that follow you to paper. See Torque / grip."
                ),
                relatedIds = listOf(AdvancedTuneIds.NOCK_TUNE, AdvancedTuneIds.TORQUE, AdvancedTuneIds.FLETCHED)
            ),
            AdvancedSection(
                id = "nock_point",
                heading = "Walkthrough — nock point (vertical)",
                kind = AdvancedSectionKind.WALKTHROUGH,
                paragraphs = listOf(
                    "Primary vertical tool for bare shaft. Easton / LIFT: bare high → raise the nocking point. Bare low → lower it. Do not move the rest while this walkthrough is open."
                ),
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What the nocking point is",
                        what = "The nocking point (tied nock-set or the D-loop’s lower/upper bound) is where the nock sits on the string. It sets the rear launch height relative to the rest. You already installed the D-loop; now you are micro-moving that rear reference, not rebuilding the loop from scratch unless serving is slipping.",
                        why = "If the rear starts high or low, the shaft leaves with a pitch rate. Fletching hides a lot of that by 20 yd. A bare shaft still has the pitch, so it impacts high or low of the fletched group-center. That is porpoising, not “the pin is off.”",
                        how = listOf(
                            "Bow in a vise or at least plumb. Arrow on the rest, nocked.",
                            "Bow square: start about 1/8–1/4 in nock-high (nock above level through the berger). Write that number down.",
                            "If the D-loop can slide, cinch or tie nock-sets so the next 1/32 in move is intentional."
                        ),
                        lookFor = listOf("Serving not mashed", "D-loop not walking", "Rest height left alone for this entire walkthrough"),
                        changeNext = "Do not touch rest, sight, or Limb Shift yet. Go shoot."
                    ),
                    TuneWalkStep(
                        title = "2. Why it matters for bare groups",
                        what = "You are comparing group-center to group-center: fletched vs bare, vertical only. Horizontal is noise to ignore until vertical is inside today’s dispersion.",
                        why = "A 1 in bare-high at 20 yd is a launch-pitch problem. Chasing it with the rest changes the front geometry and your centershot story at the same time. That is two variables. LIFT’s rule exists because people stack those and then cannot get back.",
                        how = listOf(
                            "Pick one aim point. Same pin. Same distance (~20 yd).",
                            "Decide your pass pattern: F-B-F-B-F or a 3-fletched group then 2–3 matched bares. Repeat the whole pass."
                        ),
                        lookFor = listOf("You have a fletched center you trust, not one hero arrow"),
                        changeNext = "Measure before you move metal."
                    ),
                    TuneWalkStep(
                        title = "3. Equipment, increments, shooting",
                        what = "Two or more matched bare shafts, same point weight as the fletched controls. Tape measure or a marked target. A way to move nock 1/32–1/16 in (serving jig, tied nock-set, or a D-loop that you mark before twisting).",
                        why = "Bigger than 1/16 in per move and you will leapfrog the sweet spot. One bare is a coin flip.",
                        how = listOf(
                            "Calm air. Neutral grip (see Torque if groups walk when you change hands).",
                            "Shoot pass 1. Mark each hole or photograph the face.",
                            "Measure vertical C-C: bare center minus fletched center. Logs: positive = bare high.",
                            "Shoot pass 2 without touching the bow. If pass 1 and 2 disagree by more than your fletched group height, you do not have a tune signal yet — form/clearance first."
                        ),
                        lookFor = listOf(
                            "Both passes bare high by > ~3/4 in → nock UP next",
                            "Both passes bare low by > ~3/4 in → nock DOWN next",
                            "Inside ~3/4 in or flipping sign → leave nock; confirm with another pair of groups"
                        ),
                        changeNext = "Only if both passes agree on high or low."
                    ),
                    TuneWalkStep(
                        title = "4. The correction (one variable)",
                        what = "Move only the nocking point. Bare high: raise nock a tiny amount (Easton). Bare low: lower nock a tiny amount.",
                        why = "Raising the nock raises the rear. Empirically that is what kills a high bare on a release compound when the rest was already in a normal window. It is the opposite of the reflex “bares hit high so lower the nock” that comes from mixing paper-nock-high language with bare-POI language. Paper nock-high (tail high through paper) is a different photograph; use Tear for that hole. This walkthrough is POI at 20 yd.",
                        how = listOf(
                            "Mark the current nock/D-loop with a silver line so you can revert.",
                            "Move 1/32 in (prefer) to 1/16 in. One direction only.",
                            "Re-shoot two full passes.",
                            "If vertical error grows or flips past the fletched center by more than it started, revert immediately. Then check cam timing / tiller and that the bares are actually matched."
                        ),
                        lookFor = listOf("Bare and fletched share vertical within today’s group height", "Horizontal may still be ugly — ignore it"),
                        changeNext = "Stop nock work. Horizontal is a different walkthrough. Do not “just bump the rest a little” on the way out."
                    )
                )
            ),
            AdvancedSection(
                id = "rest_height",
                heading = "Walkthrough — rest height (vertical geometry)",
                kind = AdvancedSectionKind.WALKTHROUGH,
                paragraphs = listOf(
                    "Rest height is the front half of the nock-to-rest pair. It is not the first vertical tool on a bare-shaft day. Use it when nock is already in a good range, clearance demands it, or paper nock-high/low is being fixed in the shop and the nock-set is locked."
                ),
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What rest height is",
                        what = "The launcher or containment fork’s vertical position sets where the point sits relative to the berger hole and the nock. Raising the rest raises the point (front). That is geometrically similar to lowering the nock for a paper nock-high tear, and the opposite move from “nock up” on a high bare.",
                        why = "If you move rest height and nock in the same group, you have rotated the shaft about an unknown point. You cannot read the next bare. Tear already offers “lower nock or raise rest” as alternatives for nock-high paper — alternatives, not a stack.",
                        how = listOf(
                            "Record rest height: marks on the stalk, a caliper off the shelf, or a photo with a ruler.",
                            "Confirm nock is already ~1/8–1/4 in above square and that you are not in the middle of a nock-point walkthrough."
                        ),
                        lookFor = listOf("Written starting height", "Clearance to shelf with your vane index"),
                        changeNext = "If nock is not yet settled, go back to the nock-point walkthrough."
                    ),
                    TuneWalkStep(
                        title = "2. Why it shows up in groups",
                        what = "A rest that is too low can slap the shelf or launch the point low (paper nock-high family). A rest that is too high can starve vane clearance or launch the point high (paper nock-low family). At 20 yd a bare will still show leftover pitch if the front/rear pair is wrong.",
                        why = "Hunters raise rests “for containment” and then chase paper forever. Target shooters drop rests for clearance and invent nock-high tears. Height is a clearance and launch-angle tool, not a sight.",
                        how = listOf(
                            "Powder the launcher and the shaft belly. A long belly rub means the shaft is wrapping the rest — height, containment tension, or spine, not a sight tape."
                        ),
                        lookFor = listOf("Contact streaks", "Launcher bounce / blade lean on drop-aways"),
                        changeNext = "Clean contact before you call the next height move a tune."
                    ),
                    TuneWalkStep(
                        title = "3. How to move it",
                        what = "Micro vertical rest only. Blade/containment tension is a different variable — do not change both.",
                        why = "Same as nock: 1/32–1/16 in, two confirmation passes.",
                        how = listOf(
                            "Paper nock-high (Tear): raise rest a hair if you have chosen rest instead of lowering nock. Re-paper at 4–6 ft.",
                            "Paper nock-low: lower rest only if the vane still clears the shelf.",
                            "Bare still high after a finished nock walkthrough and timing is clean: prefer a tiny nock revisit first. Only then a tiny rest-height change, and only one.",
                            "Re-shoot 20 yd F/B. Measure vertical C-C again."
                        ),
                        lookFor = listOf("Vertical C-C shrinks", "No new powder wipe", "Drop-away still timed — a height change can alter when the launcher leaves the shaft"),
                        changeNext = "If drop-away timing changed, fix cord timing as its own session, then re-bare. Do not blend that into centershot."
                    )
                )
            ),
            AdvancedSection(
                id = "spine",
                heading = "Walkthrough — spine (weak / stiff, RH CR)",
                kind = AdvancedSectionKind.WALKTHROUGH,
                paragraphs = listOf(
                    "Spine is a horizontal bias after rest is near manufacturer start and grip/clearance are clean. Cheapest lever is point weight, not a new dozen."
                ),
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What “spine” means here",
                        what = "You are asking whether this finished arrow is dynamically weak or stiff for this bow and this release — not whether the box said 300. Weak: the shaft over-flexes in the paradox window. Stiff: it under-flexes.",
                        why = "A rest click can fake a little spine. A lot of leftover L/R that survives a sane rest, a clean powder check, and two calm passes is usually dynamic spine (or nock clock — do that before you buy shafts).",
                        how = listOf(
                            "Write the current rest position and Limb Shift / yoke / S.E.T. / DeadLock marks. Those stay frozen for this walkthrough.",
                            "Confirm nock height is already done. Vertical first."
                        ),
                        lookFor = listOf("Rest within a few clicks of berger / brand start, not 1/4 in outboard “to fix weak”"),
                        changeNext = "If rest is already a huge compensation, put it back to start and re-bare. You may be looking at a rest story, not a spine story."
                    ),
                    TuneWalkStep(
                        title = "2. Why weak/stiff moves the bare",
                        what = "RH compound + release, this app (aligned with Tear): over-flex (weak) tends to leave a nock-left paper tear and a bare that impacts LEFT of fletched. Under-flex plus a clean release rarely shows as a classic “stiff right” — Tear treats nock-right as rest-too-far-right or vane/cable contact first.",
                        why = "If you apply the finger chart (bare left = stiff) on a release bow, you will move the rest the wrong way and fight the Easton CR map already in Tear. LH: mirror left/right words.",
                        how = listOf(
                            "Shoot two passes of matched bares vs fletched at 20 yd, dead air.",
                            "Measure horizontal C-C. Logs: +left from the archer.",
                            "If the sign flips between passes, stop — that is not spine."
                        ),
                        lookFor = listOf(
                            "RH CR both passes bare left > ~3/4 in, rest already sane, powder clean → dynamically WEAK",
                            "RH CR both passes bare right > ~3/4 in → rest left (in) / Limb Shift R / clearance first. Only after those, consider “stiff”",
                            "Finger/recurve (labeled): RH bare left of fletched ≈ stiff; bare right ≈ weak. Different sport."
                        ),
                        changeNext = "Pick one lever. Point weight first."
                    ),
                    TuneWalkStep(
                        title = "3. How to change dynamic spine (one lever)",
                        what = "Weaker dynamic: heavier point/insert, longer shaft, more draw weight, more aggressive launch. Stiffer dynamic: lighter point, shorter cut, less DW, or a stiffer static spine.",
                        why = "25–50 gr point change is reversible and cheaper than a dozen 250s. Cutting 1/2 in is not reversible. Dropping DW changes sight tapes and hunting KE — know that going in.",
                        how = listOf(
                            "WEAK (RH CR bare left persists): add 25–50 gr up front (point or insert) and re-bare, or drop DW 1–2 lb if the bow allows, or step to a stiffer static spine if the chart and the bare both scream it.",
                            "After a true stiff diagnosis (clearance and rest-left already done): add point weight or length, or a weaker static spine.",
                            "Re-shoot two 20 yd passes after that one change. Do not also click the rest “just to help.”",
                            "If the bare walks toward the fletched group, you picked the right direction. If it walks away, revert the grain / DW / shaft."
                        ),
                        lookFor = listOf("Bare L/R shrinks toward fletched center", "Paper, if you re-check, should not grow a new axis"),
                        changeNext = "Leftover < ~3/4 in at 20: stop spine. Finish with walk-back / French, not another 50 gr."
                    )
                )
            ),
            AdvancedSection(
                id = "centershot",
                heading = "Walkthrough — centershot (horizontal rest)",
                kind = AdvancedSectionKind.WALKTHROUGH,
                paragraphs = listOf(
                    "Centershot is the rest’s left/right versus the string. It is a walk-back / French job with a 20 yd bare hint. It is not a substitute for Limb Shift on a Mathews when the rest is already where the book wants it."
                ),
                steps = listOf(
                    TuneWalkStep(
                        title = "1. What centershot is",
                        what = "The lateral position of the launcher so the shaft’s axis sits where this bow wants it — often through or slightly outside the berger, brand-specific. You are not trying to “split the riser” by eye and call it done.",
                        why = "A rest that is 1/8 in off launches a horizontal angle. Fletching steers some of it out by 20 yd. Bare shafts and fixed blades will not. Walk-back fans and French near/far splits are this angle showing up with distance.",
                        how = listOf(
                            "Start from manufacturer centershot, not from last year’s compensation.",
                            "Record the number of clicks or a caliper off the riser wall."
                        ),
                        lookFor = listOf("Brand start written down", "Limb Shift / yoke / DeadLock / S.E.T. also written — you will not move those in the same group as the rest"),
                        changeNext = "If you are still on vertical, stop. Centershot is horizontal after nock is done."
                    ),
                    TuneWalkStep(
                        title = "2. Why 20 yd bare is only a hint",
                        what = "A 20 yd bare-left can be weak spine, rest-left, torque, or wind. Centershot is confirmed when error grows with distance (fan) or when French far misses the close-range line.",
                        why = "If you slam the rest 1/8 in because one bare sat left at 20, you will invent a paper left tear and a walk-back that now fans the other way.",
                        how = listOf(
                            "Use 20 yd bare L/R only to decide the first tiny rest direction, then prove it at a second distance."
                        ),
                        lookFor = listOf("RH CR bare left, grip clean → candidate rest RIGHT (same direction as paper nock-left)", "RH CR bare right → candidate rest LEFT (in toward the riser)"),
                        changeNext = "One or two clicks only, then re-bare or go to walk-back/French. Not both a rest click and a Limb Shift click."
                    ),
                    TuneWalkStep(
                        title = "3. Exact rest moves",
                        what = "1/64 in or one click. Same CR map as Tear. Limb Shift chases the tear (toward L for left); the rest moves the other way for a left problem (rest right).",
                        why = "Stacking rest and Limb Shift in one shot is how people lose the plot. Brand Guides exist for the cam-lean tools. This walkthrough is rest only.",
                        how = listOf(
                            "RH left problem (bare left / paper nock-left): rest RIGHT. Or, if you are using Limb Shift instead, Limb Shift toward L — not both.",
                            "RH right problem (bare right / paper nock-right): rest LEFT (in). Or Limb Shift toward R. Powder vanes first.",
                            "Re-shoot two bares at 20. Then either a 30 yd bare or a walk-back/French pass.",
                            "If the fan dies and the close group stays, you are done with rest. Resight windage last, not first."
                        ),
                        lookFor = listOf("Fan shrinks", "Whole line still offset the same amount at every distance → that leftover is SIGHT, not rest"),
                        changeNext = "Open Walk-back or French to finish. Do not keep clicking rest at one distance."
                    )
                )
            ),
            AdvancedSection(
                id = "cross",
                heading = "Cross-links",
                kind = AdvancedSectionKind.CROSSLINK,
                paragraphs = listOf(
                    "Tear tab = live paper diagnoser (keep its CR rest directions). Logs + bare hint = inch logger, not this procedure. LIFT 29.5 = timed vertical-first session. Brand Guides = Limb Shift / EZ.220 / XTS / DeadLock / S.E.T."
                ),
                relatedIds = listOf(
                    AdvancedTuneIds.PAPER,
                    AdvancedTuneIds.FLETCHED,
                    AdvancedTuneIds.NOCK_TUNE,
                    AdvancedTuneIds.PLANING,
                    AdvancedTuneIds.WALKBACK,
                    AdvancedTuneIds.FRENCH,
                    AdvancedTuneIds.DYNAMIC_SPINE,
                    AdvancedTuneIds.TORQUE
                )
            )
        )
    )
}
