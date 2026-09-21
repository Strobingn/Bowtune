package com.strobingn.bowtune.data

import java.text.DateFormat
import java.util.Date

object ReportBuilder {
    fun build(
        active: BowSetup?,
        sessions: List<TuneSession>,
        tears: List<PaperTearLog>,
        adjustments: List<AdjustmentLog>,
        arrows: List<ArrowShaft>,
        maintenance: List<MaintenanceLog>,
        checklistPercent: Int,
        checklistName: String
    ): String {
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return buildString {
            appendLine("Bow Tune report")
            appendLine("Generated ${df.format(Date())}")
            appendLine()
            appendLine("Active setup")
            if (active == null) {
                appendLine("  (none selected)")
            } else {
                appendLine("  ${active.name} — ${active.brand} ${active.model}".trim())
                appendLine("  ${active.drawWeightLbs} lb · ${active.drawLengthIn}\" DL · rest ${active.rest}")
                if (active.limbShiftSetting.isNotBlank()) appendLine("  Limb Shift: ${active.limbShiftSetting}")
                if (active.nextTuneDueEpochMs != null) {
                    appendLine("  Next tune due: ${df.format(Date(active.nextTuneDueEpochMs))}")
                }
            }
            appendLine()
            appendLine("Checklist: $checklistName — $checklistPercent% complete")
            appendLine()
            appendLine("Recent paper tears")
            if (tears.isEmpty()) appendLine("  (none)")
            tears.take(8).forEach { t ->
                appendLine("  ${df.format(Date(t.dateEpochMs))} · ${t.tearType} · ${t.setupName} · ${t.distanceFt} ft")
                if (t.notes.isNotBlank()) appendLine("    ${t.notes}")
            }
            appendLine()
            appendLine("Recent sessions")
            if (sessions.isEmpty()) appendLine("  (none)")
            sessions.take(10).forEach { s ->
                appendLine(
                    "  ${df.format(Date(s.dateEpochMs))} · ${SessionKind.label(s.sessionKind)} · " +
                        "${s.distanceYd} yd · ${s.setupName} · ${s.scoreOrGroup}"
                )
                val cond = listOf(s.indoorOutdoor, s.windNote, s.tempNote).filter { it.isNotBlank() }
                if (cond.isNotEmpty()) appendLine("    Conditions: ${cond.joinToString(" · ")}")
                if (s.notes.isNotBlank()) appendLine("    ${s.notes}")
            }
            appendLine()
            appendLine("Adjustments")
            if (adjustments.isEmpty()) appendLine("  (none)")
            adjustments.take(8).forEach { a ->
                appendLine(
                    "  ${df.format(Date(a.dateEpochMs))} · ${AdjustmentLog.label(a.kind)} · " +
                        "${a.setupName}: ${a.beforeValue} → ${a.afterValue}"
                )
            }
            appendLine()
            appendLine("Arrows")
            if (arrows.isEmpty()) appendLine("  (none)")
            arrows.forEach { a ->
                appendLine("  ${a.name} · spine ${a.spine} · ${a.lengthIn}\" · tip ${a.pointWeightGr} gr · qty ${a.quantity}")
            }
            appendLine()
            appendLine("Maintenance")
            if (maintenance.isEmpty()) appendLine("  (none)")
            maintenance.take(8).forEach { m ->
                appendLine("  ${df.format(Date(m.dateEpochMs))} · ${MaintenanceLog.label(m.kind)} · ${m.setupName}")
                if (m.notes.isNotBlank()) appendLine("    ${m.notes}")
            }
            appendLine()
            appendLine("Educational only — follow your bow manual. One change at a time.")
        }
    }
}
