package com.grama.sports.ui.screens.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.grama.sports.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(navController: NavController, viewModel: AppViewModel) {
    val tournaments by viewModel.tournaments.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Tournaments") }, 
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
                    Icon(Icons.Default.Add, contentDescription = "Add Tournament") 
                }
            }
        }
    ) { padding ->
        if (isLoading && tournaments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                if (tournaments.isEmpty()) {
                    item { 
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No tournaments found.") 
                        }
                    }
                } else {
                    items(tournaments) { t ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(t.name, style = MaterialTheme.typography.titleMedium)
                                Text("${t.sportType} | ${t.village}")
                                if (t.date.isNotBlank()) {
                                    Text("Date: ${t.date}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var village by remember { mutableStateOf("") }
        var sport by remember { mutableStateOf("Cricket") }
        var date by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showDialog = false },
            title = { Text("New Tournament") },
            text = {
                Column {
                    OutlinedTextField(enabled = !isSaving, value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(enabled = !isSaving, value = village, onValueChange = { village = it }, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(enabled = !isSaving, value = sport, onValueChange = { sport = it }, label = { Text("Sport") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(enabled = !isSaving, value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving && name.isNotBlank(),
                    onClick = { 
                        viewModel.createTournament(name, village, date.ifBlank { "Today" }, sport)
                        showDialog = false 
                    }
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Create")
                }
            },
            dismissButton = { 
                TextButton(enabled = !isSaving, onClick = { showDialog = false }) { Text("Cancel") } 
            }
        )
    }
}
