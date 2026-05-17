package com.grama.sports.ui.screens.scoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KabaddiScoreScreen(navController: NavController, matchId: String, viewModel: AppViewModel) {
    val score by viewModel.currentLiveScore.collectAsState()

    LaunchedEffect(matchId) { viewModel.observeLiveScore(matchId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kabaddi Scorer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Timers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Match Time", style = MaterialTheme.typography.labelMedium)
                    Text(score.timer, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                
                // Raid Timer (30s)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            if (score.raidTimer <= 5) Color.Red else MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        score.raidTimer.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (score.raidTimer <= 5) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Half", style = MaterialTheme.typography.labelMedium)
                    Text("${score.currentHalf}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.toggleTimer(matchId) }) { 
                    Text(if (score.isTimerRunning) "Stop Timer" else "Start Timer") 
                }
                OutlinedButton(onClick = { viewModel.resetTimer(matchId) }) { Text("Reset") }
                Button(onClick = { viewModel.switchKabaddiHalf() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                    Text("Switch Half")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Raiding Team Indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "RAIDING: ${if (score.raidingTeam == "A") score.teamAName else score.teamBName}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Team A
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.teamAName, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(score.teamAScore.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text("On Field: ${score.teamAOnFieldCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Out Queue: ${score.teamAOutQueue.size}", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Controls for Team A
                    if (score.raidingTeam == "A") {
                        Text("Raid Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row {
                            (1..3).forEach { pts ->
                                Button(
                                    onClick = { viewModel.updateKabaddiScore(true, pts, 0, 0, false) },
                                    modifier = Modifier.weight(1f).padding(1.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+$pts") }
                            }
                        }
                        Button(onClick = { viewModel.updateKabaddiScore(true, 0, 0, 1, false) }, modifier = Modifier.fillMaxWidth()) { Text("Bonus +1") }
                        OutlinedButton(onClick = { viewModel.updateKabaddiScore(true, 0, 0, 0, false) }, modifier = Modifier.fillMaxWidth()) { Text("Empty Raid") }
                        
                        Button(
                            onClick = { viewModel.handleKabaddiAction(true, "Raider Out") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Raider Caught") }
                    } else {
                        // Team A is Defending - Defensive point is scored by "Raider Caught" in Team B's section
                        Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.Center) {
                            Text("Defending", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.handleKabaddiAction(true, "Out of Bounds") }, modifier = Modifier.fillMaxWidth()) { 
                        Text("A Steps Out", fontSize = 10.sp) 
                    }
                    OutlinedButton(onClick = { viewModel.updateKabaddiScore(true, 0, 0, 0, true) }, modifier = Modifier.fillMaxWidth()) { Text("All Out (+2)") }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Team B
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.teamBName, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(score.teamBScore.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text("On Field: ${score.teamBOnFieldCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Out Queue: ${score.teamBOutQueue.size}", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (score.raidingTeam == "B") {
                        Text("Raid Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row {
                            (1..3).forEach { pts ->
                                Button(
                                    onClick = { viewModel.updateKabaddiScore(false, pts, 0, 0, false) },
                                    modifier = Modifier.weight(1f).padding(1.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+$pts") }
                            }
                        }
                        Button(onClick = { viewModel.updateKabaddiScore(false, 0, 0, 1, false) }, modifier = Modifier.fillMaxWidth()) { Text("Bonus +1") }
                        OutlinedButton(onClick = { viewModel.updateKabaddiScore(false, 0, 0, 0, false) }, modifier = Modifier.fillMaxWidth()) { Text("Empty Raid") }
                        
                        Button(
                            onClick = { viewModel.handleKabaddiAction(false, "Raider Out") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Raider Caught") }
                    } else {
                        Box(modifier = Modifier.height(180.dp), contentAlignment = Alignment.Center) {
                            Text("Defending", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.handleKabaddiAction(false, "Out of Bounds") }, modifier = Modifier.fillMaxWidth()) { 
                        Text("B Steps Out", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { viewModel.updateKabaddiScore(false, 0, 0, 0, true) }, modifier = Modifier.fillMaxWidth()) { Text("All Out (+2)") }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (score.statusText.isNotEmpty()) {
                Text(
                    text = score.statusText,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.undoScore() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Undo Last Action") }
        }
    }
}
