package com.grama.sports.ui.screens.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.grama.sports.models.Team
import com.grama.sports.models.Player
import com.grama.sports.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchSchedulerScreen(navController: NavController, viewModel: AppViewModel) {
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val allTeams by viewModel.teams.collectAsState()
    val allPlayers by viewModel.players.collectAsState()
    
    var sport by remember { mutableStateOf("Cricket") }
    
    // Filter teams based on selected sport
    val filteredTeams = remember(allTeams, sport) {
        allTeams.filter { it.sportType.equals(sport, ignoreCase = true) }
    }
    
    var teamA by remember { mutableStateOf<Team?>(null) }
    var teamB by remember { mutableStateOf<Team?>(null) }
    var selectedPlayersA by remember { mutableStateOf(setOf<String>()) }
    var selectedPlayersB by remember { mutableStateOf(setOf<String>()) }
    
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var maxOvers by remember { mutableStateOf("20") }

    var showTeamADropdown by remember { mutableStateOf(false) }
    var showTeamBDropdown by remember { mutableStateOf(false) }

    // Reset team selection when sport changes
    LaunchedEffect(sport) {
        teamA = null
        teamB = null
        selectedPlayersA = emptySet()
        selectedPlayersB = emptySet()
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Schedule New Match") }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back") 
                    } 
                }
            ) 
        }
    ) { padding ->
        if (!isAdmin) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Access Denied. Only admins can schedule matches.")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Match Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Sport Type", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cricket", "Volleyball", "Kabaddi").forEach { type ->
                        FilterChip(
                            enabled = !isSaving,
                            selected = sport == type,
                            onClick = { sport = type },
                            label = { Text(type) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Team A Selection
                Text("First Team", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = teamA?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Team A ($sport)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showTeamADropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showTeamADropdown, 
                        onDismissRequest = { showTeamADropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        if (filteredTeams.isEmpty()) {
                            DropdownMenuItem(text = { Text("No $sport teams found") }, onClick = {})
                        }
                        filteredTeams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    teamA = team
                                    selectedPlayersA = emptySet()
                                    showTeamADropdown = false
                                }
                            )
                        }
                    }
                }
                
                if (teamA != null) {
                    val teamAPlayers = allPlayers.filter { 
                        it.teamId == teamA?.id || (it.teamName == teamA?.name && it.teamName.isNotBlank())
                    }
                    if (teamAPlayers.isEmpty()) {
                        Text("No players found for ${teamA?.name}. Add them in Player Management.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        PlayerSelectionRow(
                            title = "Pick Playing XI for ${teamA?.name}", 
                            subtitle = "${selectedPlayersA.size} players selected",
                            players = teamAPlayers, 
                            selectedIds = selectedPlayersA
                        ) {
                            selectedPlayersA = if (selectedPlayersA.contains(it)) selectedPlayersA - it else selectedPlayersA + it
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Team B Selection
                Text("Second Team", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedTextField(
                        value = teamB?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Team B ($sport)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showTeamBDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showTeamBDropdown, 
                        onDismissRequest = { showTeamBDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        if (filteredTeams.isEmpty()) {
                            DropdownMenuItem(text = { Text("No $sport teams found") }, onClick = {})
                        }
                        filteredTeams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.name) },
                                onClick = {
                                    teamB = team
                                    selectedPlayersB = emptySet()
                                    showTeamBDropdown = false
                                }
                            )
                        }
                    }
                }

                if (teamB != null) {
                    val teamBPlayers = allPlayers.filter { 
                        it.teamId == teamB?.id || (it.teamName == teamB?.name && it.teamName.isNotBlank())
                    }
                    if (teamBPlayers.isEmpty()) {
                        Text("No players found for ${teamB?.name}. Add them in Player Management.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        PlayerSelectionRow(
                            title = "Pick Playing XI for ${teamB?.name}", 
                            subtitle = "${selectedPlayersB.size} players selected",
                            players = teamBPlayers, 
                            selectedIds = selectedPlayersB
                        ) {
                            selectedPlayersB = if (selectedPlayersB.contains(it)) selectedPlayersB - it else selectedPlayersB + it
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Match Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (sport == "Cricket") {
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = maxOvers,
                        onValueChange = { if (it.all { char -> char.isDigit() }) maxOvers = it },
                        label = { Text("Max Overs") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                OutlinedTextField(enabled = !isSaving, value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 25 Oct 2025)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(enabled = !isSaving, value = time, onValueChange = { time = it }, label = { Text("Time (e.g. 10:00 AM)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(enabled = !isSaving, value = venue, onValueChange = { venue = it }, label = { Text("Venue / Ground") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        if (teamA != null && teamB != null && date.isNotBlank()) {
                            viewModel.createMatch(
                                teamAId = teamA!!.id,
                                teamBId = teamB!!.id,
                                teamAName = teamA!!.name,
                                teamBName = teamB!!.name,
                                sport = sport,
                                date = date,
                                venue = venue,
                                time = time,
                                maxOvers = maxOvers.toIntOrNull() ?: 20,
                                teamAPlayerIds = selectedPlayersA.toList(),
                                teamBPlayerIds = selectedPlayersB.toList()
                            )
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSaving && teamA != null && teamB != null && teamA != teamB && (date.isNotBlank() || time.isNotBlank())
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Schedule Match")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionRow(title: String, subtitle: String, players: List<Player>, selectedIds: Set<String>, onToggle: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                players.forEach { player ->
                    val isSelected = selectedIds.contains(player.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(player.id) },
                        label = { Text(player.name, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}
