package com.grama.sports.ui.screens.fanview

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.models.LiveScore
import com.grama.sports.models.Match
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanViewScreen(navController: NavController, matchId: String, sportType: String, viewModel: AppViewModel) {
    val currentScore by viewModel.currentLiveScore.collectAsState()
    val liveMatches by viewModel.liveMatches.collectAsState()
    val completedMatches by viewModel.completedMatches.collectAsState()
    
    val match = liveMatches.find { it.id == matchId } ?: completedMatches.find { it.id == matchId }
    
    LaunchedEffect(matchId) { 
        viewModel.observeLiveScore(matchId) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Match Center", fontWeight = FontWeight.Bold) },
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) 
                    } 
                },
                actions = {
                    IconButton(onClick = { /* Share Logic */ }) {
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
                .background(Color(0xFFF5F5F7))
                .verticalScroll(rememberScrollState())
        ) {
            ScoreHeader(currentScore, sportType, match)
            
            Column(modifier = Modifier.padding(16.dp)) {
                StatusTicker(currentScore.statusText)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when {
                    sportType == "Cricket" -> CricketProfessionalScoreboard(currentScore)
                    sportType == "Volleyball" -> VolleyballScoreboard(currentScore)
                    sportType.contains("Kaba", ignoreCase = true) -> KabaddiScoreboard(currentScore)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Match Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Sport", sportType)
                        @Suppress("DEPRECATION")
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        InfoRow("Venue", match?.venue ?: "Grama Stadium")
                        @Suppress("DEPRECATION")
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        if (sportType == "Cricket") {
                            InfoRow("Toss", "${currentScore.tossWinner} elected to ${currentScore.electedTo}")
                        } else {
                            InfoRow("Status", currentScore.statusText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTicker(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = com.grama.sports.ui.theme.TertiaryColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.grama.sports.ui.theme.TertiaryColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(com.grama.sports.ui.theme.TertiaryColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text.uppercase(),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = com.grama.sports.ui.theme.TertiaryColor,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ScoreHeader(score: LiveScore, sportType: String, match: Match?) {
    val isLive = match?.status?.lowercase() == "live"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(bottom = 32.dp, top = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamColumn(score.teamAName, score.teamAScore, if(sportType == "Cricket") "/${score.teamAWickets}" else "", Alignment.Start)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VS", fontWeight = FontWeight.Black, color = com.grama.sports.ui.theme.TertiaryColor, fontSize = 20.sp)
                    when {
                        sportType == "Cricket" -> {
                            Text("(${if(score.currentInnings == 1) score.teamAOvers else score.teamBOvers})", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        sportType.contains("Kaba", ignoreCase = true) -> {
                            Text("Half ${score.currentHalf}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        sportType == "Volleyball" -> {
                            Text("Set ${score.currentSet}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }

                TeamColumn(score.teamBName, score.teamBScore, if(sportType == "Cricket") "/${score.teamBWickets}" else "", Alignment.End)
            }
            
            if (sportType == "Cricket" && score.targetScore > 0) {
                val isInnings2 = score.currentInnings == 2
                val chasingScore = if (isInnings2) score.teamBScore else 0
                val chasingOvers = if (isInnings2) score.teamBOvers else "0.0"
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLive && isInnings2) {
                    val runsNeeded = score.targetScore - chasingScore
                    val parts = chasingOvers.split(".")
                    val oversDone = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val ballsDone = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val totalBallsDone = (oversDone * 6) + ballsDone
                    val ballsRemaining = (score.maxOvers * 6) - totalBallsDone
                    
                    if (runsNeeded > 0 && ballsRemaining >= 0) {
                        Surface(
                            color = com.grama.sports.ui.theme.TertiaryColor,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "NEEDS $runsNeeded RUNS FROM $ballsRemaining BALLS",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else if (isLive) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "TARGET: ${score.targetScore}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else if (!isLive && match?.status?.lowercase() == "completed") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "MATCH COMPLETED",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamColumn(name: String, score: Int, extra: String, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment) {
        Text(name, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
            if (extra.isNotEmpty()) {
                Text(extra, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
fun CricketProfessionalScoreboard(score: LiveScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("CRR", calculateCRR(score))
                if (score.currentInnings == 2) {
                    MetricItem("RRR", calculateRRR(score))
                }
                MetricItem("MAX OVERS", "${score.maxOvers}")
            }
            
            @Suppress("DEPRECATION")
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            Text("BATTING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            PlayerStatRow(
                name = if (score.strikerName == "OUT") "Waiting..." else "${score.strikerName}*", 
                runs = "${score.strikerRuns}", 
                balls = "${score.strikerBalls}", 
                sr = calculateSR(score.strikerRuns, score.strikerBalls)
            )
            PlayerStatRow(
                name = if (score.nonStrikerName == "OUT") "Waiting..." else score.nonStrikerName, 
                runs = "${score.nonStrikerRuns}", 
                balls = "${score.nonStrikerBalls}",
                sr = calculateSR(score.nonStrikerRuns, score.nonStrikerBalls)
            )
            
            @Suppress("DEPRECATION")
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            Text("BOWLING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            BowlerStatRow(
                name = if (score.bowlerName == "OUT") "Selecting..." else score.bowlerName,
                overs = calculateBowlerOvers(score.bowlerBallsBowled),
                wickets = score.bowlerWickets,
                runs = score.bowlerRunsConceded,
                econ = calculateEcon(score.bowlerRunsConceded, score.bowlerBallsBowled)
            )
            
            Text("Extras: ${score.extras} (wd ${score.wide}, nb ${score.noBall}, b ${score.byes}, lb ${score.legByes})", 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Gray,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PlayerStatRow(name: String, runs: String, balls: String, sr: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1)
        Row {
            StatBox("R", runs, MaterialTheme.colorScheme.primary)
            StatBox("B", balls, Color.DarkGray)
            StatBox("SR", sr, Color.Gray)
        }
    }
}

@Composable
fun BowlerStatRow(name: String, overs: String, wickets: Int, runs: Int, econ: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 1)
        Row {
            StatBox("O", overs, Color.DarkGray)
            StatBox("W", "$wickets", MaterialTheme.colorScheme.primary)
            StatBox("R", "$runs", Color.DarkGray)
            StatBox("ECON", econ, Color.Gray)
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 8.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun VolleyballScoreboard(score: LiveScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SETS WON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SetScoreBox(score.teamASets)
                Text("-", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                SetScoreBox(score.teamBSets)
            }
            
            if (score.teamASetScores.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Set Scores: A(${score.teamASetScores}) vs B(${score.teamBSetScores})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = com.grama.sports.ui.theme.VolleyballColor.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    "PLAYING SET ${score.currentSet}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = com.grama.sports.ui.theme.VolleyballColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SetScoreBox(sets: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$sets", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun KabaddiScoreboard(score: LiveScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MATCH TIMER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Surface(color = Color.Black, shape = RoundedCornerShape(8.dp)) {
                        Text(score.timer, color = com.grama.sports.ui.theme.TertiaryColor, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RAID TIMER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Surface(color = if(score.raidTimer < 10) Color.Red else Color.DarkGray, shape = RoundedCornerShape(8.dp)) {
                        Text("${score.raidTimer}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                }
            }
            
            @Suppress("DEPRECATION")
            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                KabaddiDetailMetric("${score.teamAName} Raids", score.teamARaidPoints)
                KabaddiDetailMetric("${score.teamAName} Tackles", score.teamATacklePoints)
                KabaddiDetailMetric("On Field", score.teamAOnFieldCount, color = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                KabaddiDetailMetric("${score.teamBName} Raids", score.teamBRaidPoints)
                KabaddiDetailMetric("${score.teamBName} Tackles", score.teamBTacklePoints)
                KabaddiDetailMetric("On Field", score.teamBOnFieldCount, color = MaterialTheme.colorScheme.primary)
            }
            
            if (score.teamAAllOuts > 0 || score.teamBAllOuts > 0) {
                @Suppress("DEPRECATION")
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                Text("All Outs: ${score.teamAName} ${score.teamAAllOuts} | ${score.teamBName} ${score.teamBAllOuts}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun KabaddiDetailMetric(label: String, value: Int, color: Color = com.grama.sports.ui.theme.KabaddiColor) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private fun calculateCRR(score: LiveScore): String {
    val runs = if(score.currentInnings == 1) score.teamAScore else score.teamBScore
    val oversStr = if(score.currentInnings == 1) score.teamAOvers else score.teamBOvers
    val parts = oversStr.split(".")
    val ov = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
    val bl = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    val totalOvers = ov + (bl / 6.0)
    return if (totalOvers > 0) String.format("%.2f", runs / totalOvers) else "0.00"
}

private fun calculateRRR(score: LiveScore): String {
    val chasingOvers = score.teamBOvers
    val oversParts = chasingOvers.split(".")
    val overCount = oversParts.getOrNull(0)?.toIntOrNull() ?: 0
    val ballCount = oversParts.getOrNull(1)?.toIntOrNull() ?: 0
    val ballsLeft = (score.maxOvers * 6) - ((overCount * 6) + ballCount)
    val needed = score.targetScore - score.teamBScore
    return if (ballsLeft > 0) String.format("%.2f", (needed.toDouble() / ballsLeft) * 6) else "0.00"
}

private fun calculateSR(runs: Int, balls: Int): String {
    if (balls == 0) return "0.0"
    return String.format("%.1f", (runs.toDouble() / balls) * 100)
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
