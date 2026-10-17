package com.strobingn.bowtune.data

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class CoachingTip(
    val title: String,
    val detail: String,
    val severity: Severity = Severity.INFO
) {
    enum class Severity { INFO, CAUTION, LOW_CONFIDENCE }
}

data class FormMetricsSnapshot(
    val shoulderTiltDeg: Float? = null,
    val drawElbowDeg: Float? = null,
    val bowElbowDeg: Float? = null,
    val torsoLeanDeg: Float? = null
)

data class FormAnalysisResult(
    val tips: List<CoachingTip>,
    val confidenceNote: String?,
    val landmarkCount: Int,
    val averageLikelihood: Float,
    val metrics: FormMetricsSnapshot = FormMetricsSnapshot()
) {
    val isLowConfidence: Boolean
        get() = landmarkCount < 8 || averageLikelihood < 0.55f
}

/**
 * On-device archery form heuristics from ML Kit pose landmarks.
 * Assumes roughly side-on or 3/4 view at full-draw-ish stance.
 * All geometric checks are scale-normalized (using torso length or shoulder span)
 * and orientation-safe (handles archer facing left or right).
 */
object FormAnalysis {

    fun analyze(
        pose: Pose,
        assumeRightHanded: Boolean = true,
        level: CoachingLevel = CoachingLevel.STANDARD
    ): FormAnalysisResult {
        val landmarks = pose.allPoseLandmarks.filter { it.inFrameLikelihood >= 0.3f }
        val avgLike = if (landmarks.isEmpty()) 0f else landmarks.map { it.inFrameLikelihood }.average().toFloat()

        val tips = mutableListOf<CoachingTip>()
        var confidenceNote: String? = null

        if (landmarks.size < 8 || avgLike < 0.55f) {
            confidenceNote =
                "Low pose confidence (${landmarks.size} landmarks, ${(avgLike * 100).roundToInt()}% avg). " +
                    "Use decent lighting and a side-ish angle at full draw for better tips."
            tips += CoachingTip(
                title = "Hard to read stance",
                detail = "Step back so shoulders and both arms are visible. Avoid backlighting. Hold full draw briefly.",
                severity = CoachingTip.Severity.LOW_CONFIDENCE
            )
            return FormAnalysisResult(tips, confidenceNote, landmarks.size, avgLike)
        }

        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        var measuredShoulderTilt: Float? = null
        var measuredDrawElbow: Float? = null
        var measuredBowElbow: Float? = null
        var measuredTorsoLean: Float? = null

        val hasShoulders = ok(lShoulder) && ok(rShoulder)
        val hasHips = ok(lHip) && ok(rHip)

        // Compute scale-invariant reference length (torso height if hips visible, else shoulder span)
        val torsoHeight = if (hasShoulders && hasHips) {
            val sMidY = (lShoulder!!.position.y + rShoulder!!.position.y) / 2f
            val hMidY = (lHip!!.position.y + rHip!!.position.y) / 2f
            abs(hMidY - sMidY).coerceAtLeast(40f)
        } else {
            null
        }
        val shoulderSpan = if (hasShoulders) {
            hypot(
                (rShoulder!!.position.x - lShoulder!!.position.x).toDouble(),
                (rShoulder.position.y - lShoulder.position.y).toDouble()
            ).toFloat().coerceAtLeast(30f)
        } else {
            100f
        }
        val refScale = torsoHeight ?: shoulderSpan

        // 1. Shoulder line tilt (degrees from horizontal, independent of left/right facing direction)
        if (hasShoulders) {
            val dy = rShoulder!!.position.y - lShoulder!!.position.y
            val dx = rShoulder.position.x - lShoulder.position.x
            // Use abs(dx) so facing left or right both measure 0° when horizontal.
            // Guard against pure profile overlap (very small abs(dx)) by using max(abs(dx), 0.35 * refScale).
            val effectiveHorizSpan = abs(dx).coerceAtLeast(refScale * 0.35f)
            val absTilt = Math.toDegrees(atan2(abs(dy).toDouble(), effectiveHorizSpan.toDouble())).toFloat()
            measuredShoulderTilt = absTilt

            when {
                absTilt > 18f -> tips += CoachingTip(
                    title = "Shoulder line looks tilted",
                    detail = "Shoulders ~${absTilt.roundToInt()}° off level. Check for leaning into the shot or a high front shoulder. Level the shoulders and settle before release.",
                    severity = CoachingTip.Severity.CAUTION
                )
                absTilt > 10f -> tips += CoachingTip(
                    title = "Mild shoulder tilt",
                    detail = "Slight tilt (~${absTilt.roundToInt()}°). Common when the bow shoulder climbs. Soften the bow arm and keep the front shoulder down.",
                    severity = CoachingTip.Severity.INFO
                )
                else -> tips += CoachingTip(
                    title = "Shoulder line OK",
                    detail = "Shoulders look level (~${absTilt.roundToInt()}°). Keep that consistent shot to shot.",
                    severity = CoachingTip.Severity.INFO
                )
            }

            // Front / bow shoulder high-low vs drawing side (RH: left = bow)
            val bowShoulder = if (assumeRightHanded) lShoulder else rShoulder
            val drawShoulder = if (assumeRightHanded) rShoulder else lShoulder
            val shoulderDeltaY = bowShoulder.position.y - drawShoulder.position.y
            // In image coords, smaller Y is higher on screen
            if (shoulderDeltaY < -0.09f * refScale) {
                tips += CoachingTip(
                    title = "Front shoulder may be high",
                    detail = "Bow-side shoulder sits higher than the drawing shoulder. Drop the front shoulder and push the bow hand toward the target without shrugging.",
                    severity = CoachingTip.Severity.CAUTION
                )
            } else if (shoulderDeltaY > 0.14f * refScale) {
                tips += CoachingTip(
                    title = "Front shoulder looks low / collapsed",
                    detail = "Bow shoulder dropped vs draw side. Avoid collapsing into the riser — maintain bone alignment through the bow arm.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // 2. Drawing-arm elbow angle
        val dShoulder = if (assumeRightHanded) rShoulder else lShoulder
        val dElbow = if (assumeRightHanded) rElbow else lElbow
        val dWrist = if (assumeRightHanded) rWrist else lWrist
        if (ok(dShoulder) && ok(dElbow) && ok(dWrist)) {
            val angle = angleAt(dShoulder!!, dElbow!!, dWrist!!)
            measuredDrawElbow = angle
            when {
                angle < 70f -> tips += CoachingTip(
                    title = "Drawing elbow looks collapsed",
                    detail = "Elbow angle ~${angle.roundToInt()}°. At full draw the drawing elbow usually sits in line with the arrow axis. Check draw length and back tension.",
                    severity = CoachingTip.Severity.CAUTION
                )
                angle in 70f..100f -> tips += CoachingTip(
                    title = "Drawing arm mid-range",
                    detail = "Elbow ~${angle.roundToInt()}°. If this is full draw, confirm anchor is consistent (jaw / corner of mouth / peep / kisser) before blaming the bow.",
                    severity = CoachingTip.Severity.INFO
                )
                angle > 155f -> tips += CoachingTip(
                    title = "Drawing arm nearly straight",
                    detail = "Very open elbow (~${angle.roundToInt()}°). May be under-drawn or camera angle odd. Confirm you are at the back wall / full draw.",
                    severity = CoachingTip.Severity.INFO
                )
                else -> tips += CoachingTip(
                    title = "Drawing elbow in a usable range",
                    detail = "Elbow ~${angle.roundToInt()}°. Focus on repeating the same anchor each shot.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // 3. Bow arm (front) elbow — prefer slight bend / aligned bone column, not hyper-extended or collapsed
        val bShoulder = if (assumeRightHanded) lShoulder else rShoulder
        val bElbow = if (assumeRightHanded) lElbow else rElbow
        val bWrist = if (assumeRightHanded) lWrist else rWrist
        if (ok(bShoulder) && ok(bElbow) && ok(bWrist)) {
            val bowAngle = angleAt(bShoulder!!, bElbow!!, bWrist!!)
            measuredBowElbow = bowAngle
            when {
                bowAngle > 175f -> tips += CoachingTip(
                    title = "Bow arm looks hyper-extended",
                    detail = "Nearly locked straight (~${bowAngle.roundToInt()}°). Rotate the elbow slightly out of the string path to avoid slap and reduce riser torque.",
                    severity = CoachingTip.Severity.CAUTION
                )
                bowAngle < 140f -> tips += CoachingTip(
                    title = "Bow arm looks bent",
                    detail = "Elbow ~${bowAngle.roundToInt()}°. Too much bend forces muscle holding and changes centershot feel. Push toward the target with skeletal alignment.",
                    severity = CoachingTip.Severity.INFO
                )
                else -> tips += CoachingTip(
                    title = "Bow arm alignment",
                    detail = "Bow elbow ~${bowAngle.roundToInt()}°. Keep pressure through the bow hand V-lifeline without death-gripping the riser (grip torque causes false paper tears).",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // 4. Head vs shoulder midpoint (scale-normalized; in image coords, smaller Y is higher on screen)
        if (ok(nose) && hasShoulders) {
            val midX = (lShoulder!!.position.x + rShoulder!!.position.x) / 2f
            val midY = (lShoulder.position.y + rShoulder.position.y) / 2f
            val headOffsetX = abs(nose!!.position.x - midX)
            // Positive headElevation means nose is above shoulder line (normal upright stance)
            val headElevation = midY - nose.position.y
            if (headOffsetX > 0.42f * refScale) {
                tips += CoachingTip(
                    title = "Head seems shifted off center",
                    detail = "Nose is offset forward/back from the shoulder midline. Avoid tipping the head forward to meet the string — bring the string and peep to your face.",
                    severity = CoachingTip.Severity.CAUTION
                )
            }
            if (headElevation < 0.15f * refScale) {
                tips += CoachingTip(
                    title = "Head looks dropped",
                    detail = "Chin/head sits low relative to the shoulder line. Check peep height so you aren't burying your chin into your chest at anchor.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // 5. Torso lean: shoulder midpoint vs hip midpoint angle from vertical
        if (hasShoulders && hasHips) {
            val shoulderMidX = (lShoulder!!.position.x + rShoulder!!.position.x) / 2f
            val shoulderMidY = (lShoulder.position.y + rShoulder.position.y) / 2f
            val hipMidX = (lHip!!.position.x + rHip!!.position.x) / 2f
            val hipMidY = (lHip.position.y + rHip.position.y) / 2f
            val dyTorso = abs(hipMidY - shoulderMidY).coerceAtLeast(1f)
            val dxTorso = abs(shoulderMidX - hipMidX)
            val leanDeg = Math.toDegrees(atan2(dxTorso.toDouble(), dyTorso.toDouble())).toFloat()
            measuredTorsoLean = leanDeg
            if (leanDeg > 9f) {
                tips += CoachingTip(
                    title = "Possible torso lean",
                    detail = "Torso leans ~${leanDeg.roundToInt()}° off vertical vs hips. Common when fighting draw weight or leaning away from the bow. Stand tall with weight balanced 50/50.",
                    severity = CoachingTip.Severity.CAUTION
                )
            }
        }

        // Always remind about grip / form vs hardware
        tips += CoachingTip(
            title = "Form before hardware",
            detail = "Grip torque, bow-arm tension, and inconsistent anchor invent left/right tears. Fix form cues here, then re-paper before chasing rest/yoke.",
            severity = CoachingTip.Severity.INFO
        )

        if (tips.none { it.severity == CoachingTip.Severity.CAUTION }) {
            confidenceNote = "Pose looks usable. Tips are heuristics — verify with a coach or video from a consistent angle."
        }

        val leveled = when (level) {
            CoachingLevel.BEGINNER -> {
                tips += CoachingTip(
                    title = "Slow the shot",
                    detail = "Settle at full draw, check the bubble, then execute. Hardware changes cannot fix a rushed shot.",
                    severity = CoachingTip.Severity.INFO
                )
                tips
            }
            CoachingLevel.STANDARD -> tips
            CoachingLevel.PRO -> tips.filter {
                it.severity != CoachingTip.Severity.INFO || it.title.contains("alignment", ignoreCase = true)
            }.ifEmpty { tips.take(2) }
        }

        return FormAnalysisResult(
            tips = leveled,
            confidenceNote = confidenceNote,
            landmarkCount = landmarks.size,
            averageLikelihood = avgLike,
            metrics = FormMetricsSnapshot(
                shoulderTiltDeg = measuredShoulderTilt,
                drawElbowDeg = measuredDrawElbow,
                bowElbowDeg = measuredBowElbow,
                torsoLeanDeg = measuredTorsoLean
            )
        )
    }

    private fun ok(lm: PoseLandmark?): Boolean =
        lm != null && lm.inFrameLikelihood >= 0.4f

    internal fun angleAt(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Float {
        val bax = a.position.x - b.position.x
        val bay = a.position.y - b.position.y
        val bcx = c.position.x - b.position.x
        val bcy = c.position.y - b.position.y
        val dot = bax * bcx + bay * bcy
        val mag = hypot(bax.toDouble(), bay.toDouble()) * hypot(bcx.toDouble(), bcy.toDouble())
        if (mag < 1e-3) return 180f
        val cos = (dot / mag).toFloat().coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cos.toDouble())).toFloat()
    }
}
