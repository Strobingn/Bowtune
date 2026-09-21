package com.strobingn.bowtune.data

object TuneHints {
    fun bareShaft(
        bareHighLowIn: Double?,
        bareLeftRightIn: Double?,
        rightHanded: Boolean = true
    ): String {
        if (bareHighLowIn == null && bareLeftRightIn == null) {
            return "Log bare vs fletched center-to-center in inches (positive high / left from the archer)."
        }
        val bits = mutableListOf<String>()
        bareHighLowIn?.let { v ->
            bits += when {
                v > 0.75 -> "Bare high — raise the nocking point a tiny amount (Easton). Do not move the rest for vertical."
                v < -0.75 -> "Bare low — lower the nocking point a tiny amount. One variable only."
                else -> "Vertical is inside ~3/4\". Leave nock height; confirm with another group."
            }
        }
        bareLeftRightIn?.let { h ->
            val left = if (rightHanded) h > 0.75 else h < -0.75
            val right = if (rightHanded) h < -0.75 else h > 0.75
            bits += when {
                left -> "Bare left (RH) — rest right in small clicks, or Limb Shift toward L. Or the shaft is weak."
                right -> "Bare right (RH) — rest left (in toward the riser), or Limb Shift toward R. Check vane/cable clearance."
                else -> "Horizontal is tight. Save leftover for walk-back centershot."
            }
        }
        bits += "One change, then re-shoot at least two matched bares."
        return bits.joinToString(" ")
    }

    fun walkBack(errorGrowsWithDistance: Boolean?, wholeLineOffset: Boolean?): String {
        return when {
            errorGrowsWithDistance == true ->
                "Fan / error grows with distance — tiny lateral rest / centershot move, then retest 10–20–30."
            wholeLineOffset == true ->
                "Whole line offset the same amount — move sight windage, not the rest."
            errorGrowsWithDistance == false && wholeLineOffset == false ->
                "Line looks stacked. Leave centershot; confirm with a second walk-back pass."
            else ->
                "If groups drift more at 30 than 10, move rest. If the whole column sits left/right equally, move the sight."
        }
    }

    fun broadhead(
        bhLeftRightIn: Double?,
        bhHighLowIn: Double?
    ): String {
        if (bhLeftRightIn == null && bhHighLowIn == null) {
            return "Compare broadhead POI to field points at 20–40 yd. Same point weight when you can."
        }
        val bits = mutableListOf<String>()
        bhHighLowIn?.let { v ->
            bits += when {
                v > 1.0 -> "Broadheads hit high — tiny nock-up or confirm tip weight / FOC vs the field point."
                v < -1.0 -> "Broadheads hit low — tiny nock-down or check FOC / blade planing."
                else -> "Vertical BH vs field is close."
            }
        }
        bhLeftRightIn?.let { h ->
            bits += when {
                h > 1.0 -> "Broadheads left — rest right a click, or Limb Shift toward L. Recheck paper first if the hole was not a bullet."
                h < -1.0 -> "Broadheads right — rest left (in toward the riser), or Limb Shift toward R. Check vane clearance."
                else -> "Horizontal BH vs field is close."
            }
        }
        bits += "One change at a time. Do not stack a rest move and a sight move in the same group."
        return bits.joinToString(" ")
    }
}
