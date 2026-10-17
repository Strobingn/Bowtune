package com.strobingn.bowtune.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDataTest {
    private val day = 86_400_000L

    private fun session(
        t: Long,
        score: String = "",
        metrics: Map<String, Double> = emptyMap(),
        kind: String = SessionKind.GROUP
    ) = TuneSession(
        dateEpochMs = t,
        distanceYd = "20",
        scoreOrGroup = score,
        sessionKind = kind,
        metricsJson = TuneMetrics.encode(metrics)
    )

    @Test
    fun parsesGroupAndScoreText() {
        assertEquals(ScoreOrGroup.Group(2.5, 3.0), ChartData.parseScoreOrGroup("2.5×3.0"))
        assertEquals(ScoreOrGroup.Group(2.5, 3.0), ChartData.parseScoreOrGroup("2.5 x 3"))
        assertEquals(ScoreOrGroup.Group(1.75, null), ChartData.parseScoreOrGroup("1.75\""))
        assertEquals(ScoreOrGroup.Group(4.0, null), ChartData.parseScoreOrGroup("4 in"))
        assertEquals(ScoreOrGroup.Score(295.0), ChartData.parseScoreOrGroup("295"))
        assertEquals(ScoreOrGroup.Score(58.0), ChartData.parseScoreOrGroup("58/60"))
        assertEquals(ScoreOrGroup.Group(3.0, null), ChartData.parseScoreOrGroup("3"))
        assertNull(ChartData.parseScoreOrGroup("Wizard complete"))
        assertNull(ChartData.parseScoreOrGroup(""))
        assertEquals(3.0, ScoreOrGroup.Group(2.5, 3.0).size, 1e-9)
    }

    @Test
    fun structuredMetricsWinOverText() {
        val s = session(1, score = "free text 9", metrics = mapOf(TuneMetrics.SCORE to 280.0))
        assertEquals(ScoreOrGroup.Score(280.0), ChartData.scoreOrGroupFor(s))
        val g = session(1, metrics = mapOf(TuneMetrics.GROUP_W to 2.0, TuneMetrics.GROUP_H to 2.5))
        assertEquals(ScoreOrGroup.Group(2.0, 2.5), ChartData.scoreOrGroupFor(g))
    }

    @Test
    fun cameraGroupSpreadIsTheGroupSize() {
        val measured = session(
            5,
            score = "1.4 in",
            metrics = mapOf(TuneMetrics.GROUP_SPREAD to 1.4, TuneMetrics.GROUP_W to 1.1, TuneMetrics.GROUP_H to 0.9)
        )
        val group = ChartData.scoreOrGroupFor(measured) as ScoreOrGroup.Group
        assertEquals(1.4, group.size, 1e-9)
        assertEquals(1.1, group.width, 1e-9)
        assertEquals(0.9, group.height!!, 1e-9)
        val legacyCamera = session(6, score = "1.4 in")
        assertEquals(1.4, (ChartData.scoreOrGroupFor(legacyCamera) as ScoreOrGroup.Group).size, 1e-9)
        assertEquals(listOf(1.4, 1.4), ChartData.groupTrend(listOf(measured, legacyCamera)).map { it.second.size })
    }

    @Test
    fun trendsAreOldestFirstAndSplitByType() {
        val sessions = listOf(
            session(3 * day, "300"),
            session(1 * day, "290"),
            session(2 * day, "2x3"),
            session(4 * day, "")
        )
        assertEquals(listOf(290.0, 300.0), ChartData.scoreTrend(sessions).map { it.value })
        assertEquals(listOf(2 * day), ChartData.groupTrend(sessions).map { it.first })
    }

    @Test
    fun groupTrendAddsLiftWrapUpWithoutDuplicatingItsSummarySession() {
        val done = 10 * day
        val lift = GuidedTuneSession(
            protocolId = LiftVerticalTunePlan.PROTOCOL_ID,
            title = "LIFT",
            startedAtEpochMs = 0,
            updatedAtEpochMs = done,
            completedAtEpochMs = done,
            status = GuidedTuneSession.STATUS_COMPLETED,
            final30GroupSize = "2.5×3.0"
        )
        val other = lift.copy(completedAtEpochMs = 20 * day, updatedAtEpochMs = 20 * day, final30GroupSize = "2x2")
        val summary = session(done, "2.5×3.0")
        val trend = ChartData.groupTrend(listOf(summary), listOf(lift, other))
        assertEquals(listOf(done, 20 * day), trend.map { it.first })
    }

    @Test
    fun movingAverageUsesTrailingWindow() {
        assertEquals(listOf(1.0, 1.5, 2.0, 3.0), ChartData.movingAverage(listOf(1.0, 2.0, 3.0, 4.0), 3))
    }

    @Test
    fun tearVectorsPointInNockDirection() {
        val left = ChartData.tearVector(TearType.LEFT, 1.0)
        assertEquals(-1.0, left.x, 1e-9)
        assertEquals(0.0, left.y, 1e-9)
        val hr = ChartData.tearVector(TearType.HIGH_RIGHT, 1.0)
        assertTrue(hr.x > 0 && hr.y > 0)
        assertEquals(1.0, hr.length, 1e-9)
        val bullet = ChartData.tearVector(TearType.BULLET, null)
        assertEquals(0.0, bullet.length, 1e-9)
        assertTrue(bullet.magnitudeKnown)
        val unknown = ChartData.tearVector(TearType.NOCK_LOW, null)
        assertFalse(unknown.magnitudeKnown)
        assertEquals(-ChartData.DEFAULT_TEAR_MAGNITUDE_IN, unknown.y, 1e-9)
    }

    @Test
    fun tearTypeFromStoredAcceptsNameOrLabel() {
        assertEquals(TearType.NOCK_HIGH, TearType.fromStored("NOCK_HIGH"))
        assertEquals(TearType.NOCK_HIGH, TearType.fromStored("Nock-high"))
        assertEquals(TearType.LOW_LEFT, TearType.fromStored(" low-left "))
        assertNull(TearType.fromStored("sideways"))
        assertNull(TearType.fromStored(""))
    }

    @Test
    fun bulletRateAndCounts() {
        val logs = listOf(
            PaperTearLog(dateEpochMs = 1, tearType = "BULLET"),
            PaperTearLog(dateEpochMs = 2, tearType = "LEFT"),
            PaperTearLog(dateEpochMs = 3, tearType = "BULLET"),
            PaperTearLog(dateEpochMs = 4, tearType = "BULLET")
        )
        assertEquals(0.75, ChartData.bulletRate(logs)!!, 1e-9)
        assertEquals(1.0, ChartData.bulletRate(logs, lastN = 2)!!, 1e-9)
        assertNull(ChartData.bulletRate(emptyList()))
        val counts = ChartData.tearTypeCounts(logs)
        assertEquals(3, counts[TearType.BULLET])
        assertEquals(1, counts[TearType.LEFT])
        assertEquals(0, counts[TearType.RIGHT])
        assertEquals(TearType.entries.size, counts.size)
    }

    @Test
    fun perWeekBucketsNewestLast() {
        val now = 100 * day
        val buckets = ChartData.perWeek(listOf(now - day, now - 2 * day, now - 8 * day, now - 100 * day, now + day), now, weeks = 4)
        assertEquals(listOf(0, 0, 1, 2), buckets)
    }

    @Test
    fun daysUntilRoundsUp() {
        val now = 50 * day
        assertEquals(1, ChartData.daysUntil(now + 1, now))
        assertEquals(30, ChartData.daysUntil(now + 30 * day, now))
        assertEquals(0, ChartData.daysUntil(now, now))
        assertEquals(-1, ChartData.daysUntil(now - day, now))
    }

    @Test
    fun classifiesWalkBackPatterns() {
        assertEquals(WalkBackPattern.FAN, ChartData.classifyWalkBack(listOf(10 to 0.0, 20 to 1.0, 30 to 2.0)))
        assertEquals(WalkBackPattern.PARALLEL, ChartData.classifyWalkBack(listOf(10 to 1.5, 20 to 1.5, 30 to 1.75)))
        assertEquals(WalkBackPattern.STACKED, ChartData.classifyWalkBack(listOf(10 to 0.0, 20 to 0.25, 30 to -0.25)))
        assertEquals(WalkBackPattern.UNKNOWN, ChartData.classifyWalkBack(listOf(20 to 1.0)))
    }

    @Test
    fun walkBackFromLiftLogsParsesBothPasses() {
        val fields = mapOf(
            "walkback.p1_10" to "0",
            "walkback.p1_20" to "0.5 right",
            "walkback.p1_30" to "1",
            "walkback.p2_20" to "0.25 L",
            "walkback.walkbackAction" to "rest micro"
        )
        val passes = ChartData.walkBackFromLiftLogs(fields)
        assertEquals(2, passes.size)
        assertEquals(listOf(10 to 0.0, 20 to 0.5, 30 to 1.0), passes[0].points)
        assertEquals(listOf(20 to -0.25), passes[1].points)
        assertTrue(ChartData.walkBackFromLiftLogs(emptyMap()).isEmpty())
    }

    @Test
    fun walkBackFromSessionMetrics() {
        val s = session(1, metrics = mapOf(TuneMetrics.walkBackKey(10) to 0.0, TuneMetrics.walkBackKey(30) to 1.5))
        assertEquals(listOf(10 to 0.0, 30 to 1.5), ChartData.walkBackFromSession(s)!!.points)
        assertNull(ChartData.walkBackFromSession(session(1)))
    }

    @Test
    fun offsetPointsUseRightPositiveChartCoordinates() {
        val bare = session(1, metrics = mapOf(TuneMetrics.BARE_V to 1.0, TuneMetrics.BARE_H to 0.5), kind = SessionKind.BARE_SHAFT)
        val bh = session(2, metrics = mapOf(TuneMetrics.BH_H to -2.0), kind = SessionKind.BROADHEAD)
        val lift = GuidedTuneSession(
            protocolId = LiftVerticalTunePlan.PROTOCOL_ID,
            title = "LIFT",
            startedAtEpochMs = 0,
            updatedAtEpochMs = 3,
            completedAtEpochMs = 3,
            status = GuidedTuneSession.STATUS_COMPLETED,
            bareShaftHl20 = "0.5 low",
            bareShaftLr20 = "0.25 right"
        )
        val pts = ChartData.offsetPoints(listOf(bh, bare), listOf(lift))
        assertEquals(listOf(ChartData.SOURCE_BARE, ChartData.SOURCE_BROADHEAD, ChartData.SOURCE_LIFT), pts.map { it.source })
        assertEquals(-0.5, pts[0].x, 1e-9)
        assertEquals(1.0, pts[0].y, 1e-9)
        assertEquals(2.0, pts[1].x, 1e-9)
        assertEquals(0.25, pts[2].x, 1e-9)
        assertEquals(-0.5, pts[2].y, 1e-9)
    }

    @Test
    fun sessionKindCountsCoverEveryKind() {
        val counts = ChartData.sessionKindCounts(listOf(session(1), session(2), session(3, kind = SessionKind.PAPER)))
        assertEquals(2, counts[SessionKind.GROUP])
        assertEquals(1, counts[SessionKind.PAPER])
        assertEquals(SessionKind.all.toSet(), counts.keys)
    }
}
