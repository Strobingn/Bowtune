package com.strobingn.bowtune.data

import kotlin.math.roundToInt

data class FocResult(
    val focPercent: Double?,
    val totalGrains: Double?,
    val note: String
)

data class SpineResult(
    val suggestedRange: String,
    val notes: List<String>,
    /** Higher spine number (weaker shaft), when the chart has a cell. */
    val weaker: Int? = null,
    /** Lower spine number (stiffer shaft), when the chart has a cell. */
    val stiffer: Int? = null
)

data class EnergyResult(
    val kineticFtLbs: Double?,
    val momentumSlugFtS: Double?,
    val note: String
)

object Calculators {
    /**
     * AMO / Easton FOC from the nock throat.
     * FOC% = (balancePoint − length/2) / length × 100
     * Length is nock throat to the end of the shaft, not including the point.
     * Balance is measured from that same nock throat, with the point installed.
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
        val parts = listOfNotNull(shaftGr, pointGr, insertGr, nockGr, vaneGr, wrapGr)
        val total = if (parts.isEmpty()) null else parts.sum()
        val grainNote = total?.let {
            " Grain sum of the parts you typed: ${it.trim1()} gr — that is not a finished-arrow scale weight unless every piece is in the list."
        }.orEmpty()
        val gpiWarning = if (
            shaftGr != null && shaftGr < 35.0 && lengthIn != null && lengthIn > 18.0
        ) {
            " That shaft number looks like GPI, not grains. Multiply GPI by length before you trust the grain sum."
        } else {
            ""
        }
        if (lengthIn == null || lengthIn <= 0.0 || balanceFromNockIn == null) {
            return FocResult(
                focPercent = null,
                totalGrains = total,
                note = "Enter arrow length and the balance point measured from the nock throat.$grainNote$gpiWarning"
            )
        }
        if (balanceFromNockIn < 0.0 || balanceFromNockIn > lengthIn) {
            return FocResult(
                focPercent = null,
                totalGrains = total,
                note = "The balance point has to sit on the shaft, between the nock throat and the end of the shaft.$grainNote$gpiWarning"
            )
        }
        val foc = ((balanceFromNockIn - (lengthIn / 2.0)) / lengthIn) * 100.0
        val band = when {
            foc < 6.0 ->
                "Under 6% is very nose-light. A lot of target arrows start near 7%, and hunting builds are usually higher."
            foc < 10.0 ->
                "Common target FOC. Light for many hunting builds — a lot of hunters look for about 10–15%."
            foc < 15.0 ->
                "A normal hunting and 3D band. Gold Tip's older hunting note sits nearer 8–12%; fixed-blade builds often land here."
            foc < 20.0 ->
                "High FOC. Extra nose weight often stabilizes the arrow and can drift less, but it makes the shaft act weaker and drops speed if total grains went up."
            else ->
                "Very high FOC. Expect a weaker dynamic spine and a slower, more arcing shot when the point is heavy. Paper-tune and broadhead-tune before you trust it."
        }
        return FocResult(
            focPercent = foc,
            totalGrains = total,
            note = "FOC ${foc.trim1()}%. $band$grainNote$gpiWarning"
        )
    }

    /**
     * Starting spine from Easton hunting chart 301055-A (compound, mechanical release).
     * The chart is built for 301–340 fps ATA, a 100 gr point, and cut length from the
     * nock throat to the end of the shaft (point not included).
     *
     * Point weight moves the chart weight by 3 lb per 25 gr away from 100 gr.
     * Insert/outsert weight over 25 gr adds 3 lb per 25 gr.
     * ATA speed: ≤300 fps −5 lb, 341–350 +5 lb, ≥351 +10 lb.
     */
    fun spine(
        drawWeightLbs: Double?,
        arrowLengthIn: Double?,
        pointWeightGr: Double?,
        insertWeightGr: Double? = null,
        ataSpeedFps: Double? = null
    ): SpineResult {
        if (drawWeightLbs == null || drawWeightLbs < 15.0 || drawWeightLbs > 110.0) {
            return SpineResult(
                "Enter peak draw weight",
                listOf("Use the peak holding weight at your draw length, measured on a scale — not the advertised IBO number.")
            )
        }
        val length = arrowLengthIn ?: 28.0
        val point = pointWeightGr ?: 100.0
        val chartWeight = EastonHuntingChart.chartWeightLbs(
            peakLbs = drawWeightLbs,
            pointGrains = point,
            insertGrains = insertWeightGr,
            ataSpeedFps = ataSpeedFps
        )
        val roundedLength = length.roundToInt()
        val cell = EastonHuntingChart.cell(chartWeight, roundedLength)
        val notes = mutableListOf<String>()

        if (arrowLengthIn == null) {
            notes += "No cut length entered — assumed 28\". Measure nock throat to the end of the shaft, not draw length, and not including the point."
        } else if (kotlin.math.abs(length - roundedLength) >= 0.05) {
            notes += "Cut length ${length.trim1()}\" rounds to the ${roundedLength}\" column (Easton rounds .5\" up)."
        } else {
            notes += "Cut length is nock throat to the end of the shaft. Draw length is a different measurement."
        }

        if (pointWeightGr == null) {
            notes += "No point weight entered — assumed 100 gr, which is the chart's zero adjustment."
        } else if (kotlin.math.abs(point - 100.0) >= 1.0) {
            val delta = ((point - 100.0) / 25.0) * 3.0
            notes += "Point ${point.trim0()} gr shifts chart weight by ${signedLb(delta)} (3 lb per 25 gr from 100 gr)."
        }

        if (insertWeightGr != null && insertWeightGr > 25.0) {
            val delta = ((insertWeightGr - 25.0) / 25.0) * 3.0
            notes += "Insert/outsert ${insertWeightGr.trim0()} gr adds ${signedLb(delta)} (Easton: 3 lb per 25 gr over 25 gr)."
        } else if (insertWeightGr == null) {
            notes += "Insert weight not included. Easton adds 3 lb of chart weight per 25 gr of insert/outsert over 25 gr."
        }

        if (ataSpeedFps == null) {
            notes += "No ATA/IBO speed entered — chart assumes 301–340 fps. Up to 300 fps subtracts 5 lb; 341–350 adds 5 lb; 351+ adds 10 lb."
        } else {
            notes += "ATA speed ${ataSpeedFps.trim0()} fps is in Easton's ${EastonHuntingChart.speedBandLabel(ataSpeedFps)} band."
        }

        val adjusted = kotlin.math.abs(chartWeight - drawWeightLbs) >= 0.4
        if (adjusted) {
            notes += "Chart weight is ${chartWeight.trim1()} lb from a ${drawWeightLbs.trim1()} lb peak."
        }
        if (chartWeight < EastonHuntingChart.minPounds || chartWeight > EastonHuntingChart.maxPounds) {
            notes += "That chart weight sits outside Easton's ${EastonHuntingChart.minPounds}–${EastonHuntingChart.maxPounds} lb table, so this is the nearest row."
        }
        if (roundedLength !in EastonHuntingChart.minLength..EastonHuntingChart.maxLength) {
            notes += "Cut length rounds outside the ${EastonHuntingChart.minLength}–${EastonHuntingChart.maxLength}\" columns."
        }

        notes += "Carbon shafts are often labeled 340 where this chart prints 350, and 260 where it prints 250. Match your maker's number, then confirm on paper."
        notes += "RH mechanical release: a nock-left tear that remains after the rest moves right is a weak shaft. A nock-right tear is clearance or rest position before you call the shaft stiff."
        notes += "Finger release is not this chart — Easton adds 5 lb before you read it. This range is a start, not a substitute for the shaft maker's table."

        if (cell == null) {
            return SpineResult(
                suggestedRange = "No Easton group for this cell",
                notes = notes + "The hunting chart has no shaft at ${roundedLength}\" for this chart weight. Re-measure cut length before you pick a spine.",
                weaker = null,
                stiffer = null
            )
        }
        return SpineResult(
            suggestedRange = "${cell.first}–${cell.second} spine (weaker→stiffer)",
            notes = notes,
            weaker = cell.first,
            stiffer = cell.second
        )
    }

    /**
     * KE (ft·lb) = grains × fps² / 450240
     *   450240 = 2 × 7000 gr/lb × 32.16 ft/s², the constant printed on Easton and Gold Tip calculators.
     * Momentum (slug·ft/s) = grains × fps / 225218
     *   225218 = 7000 × 32.174, the constant used alongside that KE figure.
     */
    fun energy(arrowGrains: Double?, speedFps: Double?): EnergyResult {
        if (arrowGrains == null || arrowGrains <= 0 || speedFps == null || speedFps <= 0) {
            return EnergyResult(
                null,
                null,
                "Enter finished arrow weight (grains, on a scale) and chronograph speed (fps)."
            )
        }
        val ke = (arrowGrains * speedFps * speedFps) / 450240.0
        val mom = (arrowGrains * speedFps) / 225218.0
        val keBand = when {
            ke < 25.0 ->
                "Under 25 ft-lb is a target or small-game arrow. Most deer recommendations start higher."
            ke < 40.0 ->
                "25–40 ft-lb. A few rules mention the mid-20s as a deer floor, but many hunters want 40 ft-lb before they call it a deer arrow."
            ke < 65.0 ->
                "40–65 ft-lb is a common deer band. The top of this band is where a lot of elk recommendations begin."
            else ->
                "65 ft-lb and up is a common elk and larger-game band. More energy usually means a heavier arrow or more speed — watch the trajectory."
        }
        return EnergyResult(
            kineticFtLbs = ke,
            momentumSlugFtS = mom,
            note = "KE ${ke.trim1()} ft-lb · momentum ${mom.trim2()} slug·ft/s. $keBand Laws are usually draw weight and broadhead size, not a KE number."
        )
    }

    private fun signedLb(delta: Double): String {
        val rounded = (delta * 10.0).roundToInt() / 10.0
        return if (rounded >= 0) "+$rounded lb" else "$rounded lb"
    }

    private fun Double.trim0(): String = roundToInt().toString()
    private fun Double.trim1(): String = ((this * 10.0).roundToInt() / 10.0).toString()
    private fun Double.trim2(): String = ((this * 100.0).roundToInt() / 100.0).toString()
}

/**
 * Easton hunting shaft selector 301055-A, compound bow with a release aid.
 * Columns are cut length 23–34 inches. Cells are weaker→stiffer spine groups.
 * A null cell is a blank on the printed chart.
 */
internal object EastonHuntingChart {
    const val minLength = 23
    const val maxLength = 34
    const val minPounds = 22
    const val maxPounds = 90

    data class Row(val pounds: IntRange, val bands: List<Pair<Int, Int>?>)

    val rows: List<Row> = listOf(
        row(22..26, "--- --- --- 700-600 700-600 600-500 600-500 500-400 500-400 500-400 400-350 400-350"),
        row(27..31, "--- --- 700-600 700-600 600-500 600-500 500-400 500-400 500-400 400-350 400-350 400-350"),
        row(32..36, "--- 700-600 700-600 600-500 600-500 500-400 500-400 500-400 400-350 400-350 400-350 350-300"),
        row(37..41, "700-600 700-600 600-500 600-500 500-400 500-400 500-400 400-350 400-350 400-350 350-300 350-300"),
        row(42..46, "700-600 600-500 600-500 500-400 500-400 500-400 400-350 400-350 400-350 350-300 350-300 350-300"),
        row(47..51, "600-500 600-500 500-400 500-400 500-400 400-350 400-350 400-350 350-300 350-300 350-300 300-250"),
        row(52..56, "600-500 500-400 500-400 500-400 400-350 400-350 400-350 350-300 350-300 350-300 300-250 300-250"),
        row(57..61, "500-400 500-400 500-400 400-350 400-350 400-350 350-300 350-300 350-300 300-250 300-250 300-250"),
        row(62..66, "500-400 500-400 400-350 400-350 400-350 350-300 350-300 350-300 300-250 300-250 300-250 250-200"),
        row(67..72, "500-400 400-350 400-350 400-350 350-300 350-300 350-300 300-250 300-250 300-250 250-200 250-200"),
        row(73..78, "400-350 400-350 400-350 350-300 350-300 350-300 300-250 300-250 300-250 250-200 250-200 250-200"),
        row(79..84, "400-350 400-350 350-300 350-300 350-300 300-250 300-250 300-250 250-200 250-200 250-200 200-150"),
        row(85..90, "400-350 350-300 350-300 350-300 300-250 300-250 300-250 250-200 250-200 250-200 200-150 200-150")
    )

    init {
        check(rows.size == 13)
        check(rows.all { it.bands.size == 12 })
    }

    fun chartWeightLbs(
        peakLbs: Double,
        pointGrains: Double,
        insertGrains: Double?,
        ataSpeedFps: Double?
    ): Double {
        var weight = peakLbs + ((pointGrains - 100.0) / 25.0) * 3.0
        if (insertGrains != null && insertGrains > 25.0) {
            weight += ((insertGrains - 25.0) / 25.0) * 3.0
        }
        if (ataSpeedFps != null) weight += speedAdjustmentLbs(ataSpeedFps)
        return weight
    }

    fun speedAdjustmentLbs(ataSpeedFps: Double): Double = when {
        ataSpeedFps <= 300.0 -> -5.0
        ataSpeedFps < 341.0 -> 0.0
        ataSpeedFps <= 350.0 -> 5.0
        else -> 10.0
    }

    fun speedBandLabel(ataSpeedFps: Double): String = when {
        ataSpeedFps <= 300.0 -> "up to 300 fps (−5 lb)"
        ataSpeedFps < 341.0 -> "301–340 fps (no speed adjustment)"
        ataSpeedFps <= 350.0 -> "341–350 fps (+5 lb)"
        else -> "351 fps or faster (+10 lb)"
    }

    /** Weaker→stiffer pair, or null when that chart cell is blank or the length is off the sheet. */
    fun cell(chartWeightLbs: Double, roundedLengthIn: Int): Pair<Int, Int>? {
        if (roundedLengthIn !in minLength..maxLength) return null
        val row = rowFor(chartWeightLbs)
        return row.bands[roundedLengthIn - minLength]
    }

    fun rowFor(chartWeightLbs: Double): Row {
        rows.firstOrNull { chartWeightLbs >= it.pounds.first && chartWeightLbs <= it.pounds.last }
            ?.let { return it }
        return rows.minWith(
            compareBy<Row> { distanceTo(it, chartWeightLbs) }.thenByDescending { it.pounds.first }
        )
    }

    private fun distanceTo(row: Row, weight: Double): Double = when {
        weight < row.pounds.first -> row.pounds.first - weight
        weight > row.pounds.last -> weight - row.pounds.last
        else -> 0.0
    }

    private fun row(pounds: IntRange, spec: String): Row {
        val bands = spec.split(" ").map { token ->
            if (token == "---") null
            else {
                val parts = token.split("-")
                val weaker = parts[0].toInt()
                val stiffer = parts[1].toInt()
                check(weaker > stiffer) { token }
                weaker to stiffer
            }
        }
        return Row(pounds, bands)
    }
}

fun String.toDoubleOrNullLenient(): Double? =
    trim().replace(',', '.').replace(Regex("[^0-9.+-]"), "").toDoubleOrNull()
