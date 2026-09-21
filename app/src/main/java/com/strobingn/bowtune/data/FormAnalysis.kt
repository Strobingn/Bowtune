package com.strobingn.bowtune.data

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
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

data class FormAnalysisResult(
    val tips: List<CoachingTip>,
    val confidenceNote: String?,
    val landmarkCount: Int,
    val averageLikelihood: Float
) {
    val isLowConfidence: Boolean
        get() = landmarkCount < 8 || averageLikelihood < 0.55f
}

/**
 * On-device archery form heuristics from ML Kit pose landmarks.
 * Assumes roughly side-on or 3/4 view at full-draw-ish stance.
 * Honest about low confidence — not a substitute for a coach.
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

        // Shoulder line tilt (degrees from horizontal)
        if (ok(lShoulder) && ok(rShoulder)) {
            val dy = rShoulder!!.position.y - lShoulder!!.position.y
            val dx = rShoulder.position.x - lShoulder.position.x
            val tiltDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val absTilt = abs(tiltDeg)
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
                    detail = "Shoulders look roughly level. Keep that consistent shot to shot.",
                    severity = CoachingTip.Severity.INFO
                )
            }

            // Front / bow shoulder high-low vs drawing side (RH: left = bow)
            val bowShoulder = if (assumeRightHanded) lShoulder else rShoulder
            val drawShoulder = if (assumeRightHanded) rShoulder else lShoulder
            val shoulderDeltaY = bowShoulder.position.y - drawShoulder.position.y
            // In image coords, smaller Y is higher on screen
            if (shoulderDeltaY < -12f) {
                tips += CoachingTip(
                    title = "Front shoulder may be high",
                    detail = "Bow-side shoulder sits higher than the drawing shoulder. Drop the front shoulder and push the bow hand toward the target without shrugging.",
                    severity = CoachingTip.Severity.CAUTION
                )
            } else if (shoulderDeltaY > 18f) {
                tips += CoachingTip(
                    title = "Front shoulder looks low / collapsed",
                    detail = "Bow shoulder dropped a lot vs draw side. Avoid collapsing into the riser — maintain bone alignment through the bow arm.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // Drawing-arm elbow angle
        val dShoulder = if (assumeRightHanded) rShoulder else lShoulder
        val dElbow = if (assumeRightHanded) rElbow else lElbow
        val dWrist = if (assumeRightHanded) rWrist else lWrist
        if (ok(dShoulder) && ok(dElbow) && ok(dWrist)) {
            val angle = angleAt(dShoulder!!, dElbow!!, dWrist!!)
            when {
                angle < 70f -> tips += CoachingTip(
                    title = "Drawing elbow looks collapsed",
                    detail = "Elbow angle ~${angle.roundToInt()}°. At full draw the drawing elbow usually sits higher/back with a more open angle. Check draw length and anchor.",
                    severity = CoachingTip.Severity.CAUTION
                )
                angle in 70f..100f -> tips += CoachingTip(
                    title = "Drawing arm mid-range",
                    detail = "Elbow ~${angle.roundToInt()}°. If this is full draw, confirm anchor is consistent (corner of mouth / peep / kisser) before blaming the bow.",
                    severity = CoachingTip.Severity.INFO
                )
                angle > 155f -> tips += CoachingTip(
                    title = "Drawing arm nearly straight",
                    detail = "Very open elbow (~${angle.roundToInt()}°). May be under-drawn or camera angle odd. Confirm you are at wall / full draw.",
                    severity = CoachingTip.Severity.INFO
                )
                else -> tips += CoachingTip(
                    title = "Drawing elbow in a usable range",
                    detail = "Elbow ~${angle.roundToInt()}°. Focus on repeating the same anchor each shot.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // Bow arm (front) elbow — prefer slight bend, not locked hard or collapsed
        val bShoulder = if (assumeRightHanded) lShoulder else rShoulder
        val bElbow = if (assumeRightHanded) lElbow else rElbow
        val bWrist = if (assumeRightHanded) lWrist else rWrist
        if (ok(bShoulder) && ok(bElbow) && ok(bWrist)) {
            val bowAngle = angleAt(bShoulder!!, bElbow!!, bWrist!!)
            when {
                bowAngle > 175f -> tips += CoachingTip(
                    title = "Bow arm looks hyper-extended",
                    detail = "Nearly locked straight (~${bowAngle.roundToInt()}°). A soft elbow helps absorption and can reduce torque — avoid hyperextension into the joint.",
                    severity = CoachingTip.Severity.CAUTION
                )
                bowAngle < 140f -> tips += CoachingTip(
                    title = "Bow arm looks bent",
                    detail = "Elbow ~${bowAngle.roundToInt()}°. Too much bend can pull the string into the arm or change centershot feel. Push toward the target with a firmer bow arm.",
                    severity = CoachingTip.Severity.INFO
                )
                else -> tips += CoachingTip(
                    title = "Bow arm alignment",
                    detail = "Bow elbow ~${bowAngle.roundToInt()}°. Keep pressure through the bow hand without death-gripping the riser (grip torque causes false paper tears).",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // Head vs shoulder midpoint
        if (ok(nose) && ok(lShoulder) && ok(rShoulder)) {
            val midX = (lShoulder!!.position.x + rShoulder!!.position.x) / 2f
            val midY = (lShoulder.position.y + rShoulder.position.y) / 2f
            val headOffsetX = nose!!.position.x - midX
            val headDrop = nose.position.y - midY
            if (abs(headOffsetX) > 55f) {
                tips += CoachingTip(
                    title = "Head seems shifted off center",
                    detail = "Nose is offset from the shoulder midline. Avoid peeking or tilting the head into the peep — settle the peep to your eye, not the other way around.",
                    severity = CoachingTip.Severity.CAUTION
                )
            }
            if (headDrop > 90f) {
                tips += CoachingTip(
                    title = "Head looks dropped",
                    detail = "Chin/head low relative to shoulders. Check peep height and whether you are collapsing at anchor.",
                    severity = CoachingTip.Severity.INFO
                )
            }
        }

        // Leaning: shoulder midpoint vs hip midpoint
        if (ok(lShoulder) && ok(rShoulder) && ok(lHip) && ok(rHip)) {
            val shoulderMidX = (lShoulder!!.position.x + rShoulder!!.position.x) / 2f
            val hipMidX = (lHip!!.position.x + rHip!!.position.x) / 2f
            val lean = shoulderMidX - hipMidX
            if (abs(lean) > 40f) {
                tips += CoachingTip(
                    title = "Possible lean",
                    detail = "Torso midline shifts vs hips. Common when fighting draw weight or reaching for the peep. Stand tall, weight balanced, then draw.",
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

        return FormAnalysisResult(leveled, confidenceNote, landmarks.size, avgLike)
    }

    private fun ok(lm: PoseLandmark?): Boolean =
        lm != null && lm.inFrameLikelihood >= 0.4f

    private fun angleAt(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Float {
        val bax = a.position.x - b.position.x
        val bay = a.position.y - b.position.y
        val bcx = c.position.x - b.position.x
        val bcy = c.position.y - b.position.y
        val dot = bax * bcx + bay * bcy
        val mag = hypot(bax.toDouble(), bay.toDouble()) * hypot(bcx.toDouble(), bcy.toDouble())
        if (mag < 1e-3) return 180f
        val cos = (dot / mag).toFloat().coerceIn(-1f, 1f)
        return Math.toDegrees(kotlin.math.acos(cos).toDouble()).toFloat()
    }
}
