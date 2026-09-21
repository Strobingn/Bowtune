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
import com.strobingn.bowtune.data.TearType
import com.strobingn.bowtune.ui.screens.checklist.ChecklistScreen
import com.strobingn.bowtune.ui.screens.gear.GearScreen
import com.strobingn.bowtune.ui.screens.guides.AdvancedTuneDetailScreen
import com.strobingn.bowtune.ui.screens.guides.AdvancedTuneListScreen
import com.strobingn.bowtune.ui.screens.guides.AdvancedTuneWalkthroughScreen
import com.strobingn.bowtune.ui.screens.guides.GuideDetailScreen
import com.strobingn.bowtune.ui.screens.guides.GuidesListScreen
import com.strobingn.bowtune.ui.screens.home.HomeScreen
import com.strobingn.bowtune.ui.screens.papertear.PaperTearScreen
import com.strobingn.bowtune.ui.screens.sessions.BrandWizardScreen
import com.strobingn.bowtune.ui.screens.sessions.LiftVerticalTuneSessionScreen
import com.strobingn.bowtune.ui.screens.sessions.SessionsScreen
import com.strobingn.bowtune.ui.screens.vision.ShotVisionScreen

@Composable
fun BowTuneBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val current = navBackStackEntry?.destination

    NavigationBar {
        TopLevelDestination.all.forEach { dest ->
            val selected = current?.hierarchy?.any { route ->
                route.route?.substringBefore("?") == dest.route
            } == true ||
                (dest == TopLevelDestination.Guides && (
                    current?.route?.startsWith("guides") == true ||
                        current?.route?.startsWith("advanced") == true
                    )) ||
                (dest == TopLevelDestination.Sessions && current?.route?.startsWith("sessions") == true)
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
        startDestination = TopLevelDestination.Home.route,
        modifier = modifier
    ) {
        composable(TopLevelDestination.Home.route) {
            HomeScreen(
                onOpenPaperTear = {
                    navController.navigate(TopLevelDestination.PaperTear.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenGear = {
                    navController.navigate(TopLevelDestination.Gear.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenChecklist = {
                    navController.navigate(TopLevelDestination.Checklist.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenSessions = {
                    navController.navigate(TopLevelDestination.Sessions.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenAdvancedLibrary = {
                    navController.navigate(AdvancedTuneRoutes.LIST)
                }
            )
        }
        composable(
            route = "paper_tear?tear={tear}",
            arguments = listOf(
                navArgument("tear") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            PaperTearScreen(
                initialTear = entry.arguments?.getString("tear"),
                onOpenAdvanced = { id -> navController.navigate(AdvancedTuneRoutes.detail(id)) }
            )
        }
        composable(TopLevelDestination.Checklist.route) { ChecklistScreen() }
        composable(TopLevelDestination.Gear.route) { GearScreen() }
        composable(SessionRoutes.LIST) {
            SessionsScreen(
                onOpenLiftVerticalTune = {
                    navController.navigate(SessionRoutes.LIFT_VERTICAL)
                },
                onOpenWizard = { id -> navController.navigate(SessionRoutes.wizard(id)) },
                onOpenAdvanced = { id -> navController.navigate(AdvancedTuneRoutes.detail(id)) },
                onOpenAdvancedLibrary = {
                    navController.navigate(AdvancedTuneRoutes.LIST)
                }
            )
        }
        composable(SessionRoutes.LIFT_VERTICAL) {
            LiftVerticalTuneSessionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = SessionRoutes.WIZARD,
            arguments = listOf(navArgument("wizardId") { type = NavType.StringType })
        ) { entry ->
            BrandWizardScreen(
                wizardId = entry.arguments?.getString("wizardId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(TopLevelDestination.Vision.route) {
            ShotVisionScreen(
                onOpenPaperTear = { tear: TearType ->
                    navController.navigate(PaperTearRoutes.withTear(tear.name)) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(GuideRoutes.LIST) {
            GuidesListScreen(
                onOpenGuide = { id -> navController.navigate(GuideRoutes.detail(id)) },
                onOpenWizard = { id -> navController.navigate(SessionRoutes.wizard(id)) },
                onOpenAdvancedLibrary = { navController.navigate(AdvancedTuneRoutes.LIST) },
                onOpenAdvancedGuide = { id -> navController.navigate(AdvancedTuneRoutes.detail(id)) }
            )
        }
        composable(
            route = GuideRoutes.DETAIL,
            arguments = listOf(navArgument("guideId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("guideId").orEmpty()
            GuideDetailScreen(
                guideId = id,
                onBack = { navController.popBackStack() },
                onOpenWizard = { wiz -> navController.navigate(SessionRoutes.wizard(wiz)) }
            )
        }
        composable(AdvancedTuneRoutes.LIST) {
            AdvancedTuneListScreen(
                onOpenGuide = { id -> navController.navigate(AdvancedTuneRoutes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AdvancedTuneRoutes.DETAIL,
            arguments = listOf(navArgument("guideId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("guideId").orEmpty()
            AdvancedTuneDetailScreen(
                guideId = id,
                onBack = { navController.popBackStack() },
                onOpenRelated = { related ->
                    navController.navigate(AdvancedTuneRoutes.detail(related))
                },
                onOpenWalkthrough = { g, s ->
                    navController.navigate(AdvancedTuneRoutes.walk(g, s))
                }
            )
        }
        composable(
            route = AdvancedTuneRoutes.WALK,
            arguments = listOf(
                navArgument("guideId") { type = NavType.StringType },
                navArgument("sectionId") { type = NavType.StringType }
            )
        ) { entry ->
            AdvancedTuneWalkthroughScreen(
                guideId = entry.arguments?.getString("guideId").orEmpty(),
                sectionId = entry.arguments?.getString("sectionId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
