package com.strobingn.bowtune.data

enum class CoachingLevel(val label: String, val blurb: String) {
    BEGINNER(
        "Beginner",
        "Extra why-it-matters notes, slower order of attack, fewer brand jargon terms."
    ),
    STANDARD(
        "Standard",
        "Easton compound-release steps plus Mathews Limb Shift chase-the-tear notes."
    ),
    PRO(
        "Pro",
        "Shorter cues, clearance and spine nuance, walk-back after paper is close."
    );

    companion object {
        fun fromStorage(raw: String?): CoachingLevel =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}
