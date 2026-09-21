package com.strobingn.bowtune.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : TopLevelDestination("home", "Home", Icons.Filled.Home)
    data object PaperTear : TopLevelDestination("paper_tear", "Tear", Icons.Filled.GpsFixed)
    data object Checklist : TopLevelDestination("checklist", "List", Icons.Filled.Checklist)
    data object Gear : TopLevelDestination("gear", "Gear", Icons.Filled.Build)
    data object Sessions : TopLevelDestination("sessions", "Logs", Icons.Filled.SportsScore)
    data object Vision : TopLevelDestination("vision", "Vision", Icons.Filled.CameraAlt)
    data object Guides : TopLevelDestination("guides", "Guides", Icons.Filled.MenuBook)

    companion object {
        val all = listOf(Home, PaperTear, Checklist, Gear, Sessions, Vision, Guides)
    }
}

object GuideRoutes {
    const val LIST = "guides"
    const val DETAIL = "guides/{guideId}"
    fun detail(guideId: String) = "guides/$guideId"
}

object SessionRoutes {
    const val LIST = "sessions"
    const val LIFT_VERTICAL = "sessions/lift_vertical"
    const val WIZARD = "sessions/wizard/{wizardId}"
    fun wizard(wizardId: String) = "sessions/wizard/$wizardId"
}

object PaperTearRoutes {
    const val ROOT = "paper_tear"
    const val WITH_TEAR = "paper_tear?tear={tear}"
    fun withTear(tear: String) = "paper_tear?tear=$tear"
}
