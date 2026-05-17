package com.grama.sports.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.grama.sports.ui.screens.auth.*
import com.grama.sports.ui.screens.dashboard.*
import com.grama.sports.ui.screens.scoring.*
import com.grama.sports.ui.screens.fanview.*
import com.grama.sports.ui.screens.tournament.*
import com.grama.sports.ui.screens.players.*
import com.grama.sports.ui.screens.settings.*
import com.grama.sports.viewmodel.AppViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Activity-scoped shared ViewModel
    val viewModel: AppViewModel = viewModel()
    
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(navController, viewModel) }
        composable(Routes.REGISTER) { RegisterScreen(navController, viewModel) }
        composable(Routes.DASHBOARD) { DashboardScreen(navController, viewModel) }
        
        composable(Routes.TOURNAMENT_LIST) { TournamentListScreen(navController, viewModel) }
        composable(Routes.TOURNAMENT) { TournamentScreen(navController, viewModel) }
        composable(Routes.MATCH_SCHEDULER) { MatchSchedulerScreen(navController, viewModel) }
        composable(Routes.TEAM_MANAGEMENT) { TeamManagementScreen(navController, viewModel) }
        composable(Routes.PLAYER_MANAGEMENT) { PlayerManagementScreen(navController, viewModel) }

        composable(Routes.PLAYERS) { PlayerProfileScreen(navController, viewModel) }
        composable(Routes.SETTINGS) { SettingsScreen(navController, viewModel) }
        
        composable(
            route = Routes.LIVE_SCORE,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("sportType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val sportType = backStackEntry.arguments?.getString("sportType") ?: ""
            LiveScoreScreen(navController, matchId, sportType, viewModel)
        }

        composable(
            route = Routes.FAN_VIEW,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("sportType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            val sportType = backStackEntry.arguments?.getString("sportType") ?: ""
            FanViewScreen(navController, matchId, sportType, viewModel)
        }
    }
}
