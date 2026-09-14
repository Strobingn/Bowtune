package com.strobingn.bowtune.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object PaperTear : TopLevelDestination("paper_tear", "Paper Tear", Icons.Filled.GpsFixed)
    data object Checklist : TopLevelDestination("checklist", "Checklist", Icons.Filled.Checklist)
    data object Gear : TopLevelDestination("gear", "Gear", Icons.Filled.Build)
    data object Sessions : TopLevelDestination("sessions", "Sessions", Icons.Filled.SportsScore)
    data object Guides : TopLevelDestination("guides", "Guides", Icons.Filled.MenuBook)

    companion object {
        val all = listOf(PaperTear, Checklist, Gear, Sessions, Guides)
    }
}

object GuideRoutes {
    const val LIST = "guides"
    const val DETAIL = "guides/{guideId}"
    fun detail(guideId: String) = "guides/$guideId"
}
