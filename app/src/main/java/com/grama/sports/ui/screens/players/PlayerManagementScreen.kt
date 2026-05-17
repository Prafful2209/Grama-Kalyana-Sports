package com.grama.sports.ui.screens.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.grama.sports.models.Player
import com.grama.sports.models.Team
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(navController: NavController, viewModel: AppViewModel) {
    val players by viewModel.players.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showFirstDeleteDialog by remember { mutableStateOf(false) }
    var showSecondDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Player Management") }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back") 
                    } 
                }
            ) 
        },
        floatingActionButton = { 
            if (isAdmin) {
                FloatingActionButton(onClick = { showAddDialog = true }) { 
                    Icon(Icons.Default.Add, contentDescription = "Add Player") 
                }
            }
        }
    ) { padding ->
        if (isLoading && players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val cricketPlayers = players.filter { it.sportType == "Cricket" }
            val kabaddiPlayers = players.filter { it.sportType == "Kabaddi" }
            val volleyballPlayers = players.filter { it.sportType == "Volleyball" }

            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                if (players.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No players found. Add players to teams.")
                    }
                } else {
                    LazyColumn {
                        if (cricketPlayers.isNotEmpty()) {
                            item { SportSectionHeader("Cricketers") }
                            items(cricketPlayers) { player -> 
                                PlayerItem(player, isAdmin) { 
                                    playerToDelete = player
                                    showFirstDeleteDialog = true
                                } 
                            }
                        }
                        if (kabaddiPlayers.isNotEmpty()) {
                            item { SportSectionHeader("Kabaddi Players") }
                            items(kabaddiPlayers) { player -> 
                                PlayerItem(player, isAdmin) { 
                                    playerToDelete = player
                                    showFirstDeleteDialog = true
                                } 
                            }
                        }
                        if (volleyballPlayers.isNotEmpty()) {
                            item { SportSectionHeader("Volleyball Players") }
                            items(volleyballPlayers) { player -> 
                                PlayerItem(player, isAdmin) { 
                                    playerToDelete = player
                                    showFirstDeleteDialog = true
                                } 
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var jersey by remember { mutableStateOf("") }
        var selectedTeam by remember { mutableStateOf<Team?>(null) }
        var showTeamDropdown by remember { mutableStateOf(false) }
        var selectedSport by remember { mutableStateOf("Cricket") }
        val sports = listOf("Cricket", "Kabaddi", "Volleyball")

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Add Player") },
            text = {
                Column {
                    OutlinedTextField(enabled = !isSaving, value = name, onValueChange = { name = it }, label = { Text("Player Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(enabled = !isSaving, value = age, onValueChange = { age = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(enabled = !isSaving, value = jersey, onValueChange = { jersey = it }, label = { Text("Jersey Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box {
                        OutlinedTextField(
                            value = selectedTeam?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Team") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showTeamDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(expanded = showTeamDropdown, onDismissRequest = { showTeamDropdown = false }) {
                            teams.forEach { team ->
                                DropdownMenuItem(
                                    text = { Text("${team.name} (${team.sportType})") },
                                    onClick = {
                                        selectedTeam = team
                                        selectedSport = team.sportType // Automatically select team's sport
                                        showTeamDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Player Type:", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sports.forEach { sport ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedSport == sport,
                                    onClick = { selectedSport = sport }
                                )
                                Text(if(sport == "Cricket") "Cricketer" else if(sport == "Kabaddi") "Kabaddi" else sport, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && name.isNotBlank() && selectedTeam != null,
                    onClick = {
                        viewModel.createPlayer(
                            name = name, 
                            age = age.toIntOrNull() ?: 0, 
                            teamId = selectedTeam?.id ?: "", 
                            teamName = selectedTeam?.name ?: "",
                            jersey = jersey.toIntOrNull() ?: 0,
                            sportType = selectedSport
                        )
                        showAddDialog = false
                    }
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save")
                }
            },
            dismissButton = { 
                TextButton(enabled = !isSaving, onClick = { showAddDialog = false }) { Text("Cancel") } 
            }
        )
    }

    // First Deletion Confirmation
    if (showFirstDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showFirstDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete player '${playerToDelete?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFirstDeleteDialog = false
                        showSecondDeleteDialog = true
                    }
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Second Deletion Confirmation
    if (showSecondDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showSecondDeleteDialog = false },
            title = { Text("Final Confirmation") },
            text = { Text("Are you absolutely sure you want to remove '${playerToDelete?.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        playerToDelete?.id?.let { viewModel.deletePlayer(it) }
                        showSecondDeleteDialog = false
                        playerToDelete = null
                    }
                ) {
                    Text("I am sure, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecondDeleteDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }
}

@Composable
fun SportSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun PlayerItem(player: Player, isAdmin: Boolean, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Team: ${player.teamName}", style = MaterialTheme.typography.bodyMedium)
                Text("Age: ${player.age} | Jersey: #${player.jerseyNumber}", style = MaterialTheme.typography.bodySmall)
            }
            if (isAdmin) {
                IconButton(onClick = { onDelete(player.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
