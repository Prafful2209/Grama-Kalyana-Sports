package com.grama.sports.ui.screens.scoring

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.viewmodel.AppViewModel

/**
 * VolleyballScoreScreen — FIXED.
 * Previously used old ScoreViewModel with wrong path (liveScores/volleyball/{matchId}).
 * Now uses AppViewModel with unified path (liveScores/{matchId}).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolleyballScoreScreen(navController: NavController, matchId: String, viewModel: AppViewModel) {
    val score by viewModel.currentLiveScore.collectAsState()

    LaunchedEffect(matchId) { viewModel.observeLiveScore(matchId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volleyball Scorer") },
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
            Text("Set ${score.currentSet}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Sets: ${score.teamAName} ${score.teamASets} — ${score.teamBName} ${score.teamBSets}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(score.teamAName, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(score.teamAScore.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { viewModel.updateVolleyballScore(true) }) { Text("+1 Point") }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(score.teamBName, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(score.teamBScore.toString(), fontSize = 64.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary)
                    Button(onClick = { viewModel.updateVolleyballScore(false) }) { Text("+1 Point") }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.endVolleyballSet() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("End Set") }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.undoScore() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Undo Last Point") }
        }
    }
}
