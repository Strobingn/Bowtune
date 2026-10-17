package com.strobingn.bowtune.data

import kotlin.math.abs
import kotlin.math.roundToInt

data class FocResult(
    val focPercent: Double?,
    val totalGrains: Double?,
    val note: String,
    val balancePointIn: Double? = null,
    val isEstimatedFromComponents: Boolean = false,
    val shaftGrainsResolved: Double? = null,
    val frontGrains: Double? = null,
    val rearGrains: Double? = null
)

data class SpineResult(
    val suggestedRange: String,
    val notes: List<String>,
    val weakSpine: Int = 400,
    val stiffSpine: Int = 340,
    val effectiveDrawWeightLbs: Double? = null
)

data class EnergyResult(
    val kineticFtLbs: Double?,
    val momentumSlugFtS: Double?,
    val note: String,
    val grainsPerPound: Double? = null
)

object Calculators {
    /**
     * FOC from nock-end balance point (AMO standard).
     * FOC% = ((balancePoint - length / 2) / length) * 100
     *
     * Also resolves [shaftGr] whether entered as total shaft grains (e.g. 240 gr) or
     * GPI (grains per inch, e.g. 8.5 gr/in when <= 30), and if [balanceFromNockIn] is omitted,
     * estimates balance point and FOC from component moments.
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
        val resolvedShaftGr = when {
            shaftGr == null || shaftGr <= 0.0 -> null
            // Carbon/aluminum arrow GPI is ~4.5–18 gr/in; total bare shaft weight is >= 90 gr.
            shaftGr <= 30.0 && lengthIn != null && lengthIn > 10.0 -> shaftGr * lengthIn
            else -> shaftGr
        }
        val front = listOfNotNull(pointGr, insertGr).filter { it > 0.0 }.takeIf { it.isNotEmpty() }?.sum()
        val rear = listOfNotNull(nockGr, vaneGr, wrapGr).filter { it > 0.0 }.takeIf { it.isNotEmpty() }?.sum()
        val parts = listOfNotNull(resolvedShaftGr, pointGr, insertGr, nockGr, vaneGr, wrapGr).filter { it > 0.0 }
        val total = if (parts.isEmpty()) null else parts.sum()

        if (lengthIn == null || lengthIn <= 0.0) {
            return FocResult(
                focPercent = null,
                totalGrains = total,
                note = "Enter arrow length and balance point measured from the nock throat.",
                shaftGrainsResolved = resolvedShaftGr,
                frontGrains = front,
                rearGrains = rear
            )
        }

        if (balanceFromNockIn != null) {
            if (balanceFromNockIn <= 0.0 || balanceFromNockIn >= lengthIn) {
                return FocResult(
                    focPercent = null,
                    totalGrains = total,
                    note = "Balance point must be between 0\" and arrow length (${lengthIn.trim1()}\").",
                    shaftGrainsResolved = resolvedShaftGr,
                    frontGrains = front,
                    rearGrains = rear
                )
            }
            val foc = ((balanceFromNockIn - (lengthIn / 2.0)) / lengthIn) * 100.0
            val band = focBandDescription(foc)
            val grains = total?.let { " Component total ${it.trim1()} gr." } ?: ""
            return FocResult(
                focPercent = foc,
                totalGrains = total,
                note = "FOC ${foc.trim1()}%. $band$grains",
                balancePointIn = balanceFromNockIn,
                isEstimatedFromComponents = false,
                shaftGrainsResolved = resolvedShaftGr,
                frontGrains = front,
                rearGrains = rear
            )
        }

        // If balance point is not entered, estimate from component weights if shaft + front mass are present
        if (resolvedShaftGr != null && front != null && total != null && total > 0.0) {
            val pGr = (pointGr ?: 0.0).coerceAtLeast(0.0)
            val iGr = (insertGr ?: 0.0).coerceAtLeast(0.0)
            val nGr = (nockGr ?: 0.0).coerceAtLeast(0.0)
            val vGr = (vaneGr ?: 0.0).coerceAtLeast(0.0)
            val wGr = (wrapGr ?: 0.0).coerceAtLeast(0.0)
            // Center of mass locations from nock throat (0"):
            // Shaft at L/2, Point at L + 0.15", Insert at L - 0.35", Nock at 0.0", Vanes at 2.0", Wrap at 3.0"
            val moment = resolvedShaftGr * (lengthIn / 2.0) +
                pGr * (lengthIn + 0.15) +
                iGr * (lengthIn - 0.35).coerceAtLeast(lengthIn * 0.5) +
                nGr * 0.0 +
                vGr * 2.0.coerceAtMost(lengthIn * 0.25) +
                wGr * 3.0.coerceAtMost(lengthIn * 0.3)
            val estBal = (moment / total).coerceIn(0.1, lengthIn - 0.1)
            val estFoc = ((estBal - (lengthIn / 2.0)) / lengthIn) * 100.0
            val band = focBandDescription(estFoc)
            return FocResult(
                focPercent = estFoc,
                totalGrains = total,
                note = "Estimated FOC ${estFoc.trim1()}% (balance ~${estBal.trim1()}\" from nock). $band Component total ${total.trim1()} gr.",
                balancePointIn = estBal,
                isEstimatedFromComponents = true,
                shaftGrainsResolved = resolvedShaftGr,
                frontGrains = front,
                rearGrains = rear
            )
        }

        return FocResult(
            focPercent = null,
            totalGrains = total,
            note = "Enter arrow length and balance point measured from the nock throat (or enter shaft + point weights to estimate).",
            shaftGrainsResolved = resolvedShaftGr,
            frontGrains = front,
            rearGrains = rear
        )
    }

    private fun focBandDescription(foc: Double): String = when {
        foc < 7 -> "Low FOC — fine for some target setups; hunting usually wants more."
        foc < 11 -> "Typical target / 3D FOC band (7–11%)."
        foc < 16 -> "Typical hunting FOC band (11–16%)."
        else -> "High / extreme FOC (>16%) — strong broadhead recovery; watch trajectory drop at long range."
    }

    /**
     * Compound release / mechanical rest starting spine band.
     * Models effective draw weight from peak DW, arrow length delta from 28", and point/insert weight delta from 100 gr:
     *   +1" arrow length ≈ +5 lb effective DW
     *   +25 gr point/insert weight ≈ +3.5 lb effective DW
     * Higher spine number = weaker shaft. Heavier DW, longer arrow, heavier point → stiffer (lower number).
     */
    fun spine(
        drawWeightLbs: Double?,
        arrowLengthIn: Double?,
        pointWeightGr: Double?
    ): SpineResult {
        if (drawWeightLbs == null || drawWeightLbs < 20 || drawWeightLbs > 90) {
            return SpineResult(
                "Enter draw weight (20–90 lb)",
                listOf("Use peak holding/draw weight on the limbs, not advertised IBO.")
            )
        }
        val length = (arrowLengthIn ?: 28.0).coerceIn(20.0, 35.0)
        val point = (pointWeightGr ?: 100.0).coerceIn(50.0, 350.0)
        val notes = mutableListOf<String>()

        val lengthDelta = length - 28.0
        val pointDelta = point - 100.0
        val effectiveDw = drawWeightLbs + (lengthDelta * 5.0) + ((pointDelta / 25.0) * 3.5)

        val (weak, stiff) = when {
            effectiveDw < 36.0 -> 700 to 600
            effectiveDw < 43.0 -> 600 to 500
            effectiveDw < 50.0 -> 500 to 400
            effectiveDw < 56.0 -> 400 to 350
            effectiveDw < 63.0 -> 400 to 340
            effectiveDw < 72.0 -> 340 to 300
            effectiveDw < 82.0 -> 300 to 250
            effectiveDw < 92.0 -> 250 to 200
            else -> 200 to 150
        }

        if (lengthDelta >= 0.5) {
            notes += "Arrow ${length.trim1()}\" is longer than a 28\" chart baseline (+${(lengthDelta * 5.0).trim1()} lb effective load) — shifts stiffer (lower spine number)."
        } else if (lengthDelta <= -0.5) {
            notes += "Arrow ${length.trim1()}\" is shorter than 28\" (${(lengthDelta * 5.0).trim1()} lb effective load) — allows a weaker (higher number) band."
        }

        if (point >= 120.0) {
            notes += "Front weight ${point.trim0()} gr is heavier than a 100 gr chart baseline (+${((pointDelta / 25.0) * 3.5).trim1()} lb effective load) — shifts stiffer."
        } else if (point <= 85.0) {
            notes += "Light point (${point.trim0()} gr) stiffens dynamic reaction; can run a weaker spine band."
        }

        notes += "Effective chart load ~${effectiveDw.trim1()} lb. RH release paper: leftover left tear after rest-right / Limb Shift L often means dynamically weak spine."
        notes += "This is a starting range, not a brand chart. Confirm with Easton/manufacturer tables and paper at 4-6 ft."
        if (arrowLengthIn == null) notes += "No length entered — assumed 28\"."
        if (pointWeightGr == null) notes += "No point weight entered — assumed 100 gr."

        return SpineResult(
            suggestedRange = "$weak–$stiff spine (weaker→stiffer)",
            notes = notes,
            weakSpine = weak,
            stiffSpine = stiff,
            effectiveDrawWeightLbs = effectiveDw
        )
    }

    /**
     * KE (ft-lb) = grains * fps² / 450240
     * Momentum (slug·ft/s) = grains * fps / 225218
     */
    fun energy(
        arrowGrains: Double?,
        speedFps: Double?,
        drawWeightLbs: Double? = null
    ): EnergyResult {
        if (arrowGrains == null || arrowGrains <= 0 || speedFps == null || speedFps <= 0) {
            return EnergyResult(
                null,
                null,
                "Enter finished arrow weight (grains) and chronograph speed (fps)."
            )
        }
        val ke = (arrowGrains * speedFps * speedFps) / 450240.0
        val mom = (arrowGrains * speedFps) / 225218.0
        val gpp = if (drawWeightLbs != null && drawWeightLbs > 0) arrowGrains / drawWeightLbs else null
        val keBand = when {
            ke < 25 -> "Light target / small game energy (<25 ft-lb)."
            ke < 42 -> "Medium game / whitetail deer class (25–42 ft-lb)."
            ke < 65 -> "Elk / black bear / large game class (42–65 ft-lb)."
            else -> "Heavy / dangerous game class (65+ ft-lb) — strong penetration."
        }
        val safetyWarn = if (gpp != null && gpp < 5.0) {
            " Warning: ${gpp.trim1()} gr/lb is below the 5.0 gr/lb IBO safety floor (risk of limb/cam damage)."
        } else if (gpp != null) {
            " (${gpp.trim1()} gr/lb)."
        } else {
            ""
        }
        return EnergyResult(
            kineticFtLbs = ke,
            momentumSlugFtS = mom,
            note = "KE ${ke.trim1()} ft-lb · momentum ${mom.trim2()} slug·ft/s. $keBand$safetyWarn",
            grainsPerPound = gpp
        )
    }

    /**
     * Estimates real-world arrow speed (fps) from IBO rated speed (70 lb, 30" DL, 350 gr arrow):
     * - ±10 fps per inch of draw length from 30"
     * - -1 fps per 3 grains of arrow weight above (5 * drawWeightLbs), +1 fps per 3 grains below
     * - ~2 fps per lb of draw weight from 70 lb at 5 gr/lb baseline, minus ~4 fps for string accessories (D-loop + peep)
     */
    fun estimateSpeedFps(
        iboFps: Double?,
        drawLengthIn: Double?,
        drawWeightLbs: Double?,
        arrowGrains: Double?
    ): Double? {
        if (iboFps == null || iboFps <= 150.0) return null
        val dl = drawLengthIn ?: 29.0
        val dw = drawWeightLbs ?: 70.0
        val gr = arrowGrains ?: 400.0
        val dlAdjust = (dl - 30.0) * 10.0
        val fiveGppBaseline = dw * 5.0
        val weightAdjust = -((gr - fiveGppBaseline) / 3.0)
        val dwEfficiencyAdjust = (dw - 70.0) * 0.5
        val stringAccessoriesLoss = -4.0
        return (iboFps + dlAdjust + weightAdjust + dwEfficiencyAdjust + stringAccessoriesLoss)
            .coerceIn(120.0, 420.0)
    }

    private fun Double.trim0(): String = roundToInt().toString()
    private fun Double.trim1(): String = ((this * 10.0).roundToInt() / 10.0).toString()
    private fun Double.trim2(): String = ((this * 100.0).roundToInt() / 100.0).toString()
}

fun String.toDoubleOrNullLenient(): Double? =
    trim().replace(',', '.').replace(Regex("[^0-9.+-]"), "").toDoubleOrNull()
