package com.strobingn.bowtune.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/** One plotted value with the time it was logged. */
data class TimedValue(val epochMs: Long, val value: Double)

/** Parsed "Score / group" text from a session. */
sealed interface ScoreOrGroup {
    data class Score(val value: Double) : ScoreOrGroup
    data class Group(val width: Double, val height: Double?) : ScoreOrGroup {
        /** Largest dimension — the usual "group size" figure. */
        val size: Double get() = max(width, height ?: width)
    }
}

/**
 * Nock-end direction of a paper tear as seen by the archer.
 * x: + = nock right, − = nock left. y: + = nock high, − = nock low.
 */
data class TearVector(val x: Double, val y: Double, val magnitudeKnown: Boolean) {
    val length: Double get() = sqrt(x * x + y * y)
}

data class WalkBackPass(
    val label: String,
    /** Distance (yd) → horizontal offset (in, + = right). Sorted by distance. */
    val points: List<Pair<Int, Double>>
)

enum class WalkBackPattern(val label: String, val advice: String) {
    STACKED("Stacked", "Line looks stacked — leave centershot."),
    FAN("Fan", "Error grows with distance — tiny lateral rest / centershot move."),
    PARALLEL("Parallel offset", "Whole line offset equally — move sight windage, not the rest."),
    UNKNOWN("Need 2+ distances", "Log at least two distances to see the trend.")
}

data class OffsetPoint(
    val epochMs: Long,
    /** Inches, + = right of the fletched / field-point group (chart convention). */
    val x: Double,
    /** Inches, + = high. */
    val y: Double,
    val source: String
)

object ChartData {
    const val DEFAULT_TEAR_MAGNITUDE_IN = 0.5
    private const val DAY_MS = 86_400_000L
    private const val WEEK_MS = 7 * DAY_MS

    fun parseScoreOrGroup(raw: String?): ScoreOrGroup? {
        val text = raw?.trim()?.replace(',', '.') ?: return null
        if (text.isEmpty()) return null
        Regex("(\\d+(?:\\.\\d+)?)\\s*[x×X*]\\s*(\\d+(?:\\.\\d+)?)").find(text)?.let { m ->
            return ScoreOrGroup.Group(m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
        }
        val number = Regex("\\d+(?:\\.\\d+)?").find(text)?.value?.toDoubleOrNull() ?: return null
        val lower = text.lowercase()
        val looksLikeInches = text.contains('"') || Regex("\\d\\s*in\\b|inch").containsMatchIn(lower)
        val looksLikeScore = lower.contains("pt") || lower.contains("score") || lower.contains("/")
        return when {
            looksLikeInches -> ScoreOrGroup.Group(number, null)
            looksLikeScore -> ScoreOrGroup.Score(number)
            number >= 15.0 -> ScoreOrGroup.Score(number)
            else -> ScoreOrGroup.Group(number, null)
        }
    }

    /** Structured metrics win; otherwise fall back to the free-text score field. */
    fun scoreOrGroupFor(session: TuneSession): ScoreOrGroup? {
        val m = TuneMetrics.decode(session.metricsJson)
        m[TuneMetrics.SCORE]?.let { return ScoreOrGroup.Score(it) }
        m[TuneMetrics.GROUP_W]?.let { return ScoreOrGroup.Group(it, m[TuneMetrics.GROUP_H]) }
        return parseScoreOrGroup(session.scoreOrGroup)
    }

    /** Oldest → newest score values. */
    fun scoreTrend(sessions: List<TuneSession>): List<TimedValue> =
        sessions.mapNotNull { s ->
            (scoreOrGroupFor(s) as? ScoreOrGroup.Score)?.let { TimedValue(s.dateEpochMs, it.value) }
        }.sortedBy { it.epochMs }

    /** Oldest → newest (width, height?) groups, including LIFT final-30 wrap-ups. */
    fun groupTrend(
        sessions: List<TuneSession>,
        guided: List<GuidedTuneSession> = emptyList()
    ): List<Pair<Long, ScoreOrGroup.Group>> {
        val fromSessions = sessions.mapNotNull { s ->
            (scoreOrGroupFor(s) as? ScoreOrGroup.Group)?.let { s.dateEpochMs to it }
        }
        val sessionTimes = fromSessions.map { it.first }.toSet()
        val fromGuided = guided
            .filter { it.status == GuidedTuneSession.STATUS_COMPLETED }
            .mapNotNull { g ->
                val at = g.completedAtEpochMs ?: g.updatedAtEpochMs
                if (at in sessionTimes) return@mapNotNull null
                (parseScoreOrGroup(g.final30GroupSize) as? ScoreOrGroup.Group)?.let { at to it }
            }
        return (fromSessions + fromGuided).sortedBy { it.first }
    }

    fun movingAverage(values: List<Double>, window: Int): List<Double> {
        if (window <= 1) return values
        return values.indices.map { i ->
            val from = (i - window + 1).coerceAtLeast(0)
            values.subList(from, i + 1).average()
        }
    }

    fun tearVector(type: TearType, magnitudeIn: Double?): TearVector {
        val (dx, dy) = when (type) {
            TearType.BULLET -> 0.0 to 0.0
            TearType.NOCK_HIGH -> 0.0 to 1.0
            TearType.NOCK_LOW -> 0.0 to -1.0
            TearType.LEFT -> -1.0 to 0.0
            TearType.RIGHT -> 1.0 to 0.0
            TearType.HIGH_LEFT -> -DIAG to DIAG
            TearType.HIGH_RIGHT -> DIAG to DIAG
            TearType.LOW_LEFT -> -DIAG to -DIAG
            TearType.LOW_RIGHT -> DIAG to -DIAG
        }
        val known = magnitudeIn != null && magnitudeIn >= 0.0
        val mag = when {
            type == TearType.BULLET -> 0.0
            known -> magnitudeIn!!
            else -> DEFAULT_TEAR_MAGNITUDE_IN
        }
        return TearVector(dx * mag, dy * mag, known || type == TearType.BULLET)
    }

    fun tearVector(log: PaperTearLog): TearVector? =
        TearType.fromStored(log.tearType)?.let { tearVector(it, log.magnitudeIn) }

    /** Share of bullet holes among the newest [lastN] logs (input newest-first or any order). */
    fun bulletRate(logs: List<PaperTearLog>, lastN: Int = 10): Double? {
        val recent = logs.sortedByDescending { it.dateEpochMs }.take(lastN)
        if (recent.isEmpty()) return null
        return recent.count { TearType.fromStored(it.tearType) == TearType.BULLET }.toDouble() / recent.size
    }

    fun tearTypeCounts(logs: List<PaperTearLog>): Map<TearType, Int> {
        val counts = logs.mapNotNull { TearType.fromStored(it.tearType) }.groupingBy { it }.eachCount()
        return TearType.entries.associateWith { counts[it] ?: 0 }
    }

    fun sessionKindCounts(sessions: List<TuneSession>): Map<String, Int> {
        val counts = sessions.groupingBy { it.sessionKind }.eachCount()
        return SessionKind.all.associateWith { counts[it] ?: 0 }
    }

    /** Session counts per 7-day bucket, oldest bucket first, the last bucket ending at [nowMs]. */
    fun perWeek(timestamps: List<Long>, nowMs: Long, weeks: Int = 8): List<Int> {
        val buckets = IntArray(weeks)
        timestamps.forEach { t ->
            val age = nowMs - t
            if (age < 0) return@forEach
            val idx = (age / WEEK_MS).toInt()
            if (idx < weeks) buckets[weeks - 1 - idx]++
        }
        return buckets.toList()
    }

    fun daysUntil(dueEpochMs: Long, nowMs: Long): Long =
        Math.floorDiv(dueEpochMs - nowMs + DAY_MS - 1, DAY_MS)

    fun walkBackFromSession(session: TuneSession): WalkBackPass? {
        val m = TuneMetrics.decode(session.metricsJson)
        val pts = TuneMetrics.WALKBACK_DISTANCES.mapNotNull { yd ->
            m[TuneMetrics.walkBackKey(yd)]?.let { yd to it }
        }
        return if (pts.isEmpty()) null else WalkBackPass("Walk-back", pts)
    }

    /** Extracts both walk-back passes from LIFT 29.5 block logs (`walkback.p1_10` … `walkback.p2_30`). */
    fun walkBackFromLiftLogs(fields: Map<String, String>): List<WalkBackPass> =
        listOf(1, 2).mapNotNull { pass ->
            val pts = listOf(10, 20, 30).mapNotNull { yd ->
                parseSignedOffset(fields["walkback.p${pass}_$yd"], RIGHT_WORDS, LEFT_WORDS)?.let { yd to it }
            }
            if (pts.isEmpty()) null else WalkBackPass("Pass $pass", pts)
        }

    /**
     * Fan = least-squares slope moves the impact > [toleranceIn] across 20 yd;
     * parallel = flat slope but mean offset beyond tolerance; else stacked.
     */
    fun classifyWalkBack(points: List<Pair<Int, Double>>, toleranceIn: Double = 0.75): WalkBackPattern {
        val distinct = points.map { it.first }.distinct()
        if (distinct.size < 2) return WalkBackPattern.UNKNOWN
        val n = points.size.toDouble()
        val mx = points.sumOf { it.first.toDouble() } / n
        val my = points.sumOf { it.second } / n
        val sxx = points.sumOf { (it.first - mx) * (it.first - mx) }
        val sxy = points.sumOf { (it.first - mx) * (it.second - my) }
        val slope = if (sxx == 0.0) 0.0 else sxy / sxx
        return when {
            abs(slope * 20.0) > toleranceIn -> WalkBackPattern.FAN
            abs(my) > toleranceIn -> WalkBackPattern.PARALLEL
            else -> WalkBackPattern.STACKED
        }
    }

    /** Bare-shaft and broadhead offsets in chart coordinates (+x = right, +y = high). */
    fun offsetPoints(
        sessions: List<TuneSession>,
        guided: List<GuidedTuneSession> = emptyList()
    ): List<OffsetPoint> {
        val out = mutableListOf<OffsetPoint>()
        sessions.forEach { s ->
            val m = TuneMetrics.decode(s.metricsJson)
            val bv = m[TuneMetrics.BARE_V]
            val bh = m[TuneMetrics.BARE_H]
            if (bv != null || bh != null) {
                out += OffsetPoint(s.dateEpochMs, -(bh ?: 0.0), bv ?: 0.0, SOURCE_BARE)
            }
            val hv = m[TuneMetrics.BH_V]
            val hh = m[TuneMetrics.BH_H]
            if (hv != null || hh != null) {
                out += OffsetPoint(s.dateEpochMs, -(hh ?: 0.0), hv ?: 0.0, SOURCE_BROADHEAD)
            }
        }
        guided.filter { it.status == GuidedTuneSession.STATUS_COMPLETED }.forEach { g ->
            val v = parseSignedOffset(g.bareShaftHl20, VERTICAL_UP_WORDS, VERTICAL_DOWN_WORDS)
            val h = parseSignedOffset(g.bareShaftLr20, LEFT_WORDS, RIGHT_WORDS)
            if (v != null || h != null) {
                out += OffsetPoint(g.completedAtEpochMs ?: g.updatedAtEpochMs, -(h ?: 0.0), v ?: 0.0, SOURCE_LIFT)
            }
        }
        return out.sortedBy { it.epochMs }
    }

    const val SOURCE_BARE = "bare"
    const val SOURCE_BROADHEAD = "broadhead"
    const val SOURCE_LIFT = "lift"

    private const val DIAG = 0.7071067811865476
}
