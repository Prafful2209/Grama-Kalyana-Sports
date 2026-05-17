package com.grama.sports.ui.screens.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.grama.sports.models.Team
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(navController: NavController, viewModel: AppViewModel) {
    val teams by viewModel.teams.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var teamToDelete by remember { mutableStateOf<Team?>(null) }
    var showFirstDeleteDialog by remember { mutableStateOf(false) }
    var showSecondDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Team Management") }, 
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
                    Icon(Icons.Default.Add, contentDescription = "Add Team") 
                }
            }
        }
    ) { padding ->
        if (isLoading && teams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val cricketTeams = teams.filter { it.sportType == "Cricket" }
            val kabaddiTeams = teams.filter { it.sportType == "Kabaddi" }
            val volleyballTeams = teams.filter { it.sportType == "Volleyball" }

            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                if (teams.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No teams registered. Add one to get started.")
                    }
                } else {
                    LazyColumn {
                        if (cricketTeams.isNotEmpty()) {
                            item { SportSectionHeader("Cricket Teams") }
                            items(cricketTeams) { team -> 
                                TeamItem(team, isAdmin) { 
                                    teamToDelete = team
                                    showFirstDeleteDialog = true 
                                } 
                            }
                        }
                        if (kabaddiTeams.isNotEmpty()) {
                            item { SportSectionHeader("Kabaddi Teams") }
                            items(kabaddiTeams) { team -> 
                                TeamItem(team, isAdmin) { 
                                    teamToDelete = team
                                    showFirstDeleteDialog = true 
                                } 
                            }
                        }
                        if (volleyballTeams.isNotEmpty()) {
                            item { SportSectionHeader("Volleyball Teams") }
                            items(volleyballTeams) { team -> 
                                TeamItem(team, isAdmin) { 
                                    teamToDelete = team
                                    showFirstDeleteDialog = true 
                                } 
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Team Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var village by remember { mutableStateOf("") }
        var selectedSport by remember { mutableStateOf("Cricket") }
        val sports = listOf("Cricket", "Kabaddi", "Volleyball")

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Add New Team") },
            text = {
                Column {
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = name, 
                        onValueChange = { name = it }, 
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = village, 
                        onValueChange = { village = it }, 
                        label = { Text("Village") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Sport Type:", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sports.forEach { sport ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedSport == sport,
                                    onClick = { selectedSport = sport }
                                )
                                Text(sport, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && name.isNotBlank(),
                    onClick = { 
                        viewModel.createTeam(name, village, selectedSport)
                        showAddDialog = false 
                    }
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save Team")
                }
            },
            dismissButton = { 
                TextButton(enabled = !isSaving, onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // First Confirmation Dialog
    if (showFirstDeleteDialog && teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { showFirstDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete team '${teamToDelete?.name}'?") },
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

    // Second Confirmation Dialog
    if (showSecondDeleteDialog && teamToDelete != null) {
        AlertDialog(
            onDismissRequest = { showSecondDeleteDialog = false },
            title = { Text("Final Confirmation") },
            text = { Text("This action is permanent and will delete all associated data for '${teamToDelete?.name}'. Are you absolutely sure?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        teamToDelete?.id?.let { viewModel.deleteTeam(it) }
                        showSecondDeleteDialog = false
                        teamToDelete = null
                    }
                ) {
                    Text("I am sure, Delete it")
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
fun TeamItem(team: Team, isAdmin: Boolean, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Village: ${team.village}", style = MaterialTheme.typography.bodyMedium)
            }
            if (isAdmin) {
                IconButton(onClick = { onDelete(team.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
