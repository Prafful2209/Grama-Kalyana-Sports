@file:OptIn(ExperimentalMaterial3Api::class)

package com.grama.sports.ui.screens.scoring

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.models.*
import com.grama.sports.viewmodel.AppViewModel

@Composable
fun LiveScoreScreen(
    navController: NavController,
    matchId: String,
    sportType: String,
    viewModel: AppViewModel
) {
    val currentScore by viewModel.currentLiveScore.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current
    
    val liveMatches by viewModel.liveMatches.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val completedMatches by viewModel.completedMatches.collectAsState()
    
    // Search in all categories to ensure we have the match object
    val match = liveMatches.find { it.id == matchId } 
        ?: upcomingMatches.find { it.id == matchId }
        ?: completedMatches.find { it.id == matchId }

    LaunchedEffect(matchId) { viewModel.observeLiveScore(matchId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scorer Panel: $sportType", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { viewModel.undoScore() }) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.White)
                        }
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val text = "Live Update: $sportType Match!\n${currentScore.statusText}\nMatch ID: $matchId"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Scorecard"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAdmin) {
                val isKabaddi = sportType.contains("Kaba", ignoreCase = true)
                when {
                    sportType == "Cricket"    -> CricketScoringPanel(currentScore, viewModel, match)
                    sportType == "Volleyball" -> VolleyballScoringPanel(currentScore, viewModel, match)
                    isKabaddi                -> KabaddiScoringPanel(currentScore, viewModel, matchId, match)
                    else                      -> Text("Unsupported Sport: $sportType")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Access Denied", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Switch to Admin mode to update scores.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun CricketScoringPanel(score: LiveScore, viewModel: AppViewModel, match: Match?) {
    val isInnings2 = score.currentInnings == 2
    val isLive = match?.status?.lowercase() == "live"
    
    val currentTeamScore = if (score.currentInnings == 1) score.teamAScore else score.teamBScore
    val currentTeamWickets = if (score.currentInnings == 1) score.teamAWickets else score.teamBWickets
    val currentTeamOvers = if (score.currentInnings == 1) score.teamAOvers else score.teamBOvers
    val currentTeamName = if (score.currentInnings == 1) score.teamAName else score.teamBName
    
    val allPlayers by viewModel.players.collectAsState()
    
    val teamAPlayingXI = if (match?.teamAPlayerIds?.isNotEmpty() == true) {
        allPlayers.filter { it.id in match.teamAPlayerIds }
    } else allPlayers.filter { it.teamId == match?.teamAId || it.teamName == match?.teamAName }

    val teamBPlayingXI = if (match?.teamBPlayerIds?.isNotEmpty() == true) {
        allPlayers.filter { it.id in match.teamBPlayerIds }
    } else allPlayers.filter { it.teamId == match?.teamBId || it.teamName == match?.teamBName }

    val battingPlayers = if (score.currentInnings == 1) teamAPlayingXI else teamBPlayingXI
    val bowlingPlayers = if (score.currentInnings == 1) teamBPlayingXI else teamAPlayingXI

    var showPlayerSelection by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    com.grama.sports.ui.theme.GradientDarkStart,
                    com.grama.sports.ui.theme.GradientDarkEnd
                )
            )
        )) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    currentTeamName.uppercase(), 
                    style = MaterialTheme.typography.labelLarge, 
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "INNINGS ${score.currentInnings}", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    "$currentTeamScore / $currentTeamWickets", 
                    fontSize = 56.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White
                )
                
                Text("OVERS: $currentTeamOvers / ${score.maxOvers}", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                
                // FIX: Only show chasing target info if match is Live
                if (isLive && isInnings2 && score.targetScore > 0) {
                    val runsNeeded = score.targetScore - currentTeamScore
                    val parts = currentTeamOvers.split(".")
                    val oversDone = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val ballsDone = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val totalBallsDone = (oversDone * 6) + ballsDone
                    val ballsRemaining = (score.maxOvers * 6) - totalBallsDone
                    
                    if (runsNeeded > 0 && ballsRemaining >= 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = com.grama.sports.ui.theme.TertiaryColor.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "NEEDS $runsNeeded RUNS FROM $ballsRemaining BALLS",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Text("Striker", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(if(score.strikerName == "OUT") "SELECT NEW" else "${score.strikerName}*", 
                            color = if(score.strikerName == "OUT") com.grama.sports.ui.theme.TertiaryColor else Color.White, 
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        if(score.strikerName != "OUT") {
                            Text("${score.strikerRuns}(${score.strikerBalls}) | SR: ${calculateSR(score.strikerRuns, score.strikerBalls)}", 
                                color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text("Non-Striker", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(score.nonStrikerName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${score.nonStrikerRuns}(${score.nonStrikerBalls}) | SR: ${calculateSR(score.nonStrikerRuns, score.nonStrikerBalls)}", 
                            color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bowler", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        Text(if(score.bowlerName == "OUT") "SELECT NEW" else score.bowlerName, 
                            color = if(score.bowlerName == "OUT") com.grama.sports.ui.theme.TertiaryColor else Color.White, 
                            fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    if(score.bowlerName != "OUT") {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${calculateBowlerOvers(score.bowlerBallsBowled)}-${score.bowlerWickets}-${score.bowlerRunsConceded}", 
                                color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Econ: ${calculateEcon(score.bowlerRunsConceded, score.bowlerBallsBowled)}", 
                                color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        score.statusText.uppercase(), 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = com.grama.sports.ui.theme.TertiaryColor, 
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    
    if (isLive) {
        Button(
            onClick = { showPlayerSelection = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.Person, null)
            Spacer(Modifier.width(8.dp))
            Text("Change Players (Striker/Bowler)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isReady = score.strikerName != "OUT" && score.strikerName != "Striker" && score.bowlerName != "OUT" && score.bowlerName != "Bowler"
        
        Text("Record Runs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0, 1, 2, 3).forEach { r ->
                ScoreButton("$r", Modifier.weight(1f), enabled = isReady) { viewModel.updateCricketScore(r) }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreButton("4", Modifier.weight(1f), com.grama.sports.ui.theme.SecondaryColor, enabled = isReady) { viewModel.updateCricketScore(4) }
            ScoreButton("6", Modifier.weight(1f), com.grama.sports.ui.theme.PrimaryColor, enabled = isReady) { viewModel.updateCricketScore(6) }
            ScoreButton("WD", Modifier.weight(1f), com.grama.sports.ui.theme.TertiaryColor, enabled = isReady) { viewModel.updateCricketScore(0, extraType = "Wide") }
            ScoreButton("NB", Modifier.weight(1f), Color(0xFFFF9500), enabled = isReady) { viewModel.updateCricketScore(0, extraType = "No Ball") }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Other Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreButton("BYE", Modifier.weight(1f), enabled = isReady) { viewModel.updateCricketScore(1, extraType = "Bye") }
            ScoreButton("LB", Modifier.weight(1f), enabled = isReady) { viewModel.updateCricketScore(1, extraType = "Leg Bye") }
            Button(
                enabled = isReady,
                onClick = { viewModel.updateCricketScore(0, isWicket = true) },
                modifier = Modifier.weight(2f).height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { 
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("WICKET", fontWeight = FontWeight.Black) 
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (score.currentInnings == 1) {
            Button(
                onClick = { viewModel.switchInnings() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) { 
                Text("FORCE END INNINGS", fontWeight = FontWeight.Bold) 
            }
        } else {
            Button(
                onClick = { 
                    val result = if (score.teamBScore >= score.targetScore) {
                        "${score.teamBName} won"
                    } else {
                        "${score.teamAName} won"
                    }
                    viewModel.endMatch(score.matchId, result) 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) { 
                Text("END MATCH MANUALLY", fontWeight = FontWeight.Bold) 
            }
        }
    } else {
        // Match is Completed
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MATCH COMPLETED", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(match?.result ?: score.statusText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showPlayerSelection) {
        PlayerSelectionDialog(
            battingPlayers = battingPlayers,
            bowlingPlayers = bowlingPlayers,
            score = score,
            onDismiss = { showPlayerSelection = false },
            onConfirm = { s, ns, b ->
                viewModel.updateCricketPlayers(s, ns, b)
                showPlayerSelection = false
            }
        )
    }
}

@Composable
fun KabaddiScoringPanel(score: LiveScore, viewModel: AppViewModel, matchId: String, match: Match?) {
    val allPlayers by viewModel.players.collectAsState()
    val teamAPlayers = allPlayers.filter { it.teamId == match?.teamAId || it.teamName == match?.teamAName }
    val teamBPlayers = allPlayers.filter { it.teamId == match?.teamBId || it.teamName == match?.teamBName }
    
    val currentRaiderName = score.currentRaiderName
    val isTeamARaiding = score.raidingTeam == "A"
    val isLive = match?.status?.lowercase() == "live"

    Card(
        modifier = Modifier.fillMaxWidth(), 
        elevation = CardDefaults.cardElevation(8.dp), 
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("HALF ${score.currentHalf}", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("RAID TIMER: ${score.raidTimer}s", color = if(score.raidTimer < 10) Color.Red else Color.Black, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.Black, 
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    score.timer, 
                    fontSize = 56.sp, 
                    fontWeight = FontWeight.Black, 
                    color = com.grama.sports.ui.theme.TertiaryColor, 
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
            
            if (isLive) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.toggleTimer(matchId) }, 
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Icon(if(score.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if(score.isTimerRunning) "PAUSE" else "START") 
                    }
                    OutlinedButton(
                        onClick = { viewModel.resetTimer(matchId) }, 
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("RESET") }
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KabaddiTeamScoreCard(score.teamAName, score.teamAScore, score.teamAOnFieldCount, isTeamARaiding, Modifier.weight(1f)) {
             if (isLive) viewModel.setRaidingTeam("A")
        }
        KabaddiTeamScoreCard(score.teamBName, score.teamBScore, score.teamBOnFieldCount, !isTeamARaiding, Modifier.weight(1f)) {
             if (isLive) viewModel.setRaidingTeam("B")
        }
    }
    
    if (isLive) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Select Current Raider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val raiderList = if(isTeamARaiding) teamAPlayers else teamBPlayers
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            raiderList.forEach { p ->
                FilterChip(
                    selected = currentRaiderName == p.name,
                    onClick = { viewModel.setCurrentRaider(p.name) },
                    label = { Text(p.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Record Action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        val raidTeamA = isTeamARaiding
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { pts ->
                ScoreButton("+$pts", Modifier.weight(1f)) { viewModel.handleKabaddiAction(raidTeamA, "Raid", pts) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreButton("BONUS", Modifier.weight(1f), com.grama.sports.ui.theme.SecondaryColor) { viewModel.handleKabaddiAction(raidTeamA, "Bonus") }
            ScoreButton("CAUGHT", Modifier.weight(1f), Color.Red) { viewModel.handleKabaddiAction(raidTeamA, "Raider Out") }
            ScoreButton("EMPTY", Modifier.weight(1f), Color.Gray) { viewModel.handleKabaddiAction(raidTeamA, "Empty Raid") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Other Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.handleKabaddiAction(!raidTeamA, "Tackle") },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.grama.sports.ui.theme.KabaddiColor)
            ) {
                Text("TACKLE POINT", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.handleKabaddiAction(raidTeamA, "Out of Bounds") },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("OUT OF BOUNDS", fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.handleKabaddiAction(raidTeamA, "All Out") },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("ALL OUT (+2)", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { viewModel.switchKabaddiHalf() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("SWITCH HALF / HALFTIME")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { 
                val res = if(score.teamAScore > score.teamBScore) "${score.teamAName} Won" else if(score.teamBScore > score.teamAScore) "${score.teamBName} Won" else "Match Tied"
                viewModel.endMatch(matchId, res)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("END MATCH")
        }
    } else {
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MATCH COMPLETED", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(match?.result ?: score.statusText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun KabaddiTeamScoreCard(name: String, score: Int, onField: Int, isRaiding: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if(isRaiding) com.grama.sports.ui.theme.KabaddiColor.copy(alpha = 0.1f) else Color.White
        ),
        border = if(isRaiding) androidx.compose.foundation.BorderStroke(2.dp, com.grama.sports.ui.theme.KabaddiColor) else null,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(score.toString(), fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("ON FIELD: $onField", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            if (isRaiding) {
                Text("RAIDING", color = com.grama.sports.ui.theme.KabaddiColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun VolleyballScoringPanel(score: LiveScore, viewModel: AppViewModel, match: Match?) {
    val allPlayers by viewModel.players.collectAsState()
    val teamAPlayers = allPlayers.filter { it.teamId == match?.teamAId || it.teamName == match?.teamAName }
    val teamBPlayers = allPlayers.filter { it.teamId == match?.teamBId || it.teamName == match?.teamBName }
    val isLive = match?.status?.lowercase() == "live"
    
    var selectedPlayer by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(), 
        elevation = CardDefaults.cardElevation(8.dp), 
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SET ${score.currentSet}", fontWeight = FontWeight.Black, color = com.grama.sports.ui.theme.VolleyballColor)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                VolleyTeamPointColumn(score.teamAName, score.teamAScore, score.teamASets, score.servingTeam == "A")
                Text("VS", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.LightGray)
                VolleyTeamPointColumn(score.teamBName, score.teamBScore, score.teamBSets, score.servingTeam == "B")
            }
            
            if (score.teamASetScores.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Sets: A(${score.teamASetScores}) - B(${score.teamBSetScores})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
    
    if (isLive) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Select Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (teamAPlayers + teamBPlayers).forEach { p ->
                FilterChip(
                    selected = selectedPlayer == p.name,
                    onClick = { selectedPlayer = p.name },
                    label = { Text(p.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(score.teamAName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                VolleyActionButtons { type -> viewModel.handleVolleyballPoint(true, type, selectedPlayer) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(score.teamBName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                VolleyActionButtons { type -> viewModel.handleVolleyballPoint(false, type, selectedPlayer) }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                val res = if(score.teamASets > score.teamBSets) "${score.teamAName} Won" else "${score.teamBName} Won"
                viewModel.endMatch(score.matchId, res)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("END MATCH MANUALLY")
        }
    } else {
        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MATCH COMPLETED", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(match?.result ?: score.statusText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun VolleyActionButtons(onAction: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onAction("Kill") }, modifier = Modifier.fillMaxWidth()) { Text("KILL (+1)") }
        Button(onClick = { onAction("Ace") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = com.grama.sports.ui.theme.SecondaryColor)) { Text("ACE (+1)") }
        Button(onClick = { onAction("Block") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = com.grama.sports.ui.theme.VolleyballColor)) { Text("BLOCK (+1)") }
        OutlinedButton(onClick = { onAction("Error") }, modifier = Modifier.fillMaxWidth()) { Text("OTHER PT") }
    }
}

@Composable
fun VolleyTeamPointColumn(name: String, score: Int, sets: Int, isServing: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Sets: $sets", style = MaterialTheme.typography.labelSmall)
        Text(score.toString(), fontSize = 64.sp, fontWeight = FontWeight.Black)
        if (isServing) {
            Surface(color = Color.Yellow, shape = CircleShape) {
                Icon(Icons.Default.Star, contentDescription = "Serving", modifier = Modifier.size(16.dp).padding(2.dp))
            }
        }
    }
}

@Composable
fun ScoreButton(text: String, modifier: Modifier = Modifier, containerColor: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = if (containerColor != null) ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
                 else ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.elevatedButtonElevation(2.dp)
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
fun PlayerSelectionDialog(
    battingPlayers: List<Player>,
    bowlingPlayers: List<Player>,
    score: LiveScore,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var selectedStriker by remember { mutableStateOf(if(score.strikerName == "OUT" || score.strikerName == "Striker") "" else score.strikerName) }
    var selectedNonStriker by remember { mutableStateOf(if(score.nonStrikerName == "OUT" || score.nonStrikerName == "Non-Striker") "" else score.nonStrikerName) }
    var selectedBowler by remember { mutableStateOf(if(score.bowlerName == "OUT" || score.bowlerName == "Bowler") "" else score.bowlerName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Match Players") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Select Striker", fontWeight = FontWeight.Bold)
                battingPlayers.filter { it.name != selectedNonStriker && it.name !in score.outPlayers }.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedStriker = p.name }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedStriker == p.name, onClick = { selectedStriker = p.name })
                        Text(p.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Non-Striker", fontWeight = FontWeight.Bold)
                battingPlayers.filter { it.name != selectedStriker && it.name !in score.outPlayers }.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedNonStriker = p.name }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedNonStriker == p.name, onClick = { selectedNonStriker = p.name })
                        Text(p.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Current Bowler", fontWeight = FontWeight.Bold)
                bowlingPlayers.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedBowler = p.name }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedBowler == p.name, onClick = { selectedBowler = p.name })
                        Text(p.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedStriker.isNotBlank() && selectedNonStriker.isNotBlank() && selectedBowler.isNotBlank(),
                onClick = { onConfirm(selectedStriker, selectedNonStriker, selectedBowler) }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun calculateSR(runs: Int, balls: Int): String {
    if (balls == 0) return "0.00"
    return String.format("%.2f", (runs.toDouble() / balls) * 100)
}

private fun calculateEcon(runs: Int, balls: Int): String {
    if (balls == 0) return "0.00"
    val overs = balls / 6.0
    return String.format("%.2f", runs / overs)
}

private fun calculateBowlerOvers(balls: Int): String {
    val overs = balls / 6
    val remainingBalls = balls % 6
    return "$overs.$remainingBalls"
}
