package com.strobingn.bowtune.data

/**
 * 90-minute Mathews LIFT 29.5 guided vertical-tune protocol.
 * Priority: bare-shaft-high; establish vertical first; one variable at a time.
 */
object LiftVerticalTunePlan {
    const val PROTOCOL_ID = "lift_29_5_vertical_90"
    const val TITLE = "90-minute LIFT 29.5 — vertical tune first"
    const val BOW = "Mathews LIFT 29.5"
    const val CARD_LABEL = "LIFT 29.5 — Vertical Tune (90 min)"
    const val PRIORITY_BANNER =
        "Priority: bare-shaft-high. Establish vertical first. Don't chase paper / walk-back / stabilizer together."
    const val RULE_CHIP =
        "Bare high → raise nocking point (Easton). Multiple bare shafts. One variable at a time. Don't move rest while adjusting nock for vertical."

    data class LogField(
        val key: String,
        val label: String,
        val hint: String = ""
    )

    data class Block(
        val id: String,
        val timeRange: String,
        val suggestedMinutes: Int,
        val title: String,
        val instructions: String,
        val logFields: List<LogField>
    )

    val blocks: List<Block> = listOf(
        Block(
            id = "baseline",
            timeRange = "0–10",
            suggestedMinutes = 10,
            title = "Baseline",
            instructions = """
                Shoot 9 fletched at 20 yd (3×3 groups). No tune adjustments.
                Log group width × height, POI vs aim point, bubble (L/C/R), pin float (H/C/L), and any bad shots.
                This is your form/dispersion reference for the rest of the session.
            """.trimIndent(),
            logFields = listOf(
                LogField("groupWxH", "Group W×H (in)", "e.g. 2.5×3.0"),
                LogField("poiVsAim", "POI vs aim", "high/low/left/right inches"),
                LogField("bubble", "Bubble L / C / R", "which side during shot"),
                LogField("pinFloat", "Pin H / C / L", "float tendency"),
                LogField("badShots", "Bad shots notes", "pulled, punched, etc.")
            )
        ),
        Block(
            id = "bare_vertical",
            timeRange = "10–30",
            suggestedMinutes = 20,
            title = "Bare vertical",
            instructions = """
                Use ≥2 matched bare shafts. Shoot F-B-F-B-F twice at 20 yd.
                Measure group-center to group-center (fletched vs bare).
                Pass 1 & 2: log bare V H/L", bare H L/R", bare-to-bare spread.
                Both passes bare high → tiny UP nocking-point adjust, retest. Do NOT move rest.
                If worse → revert. Stop when bare & fletched share the same vertical within today's dispersion.
            """.trimIndent(),
            logFields = listOf(
                LogField("pass1BareVH", "Pass1 bare V H/L\"", "positive = bare high"),
                LogField("pass1BareHL", "Pass1 bare H L/R\"", "positive = bare left (RH)"),
                LogField("pass1BareSpread", "Pass1 bare-to-bare spread\""),
                LogField("pass2BareVH", "Pass2 bare V H/L\""),
                LogField("pass2BareHL", "Pass2 bare H L/R\""),
                LogField("pass2BareSpread", "Pass2 bare-to-bare spread\""),
                LogField("nockAdjust", "Nock adjust made", "e.g. +1/32\" up / none / reverted"),
                LogField("bareNotes", "Notes")
            )
        ),
        Block(
            id = "paper_confirm",
            timeRange = "30–42",
            suggestedMinutes = 12,
            title = "Paper confirm only",
            instructions = """
                Shoot 3 fletched through paper at 4-6 ft (Easton start). Log each tear V+H.
                If paper AGREES with bare-shaft vertical → investigate clearance / grip / face before moving the bow.
                If paper DISAGREES with bare → do not chase paper alone; resolve shooter/clearance first.
                This block confirms; it does not start a new tune path.
            """.trimIndent(),
            logFields = listOf(
                LogField("tear1VH", "Tear 1 V+H", "e.g. nock-high 1/2\", left 1/4\""),
                LogField("tear2VH", "Tear 2 V+H"),
                LogField("tear3VH", "Tear 3 V+H"),
                LogField("agreesWithBare", "Agrees with bare? Y/N + note")
            )
        ),
        Block(
            id = "torque",
            timeRange = "42–55",
            suggestedMinutes = 13,
            title = "Torque check",
            instructions = """
                At 20 yd shoot 3×3: A = normal grip, B = tiny clockwise torque, C = tiny counter-clockwise.
                Log horizontal POI of B and C vs A.
                Large torque POI shift → grip is priority (fix grip before rest/sight).
                Normal small lateral error → note for walk-back; do not fix yet.
            """.trimIndent(),
            logFields = listOf(
                LogField("groupA", "Group A normal H vs aim"),
                LogField("groupB", "Group B tiny CW vs A (H\")"),
                LogField("groupC", "Group C tiny CCW vs A (H\")"),
                LogField("torqueVerdict", "Verdict", "grip priority / note for walk-back")
            )
        ),
        Block(
            id = "walkback",
            timeRange = "55–70",
            suggestedMinutes = 15,
            title = "Walk-back",
            instructions = """
                Vertical line on target. One aim point. Shoot 10 / 20 / 30 yd, repeat.
                Log horizontal displacement Pass 1 & Pass 2 at each distance.
                Trend that grows with distance → tiny lateral rest move (centershot).
                Whole line offset the same amount → move sight, not rest.
            """.trimIndent(),
            logFields = listOf(
                LogField("p1_10", "Pass1 H @10 yd"),
                LogField("p1_20", "Pass1 H @20 yd"),
                LogField("p1_30", "Pass1 H @30 yd"),
                LogField("p2_10", "Pass2 H @10 yd"),
                LogField("p2_20", "Pass2 H @20 yd"),
                LogField("p2_30", "Pass2 H @30 yd"),
                LogField("walkbackAction", "Action", "rest micro / sight / none")
            )
        ),
        Block(
            id = "balance",
            timeRange = "70–80",
            suggestedMinutes = 10,
            title = "Balance / stabilizer",
            instructions = """
                Take 5 full draws. Note bubble, pin float, hold, and reaction.
                Then two 3-arrow groups at 20 yd.
                Pin sits low → add rearward horizontal leverage (not just more vertical-down rear weight).
                Bubble rolls → rear lateral weight/angle.
                Change angle OR weight only — one variable.
            """.trimIndent(),
            logFields = listOf(
                LogField("drawNotes", "5-draw notes", "bubble / pin / float / reaction"),
                LogField("group1_20", "3-arrow #1 @20 W×H / POI"),
                LogField("group2_20", "3-arrow #2 @20 W×H / POI"),
                LogField("stabChange", "Stab change", "angle OR weight — what + result")
            )
        ),
        Block(
            id = "final_30",
            timeRange = "80–90",
            suggestedMinutes = 10,
            title = "Final 30 proof",
            instructions = """
                Two 3-arrow groups at 30 yd. No adjustments.
                Log W×H, POI vs aim, bad shots, pin behavior.
                This is the session proof card before the wrap-up numbers.
            """.trimIndent(),
            logFields = listOf(
                LogField("g1WxH", "Group 1 W×H @30"),
                LogField("g1Poi", "Group 1 POI vs aim"),
                LogField("g2WxH", "Group 2 W×H @30"),
                LogField("g2Poi", "Group 2 POI vs aim"),
                LogField("badShots", "Bad shots"),
                LogField("pin", "Pin behavior")
            )
        )
    )

    data class DecisionNode(
        val condition: String,
        val action: String
    )

    val decisionTree: List<DecisionNode> = listOf(
        DecisionNode(
            "Bare shafts consistently HIGH vs fletched at 20",
            "Raise nocking point a tiny amount (Easton). Retest. Do NOT move rest while fixing vertical."
        ),
        DecisionNode(
            "Bare shafts consistently LOW vs fletched at 20",
            "Lower nocking point a tiny amount. Retest. One variable only."
        ),
        DecisionNode(
            "Vertical now matches within day's dispersion; bare still L/R",
            "Leave nock alone. Note lateral for walk-back / centershot later — not this vertical pass."
        ),
        DecisionNode(
            "Nock adjust made vertical WORSE",
            "Revert immediately. Re-check form, spine match, and that bares are truly matched."
        ),
        DecisionNode(
            "Paper tear AGREES with bare vertical",
            "Investigate clearance, grip, and face contact before moving bow hardware further."
        ),
        DecisionNode(
            "Paper tear DISAGREES with bare",
            "Do not chase paper alone. Fix shooter/clearance path first; re-paper after."
        ),
        DecisionNode(
            "Torque check: large H POI shift CW/CCW vs normal",
            "Grip is priority. Rebuild neutral grip before rest or sight changes."
        ),
        DecisionNode(
            "Torque check: small/normal lateral only",
            "Note for walk-back. Do not \"fix\" with rest yet."
        ),
        DecisionNode(
            "Walk-back: H error grows with distance (fan)",
            "Tiny lateral rest / centershot move. Retest 10–20–30."
        ),
        DecisionNode(
            "Walk-back: whole vertical line offset equally",
            "Move sight windage, not rest."
        ),
        DecisionNode(
            "Pin sits low in housing / hold fights down",
            "Add rearward horizontal leverage (bar length/weight aft) — not only more vertical-down rear weight."
        ),
        DecisionNode(
            "Bubble rolls L or R at full draw",
            "Adjust rear lateral weight or angle. Change angle OR weight only."
        ),
        DecisionNode(
            "Final 30 groups acceptable; wrap-up numbers recorded",
            "Session complete. Save four wrap-up numbers. Next session: lateral fine-tune only if needed."
        ),
        DecisionNode(
            "Final 30 still vertically wild",
            "Do not start stabilizer/walk-back chase. Return to bare vertical with fresh form next session."
        )
    )
}
