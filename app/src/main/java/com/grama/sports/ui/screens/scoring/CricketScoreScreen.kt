package com.grama.sports.ui.screens.scoring

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.viewmodel.AppViewModel

/**
 * CricketScoreScreen — FIXED.
 * Previously used old ScoreViewModel which wrote to liveScores/cricket/{matchId} (wrong path).
 * Now uses AppViewModel which writes to unified liveScores/{matchId} path.
 * This ensures fan screens receive the updates instantly via Firebase realtime listeners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketScoreScreen(navController: NavController, matchId: String, viewModel: AppViewModel) {
    val score by viewModel.currentLiveScore.collectAsState()

    LaunchedEffect(matchId) { viewModel.observeLiveScore(matchId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cricket Scorer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val teamName = if (score.currentInnings == 1) score.teamAName else score.teamBName
            
            // Team Name Display
            Text(teamName.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Innings ${score.currentInnings}", style = MaterialTheme.typography.labelLarge)
            
            val teamScore = if (score.currentInnings == 1) score.teamAScore else score.teamBScore
            val teamWickets = if (score.currentInnings == 1) score.teamAWickets else score.teamBWickets
            val teamOvers = if (score.currentInnings == 1) score.teamAOvers else score.teamBOvers
            
            Text("$teamScore/$teamWickets", fontSize = 56.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("Overs: $teamOvers", fontSize = 20.sp)

            if (score.currentInnings == 2 && score.targetScore > 0) {
                val needed = score.targetScore - score.teamBScore
                Text("Need $needed runs to win", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Run Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 2, 3).forEach { r ->
                    Button(onClick = { viewModel.updateCricketScore(r) }, modifier = Modifier.weight(1f).height(60.dp)) { Text("$r") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.updateCricketScore(4) }, modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("4", fontWeight = FontWeight.Bold) }
                Button(onClick = { viewModel.updateCricketScore(6) }, modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("6", fontWeight = FontWeight.Bold) }
                Button(onClick = { viewModel.updateCricketScore(0, extraType = "Wide") }, modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))) { Text("WD") }
                Button(onClick = { viewModel.updateCricketScore(0, extraType = "No Ball") }, modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))) { Text("NB") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.updateCricketScore(1, extraType = "Bye") }, modifier = Modifier.weight(1f).height(60.dp)) { Text("BYE") }
                Button(onClick = { viewModel.updateCricketScore(1, extraType = "Leg Bye") }, modifier = Modifier.weight(1f).height(60.dp)) { Text("LB") }
                Button(
                    onClick = { viewModel.updateCricketScore(0, isWicket = true) },
                    modifier = Modifier.weight(2f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("WICKET", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (score.currentInnings == 1) {
                OutlinedButton(onClick = { viewModel.switchInnings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("End Innings & Set Target")
                }
            }
        }
    }
}
