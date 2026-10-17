package com.strobingn.bowtune.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Numeric chart metrics stored per [TuneSession] in `metricsJson`.
 * Kept as a flat `{"key": number}` object with a tiny hand-rolled codec so it is
 * usable from plain JVM unit tests (android.jar's org.json is a stub there).
 */
object TuneMetrics {
    const val SCORE = "score"
    const val GROUP_W = "groupW"
    const val GROUP_H = "groupH"
    /** Center-to-center extreme spread (longest gap between two arrows), inches. */
    const val GROUP_SPREAD = "groupSpread"
    /** Bare vs fletched, inches. + = bare high. */
    const val BARE_V = "bareV"
    /** Bare vs fletched, inches. + = bare left (RH archer). */
    const val BARE_H = "bareH"
    /** Broadhead vs field, inches. + = broadhead left. */
    const val BH_H = "bhH"
    /** Broadhead vs field, inches. + = broadhead high. */
    const val BH_V = "bhV"
    const val WIND_MPH = "windMph"
    const val TEMP_F = "tempF"

    val WALKBACK_DISTANCES = listOf(10, 20, 30, 40)

    /** Walk-back horizontal offset at [yd], inches. + = right of the vertical line. */
    fun walkBackKey(yd: Int) = "wb$yd"

    private val entryRegex = Regex("\"([^\"]+)\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?)")

    fun encode(values: Map<String, Double>): String =
        values.entries
            .filter { it.value.isFinite() }
            .joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
                "\"${k.replace("\"", "")}\":${formatNumber(v, 4)}"
            }

    fun decode(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        return entryRegex.findAll(json).mapNotNull { m ->
            m.groupValues[2].toDoubleOrNull()?.let { m.groupValues[1] to it }
        }.toMap()
    }
}

/** Slider bounds for a numeric entry. */
data class NumericSpec(
    val min: Double,
    val max: Double,
    val step: Double,
    val unit: String
)

/** Trims trailing zeros: 29.50 → "29.5", 70.0 → "70". Always uses '.' as the separator. */
fun formatNumber(value: Double, maxDecimals: Int = 2): String {
    if (!value.isFinite()) return ""
    val factor = Math.pow(10.0, maxDecimals.toDouble())
    val rounded = (value * factor).roundToLong() / factor
    val text = String.format(Locale.US, "%.${maxDecimals}f", rounded)
    val trimmed = if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
    return if (trimmed == "-0") "0" else trimmed
}

/** Snaps [value] onto a [step] grid anchored at [min], clamped to [min]..[max]. */
fun snapToStep(value: Double, min: Double, max: Double, step: Double): Double {
    if (step <= 0.0) return value.coerceIn(min, max)
    val steps = ((value - min) / step).roundToLong()
    return (min + steps * step).coerceIn(min, max)
}

private val strictNumberRegex = Regex(
    "^\\s*([-+]?(?:\\d+(?:[.,]\\d+)?|[.,]\\d+))\\s*" +
        "(?:\"|in|inch|inches|lb|lbs|#|gr|grain|grains|fps|yd|yds|ft|mph|°f|°|f|%|days?)?\\s*$",
    RegexOption.IGNORE_CASE
)

/**
 * A plain number with an optional unit suffix ("70", "29.5 in", "310fps"). Anything else
 * (free-text notes like "2 clicks left") returns null so callers can keep it as text.
 */
fun strictNumberOrNull(raw: String?): Double? {
    val m = strictNumberRegex.find(raw ?: return null) ?: return null
    return m.groupValues[1].replace(',', '.').toDoubleOrNull()
}

/** Parses "1/4", "1 1/4", "0.25", "-0.5", "+1". Returns null when no number is present. */
fun parseLooseNumber(raw: String?): Double? {
    val text = raw?.trim()?.replace(',', '.') ?: return null
    if (text.isEmpty()) return null
    Regex("([-+]?)(\\d+)\\s+(\\d+)\\s*/\\s*(\\d+)").find(text)?.let { m ->
        val whole = m.groupValues[2].toDouble()
        val den = m.groupValues[4].toDouble()
        if (den != 0.0) {
            val v = whole + m.groupValues[3].toDouble() / den
            return if (m.groupValues[1] == "-") -v else v
        }
    }
    Regex("([-+]?)(\\d+)\\s*/\\s*(\\d+)").find(text)?.let { m ->
        val den = m.groupValues[3].toDouble()
        if (den != 0.0) {
            val v = m.groupValues[2].toDouble() / den
            return if (m.groupValues[1] == "-") -v else v
        }
    }
    return Regex("[-+]?\\d*\\.?\\d+").find(text)?.value?.toDoubleOrNull()
}

/**
 * Parses a signed offset such as "+0.5 high", "0.25 left", "1/4 right" or "-0.5".
 * Direction words override the numeric sign: words in [positiveWords] give +, [negativeWords] give −.
 */
fun parseSignedOffset(
    raw: String?,
    positiveWords: List<String>,
    negativeWords: List<String>
): Double? {
    val value = parseLooseNumber(raw) ?: return null
    val lower = raw.orEmpty().lowercase(Locale.US)
    return when {
        positiveWords.any { Regex("\\b$it\\b").containsMatchIn(lower) } -> abs(value)
        negativeWords.any { Regex("\\b$it\\b").containsMatchIn(lower) } -> -abs(value)
        else -> value
    }
}

val VERTICAL_UP_WORDS = listOf("high", "up", "h")
val VERTICAL_DOWN_WORDS = listOf("low", "down", "l")
val LEFT_WORDS = listOf("left", "l")
val RIGHT_WORDS = listOf("right", "r")

/** "+0.5 high" style strings for vertical offsets (+ = high). */
fun describeVertical(inches: Double): String = when {
    abs(inches) < 0.005 -> "0 level"
    inches > 0 -> "${formatNumber(inches, 3)} high"
    else -> "${formatNumber(-inches, 3)} low"
}

/** Horizontal offsets where + = left (bare / broadhead convention). */
fun describeLeftPositive(inches: Double): String = when {
    abs(inches) < 0.005 -> "0 center"
    inches > 0 -> "${formatNumber(inches, 3)} left"
    else -> "${formatNumber(-inches, 3)} right"
}

/** Horizontal offsets where + = right (walk-back convention). */
fun describeRightPositive(inches: Double): String = when {
    abs(inches) < 0.005 -> "0 center"
    inches > 0 -> "${formatNumber(inches, 3)} right"
    else -> "${formatNumber(-inches, 3)} left"
}

/** Common inch fractions for tuning offsets, e.g. 0.125 → "1/8". Falls back to decimals. */
fun inchFraction(inches: Double): String {
    val sign = if (inches < 0) "-" else ""
    val a = abs(inches)
    val whole = a.toLong()
    val frac = a - whole
    val sixtyFourths = (frac * 64).roundToLong()
    if (abs(frac * 64 - sixtyFourths) > 0.01) return formatNumber(inches, 3)
    if (sixtyFourths == 0L) return "$sign$whole"
    if (sixtyFourths == 64L) return "$sign${whole + 1}"
    var num = sixtyFourths
    var den = 64L
    while (num % 2 == 0L) {
        num /= 2
        den /= 2
    }
    return if (whole == 0L) "$sign$num/$den" else "$sign$whole $num/$den"
}
