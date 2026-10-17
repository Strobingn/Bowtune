package com.strobingn.bowtune.data

import kotlin.math.roundToInt

data class FocResult(
    val focPercent: Double?,
    val totalGrains: Double?,
    val note: String
)

data class SpineResult(
    val suggestedRange: String,
    val notes: List<String>
)

data class EnergyResult(
    val kineticFtLbs: Double?,
    val momentumSlugFtS: Double?,
    val note: String
)

object Calculators {
    /**
     * FOC from nock-end balance point. Optional component weights are summed for grain total.
     * FOC% = (balancePoint - length/2) / length * 100
     */
    fun foc(
        lengthIn: Double?,
        balanceFromNockIn: Double?,
        shaftGr: Double? = null,
        pointGr: Double? = null,
        insertGr: Double? = null,
        nockGr: Double? = null,
        vaneGr: Double? = null,
        wrapGr: Double? = null
    ): FocResult {
        val weights = listOf(shaftGr, pointGr, insertGr, nockGr, vaneGr, wrapGr)
        val parts = weights.filterNotNull()
        val total = if (parts.isEmpty()) null else parts.sum()
        if (weights.any { it != null && (!it.isFinite() || it < 0.0) }) {
            return FocResult(null, null, "Component weights must be zero or greater.")
        }
        if (lengthIn == null || !lengthIn.isFinite() || lengthIn <= 0.0 ||
            balanceFromNockIn == null || !balanceFromNockIn.isFinite()
        ) {
            return FocResult(
                focPercent = null,
                totalGrains = total,
                note = "Enter arrow length and balance point measured from the nock throat."
            )
        }
        if (balanceFromNockIn !in 0.0..lengthIn) {
            return FocResult(
                focPercent = null,
                totalGrains = total,
                note = "Balance point must fall between the nock throat and the end of the arrow."
            )
        }
        val foc = ((balanceFromNockIn - (lengthIn / 2.0)) / lengthIn) * 100.0
        val band = when {
            foc < 7 -> "Low FOC — verify stable flight before changing components."
            foc < 11 -> "Common target / 3D starting range."
            foc < 16 -> "Common hunting starting range."
            else -> "High FOC — the heavier front can lower speed; verify trajectory and broadhead flight."
        }
        val grains = total?.let { " Component total ${it.trim1()} gr." } ?: ""
        return FocResult(
            focPercent = foc,
            totalGrains = total,
            note = "FOC ${foc.trim1()}%. $band$grains"
        )
    }

    /**
     * Compound release / mechanical rest starting spine band.
     * Higher spine number = weaker shaft. Heavier DW, longer arrow, heavier point → stiffer (lower number).
     */
    fun spine(
        drawWeightLbs: Double?,
        arrowLengthIn: Double?,
        pointWeightGr: Double?
    ): SpineResult {
        if (drawWeightLbs == null || !drawWeightLbs.isFinite() || drawWeightLbs < 20 || drawWeightLbs > 90) {
            return SpineResult(
                "Enter draw weight (20–90 lb)",
                listOf("Use peak holding/draw weight on the limbs, not advertised IBO.")
            )
        }
        val length = arrowLengthIn ?: 28.0
        val point = pointWeightGr ?: 100.0
        if (!length.isFinite() || length !in 20.0..36.0) {
            return SpineResult(
                "Enter arrow length (20–36 in)",
                listOf("Use carbon-to-carbon shaft length, not draw length.")
            )
        }
        if (!point.isFinite() || point !in 50.0..300.0) {
            return SpineResult(
                "Enter point weight (50–300 gr)",
                listOf("Use total point-end weight if your shaft chart calls for it.")
            )
        }

        val base = when {
            drawWeightLbs < 40 -> 700 to 600
            drawWeightLbs < 45 -> 600 to 500
            drawWeightLbs < 50 -> 500 to 400
            drawWeightLbs < 55 -> 400 to 350
            drawWeightLbs < 60 -> 400 to 340
            drawWeightLbs < 65 -> 340 to 300
            drawWeightLbs < 70 -> 300 to 250
            drawWeightLbs < 80 -> 250 to 200
            else -> 200 to 150
        }

        var weak = base.first
        var stiff = base.second
        val notes = mutableListOf<String>()

        val lengthDelta = length - 28.0
        if (lengthDelta >= 1.0) {
            val steps = lengthDelta.toInt()
            weak -= 50 * steps
            stiff -= 50 * steps
            notes += "Arrow ${length.trim1()}\" is longer than a 28\" chart start — shift stiffer (lower spine number)."
        } else if (lengthDelta <= -1.0) {
            val steps = (-lengthDelta).toInt()
            weak += 50 * steps
            stiff += 50 * steps
            notes += "Shorter than 28\" can run a weaker (higher number) band."
        }

        if (point >= 125) {
            val steps = ((point - 100) / 25).toInt().coerceAtLeast(1)
            weak -= 50 * steps
            stiff -= 50 * steps
            notes += "Point ${point.trim0()} gr is heavy vs a 100 gr chart — need stiffer spine or less DW."
        } else if (point <= 85) {
            weak += 50
            stiff += 50
            notes += "Light point can let you run a weaker band; confirm paper (left tear if too weak)."
        }

        weak = weak.coerceIn(150, 900)
        stiff = stiff.coerceIn(120, 800)
        if (stiff > weak) {
            val swap = stiff
            stiff = weak
            weak = swap
        }

        notes += "RH release paper: leftover left tear after rest-right / Limb Shift L often means weak spine or too much DW."
        notes += "This estimate cannot replace a shaft maker's chart, which accounts for bow speed, cam aggressiveness, and point system. Verify there first, then confirm in flight."
        if (arrowLengthIn == null) notes += "No length entered — assumed 28\"."
        if (pointWeightGr == null) notes += "No point weight entered — assumed 100 gr."

        return SpineResult(
            suggestedRange = "$weak–$stiff spine (weaker→stiffer)",
            notes = notes
        )
    }

    /**
     * KE (ft-lb) = grains * fps² / 450240
     * Momentum (slug·ft/s) = grains * fps / 225218
     */
    fun energy(arrowGrains: Double?, speedFps: Double?): EnergyResult {
        if (arrowGrains == null || !arrowGrains.isFinite() || arrowGrains <= 0 ||
            speedFps == null || !speedFps.isFinite() || speedFps <= 0
        ) {
            return EnergyResult(
                null,
                null,
                "Enter finished arrow weight (grains) and chronograph speed (fps)."
            )
        }
        val ke = (arrowGrains * speedFps * speedFps) / 450240.0
        val mom = (arrowGrains * speedFps) / 225218.0
        val keBand = when {
            ke < 25 -> "Low energy; confirm the arrow and setup are suitable for the intended use."
            ke < 40 -> "Moderate energy. Penetration also depends on momentum, broadhead, and shot placement."
            ke < 55 -> "Strong energy for many hunting setups; check local equipment rules."
            else -> "High energy. Confirm arrow weight stays within the bow maker's minimum grains-per-pound limit."
        }
        return EnergyResult(
            kineticFtLbs = ke,
            momentumSlugFtS = mom,
            note = "KE ${ke.trim1()} ft-lb · momentum ${mom.trim2()} slug·ft/s. $keBand"
        )
    }

    private fun Double.trim0(): String = roundToInt().toString()
    private fun Double.trim1(): String = ((this * 10.0).roundToInt() / 10.0).toString()
    private fun Double.trim2(): String = ((this * 100.0).roundToInt() / 100.0).toString()
}

fun String.toDoubleOrNullLenient(): Double? =
    trim().replace(',', '.').replace(Regex("[^0-9.+-]"), "").toDoubleOrNull()
