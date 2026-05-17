package com.grama.sports.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.models.Match
import com.grama.sports.ui.navigation.Routes
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: AppViewModel) {
    val liveMatches by viewModel.liveMatches.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val completedMatches by viewModel.completedMatches.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var matchToDelete by remember { mutableStateOf<Match?>(null) }
    var matchToReschedule by remember { mutableStateOf<Match?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Grama-Kalyana Sports", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (isAdmin) "Admin Mode" else "Fan Mode", 
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAdmin) com.grama.sports.ui.theme.TertiaryColor else Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAdminMode() }) {
                        Icon(
                            if (isAdmin) Icons.Default.Visibility else Icons.Default.AdminPanelSettings,
                            contentDescription = "Switch Mode",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.MATCH_SCHEDULER) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Schedule Match") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isAdmin) {
                item {
                    SectionHeader("Management Controls")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminActionCard("Tournaments", Icons.Default.EmojiEvents, Modifier.weight(1f)) {
                            navController.navigate(Routes.TOURNAMENT_LIST)
                        }
                        AdminActionCard("Teams", Icons.Default.Groups, Modifier.weight(1f)) {
                            navController.navigate(Routes.TEAM_MANAGEMENT)
                        }
                        AdminActionCard("Players", Icons.Default.Person, Modifier.weight(1f)) {
                            navController.navigate(Routes.PLAYER_MANAGEMENT)
                        }
                    }
                }
            }

            item { SectionHeader("Live Matches") }
            if (liveMatches.isEmpty()) {
                if (isLoading) {
                    item { LoadingPlaceholder() }
                } else {
                    item { EmptyStateCard("No live matches at the moment") }
                }
            } else {
                items(liveMatches) { match ->
                    LiveMatchCard(
                        match = match,
                        isAdmin = isAdmin,
                        onScoreClick = { navController.navigate(Routes.liveScore(match.id, match.sportType)) },
                        onViewClick = { navController.navigate(Routes.fanView(match.id, match.sportType)) },
                        onDeleteClick = { matchToDelete = match }
                    )
                }
            }

            item { SectionHeader("Upcoming Matches") }
            if (upcomingMatches.isEmpty()) {
                if (!isLoading) {
                    item { EmptyStateCard("No scheduled matches") }
                }
            } else {
                items(upcomingMatches) { match ->
                    UpcomingMatchCard(
                        match = match, 
                        isAdmin = isAdmin,
                        onStartMatch = { viewModel.startMatch(match) },
                        onViewClick = { navController.navigate(Routes.fanView(match.id, match.sportType)) },
                        onDeleteClick = { matchToDelete = match },
                        onRescheduleClick = { matchToReschedule = match }
                    )
                }
            }

            item { SectionHeader("Recent Results") }
            if (completedMatches.isEmpty()) {
                if (!isLoading) {
                    item { EmptyStateCard("No completed matches yet") }
                }
            } else {
                items(completedMatches) { match ->
                    CompletedMatchCard(match, isAdmin, 
                        onClick = { navController.navigate(Routes.fanView(match.id, match.sportType)) },
                        onDeleteClick = { matchToDelete = match }
                    )
                }
            }

            item { SectionHeader("Active Tournaments") }
            if (tournaments.isEmpty()) {
                if (!isLoading) {
                    item { EmptyStateCard("No active tournaments") }
                }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(tournaments) { tournament ->
                            TournamentCard(tournament)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Delete Match") },
            text = { Text("Are you sure you want to delete this match data? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMatch(matchToDelete!!.id)
                        matchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (matchToReschedule != null) {
        RescheduleDialog(
            match = matchToReschedule!!,
            onDismiss = { matchToReschedule = null },
            onConfirm = { date, time, venue ->
                viewModel.rescheduleMatch(matchToReschedule!!.id, date, time, venue)
                matchToReschedule = null
            }
        )
    }
}

@Composable
fun RescheduleDialog(match: Match, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var date by remember { mutableStateOf(match.date) }
    var time by remember { mutableStateOf(match.time) }
    var venue by remember { mutableStateOf(match.venue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Match") },
        text = {
            Column {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("New Date") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("New Time") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("New Venue") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(date, time, venue) }) { Text("Reschedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingMatchCard(match: Match, isAdmin: Boolean, onStartMatch: () -> Unit, onViewClick: () -> Unit, onDeleteClick: () -> Unit, onRescheduleClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onViewClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(match.sportType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${match.date} ${match.time}", style = MaterialTheme.typography.labelSmall)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${match.teamAName} vs ${match.teamBName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Venue: ${match.venue}", style = MaterialTheme.typography.bodySmall)
            
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartMatch, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Start Live", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onRescheduleClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("Reschedule", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedMatchCard(match: Match, isAdmin: Boolean, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(match.sportType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.date, style = MaterialTheme.typography.labelSmall)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${match.teamAName} vs ${match.teamBName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (match.result.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(match.result, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("View Full Scorecard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActionCard(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LiveMatchCard(match: Match, isAdmin: Boolean, onScoreClick: () -> Unit, onViewClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        com.grama.sports.ui.theme.GradientBrightStart,
                        com.grama.sports.ui.theme.GradientBrightEnd
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = com.grama.sports.ui.theme.TertiaryColor, shape = RoundedCornerShape(4.dp)) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(match.sportType, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(match.teamAName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("VS", fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 8.dp))
                    Text(match.teamBName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onViewClick, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = com.grama.sports.ui.theme.GradientBrightEnd)
                    ) { Text("View Score") }
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onScoreClick, 
                            modifier = Modifier.weight(1f), 
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) { Text("Update Score") }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentCard(tournament: com.grama.sports.models.Tournament) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(tournament.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(tournament.village, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(tournament.sportType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(message, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
