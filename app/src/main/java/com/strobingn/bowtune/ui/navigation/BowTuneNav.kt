package com.strobingn.bowtune.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.strobingn.bowtune.ui.screens.checklist.ChecklistScreen
import com.strobingn.bowtune.ui.screens.gear.GearScreen
import com.strobingn.bowtune.ui.screens.guides.GuideDetailScreen
import com.strobingn.bowtune.ui.screens.guides.GuidesListScreen
import com.strobingn.bowtune.ui.screens.papertear.PaperTearScreen
import com.strobingn.bowtune.ui.screens.sessions.SessionsScreen

@Composable
fun BowTuneBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination

    NavigationBar {
        TopLevelDestination.all.forEach { dest ->
            val selected = current?.hierarchy?.any { it.route == dest.route } == true ||
                (dest == TopLevelDestination.Guides && current?.route?.startsWith("guides") == true)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = {
                    Text(
                        dest.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun BowTuneNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.PaperTear.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.PaperTear.route) { PaperTearScreen() }
        composable(TopLevelDestination.Checklist.route) { ChecklistScreen() }
        composable(TopLevelDestination.Gear.route) { GearScreen() }
        composable(TopLevelDestination.Sessions.route) { SessionsScreen() }
        composable(GuideRoutes.LIST) {
            GuidesListScreen(
                onOpenGuide = { id -> navController.navigate(GuideRoutes.detail(id)) }
            )
        }
        composable(
            route = GuideRoutes.DETAIL,
            arguments = listOf(navArgument("guideId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("guideId").orEmpty()
            GuideDetailScreen(
                guideId = id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
