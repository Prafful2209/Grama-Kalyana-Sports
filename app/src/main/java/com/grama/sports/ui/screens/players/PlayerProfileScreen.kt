package com.grama.sports.ui.screens.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(navController: NavController, viewModel: AppViewModel) {
    val players by viewModel.players.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPlayers = players.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.teamName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Player Profiles") }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back") 
                    } 
                }
            ) 
        },
        floatingActionButton = { 
            if (isAdmin) {
                FloatingActionButton(onClick = { showDialog = true }) { 
                    Icon(Icons.Default.Add, contentDescription = "Add Player") 
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Players or Teams") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filteredPlayers.isEmpty()) {
                        item { 
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(if (searchQuery.isEmpty()) "No players registered yet." else "No players found matching \"$searchQuery\"")
                            }
                        }
                    } else {
                        items(filteredPlayers) { p ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Team: ${p.teamName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Age: ${p.age}")
                                        Text("Jersey: #${p.jerseyNumber}")
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Matches: ${p.cricketMatches}")
                                        Text("Runs: ${p.cricketRuns}")
                                        Text("Wickets: ${p.cricketWickets}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddPlayerDialog(
            onDismiss = { showDialog = false },
            onSave = { name, age, team, jersey ->
                viewModel.createPlayer(name, age, "", team, jersey)
                showDialog = false
            }
        )
    }
}

@Composable
fun AddPlayerDialog(onDismiss: () -> Unit, onSave: (String, Int, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var jersey by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Player") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("Team Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = age, 
                        onValueChange = { age = it }, 
                        label = { Text("Age") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = jersey, 
                        onValueChange = { jersey = it }, 
                        label = { Text("Jersey") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onSave(name, age.toIntOrNull() ?: 0, teamName, jersey.toIntOrNull() ?: 0)
                }
            }) { Text("Save Player") }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel") } 
        }
    )
}
