package com.grama.sports.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.grama.sports.models.*
import kotlinx.coroutines.tasks.await

class FirebaseManager {
    companion object {
        // Updated URL based on standard Firebase naming conventions for projects with generated IDs.
        // The previous error "forcefully killed" strongly suggests the URL was incorrect or blocked.
        private const val DATABASE_URL = "https://gramakalyanasports-44ff0-default-rtdb.firebaseio.com"
    }

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val database: FirebaseDatabase = try {
        FirebaseDatabase.getInstance(DATABASE_URL).apply {
            try { setPersistenceEnabled(true) } catch (e: Exception) {}
        }
    } catch (e: Exception) {
        Log.e("FirebaseManager", "Failed to initialize with URL: $DATABASE_URL. Falling back to default instance.", e)
        FirebaseDatabase.getInstance().apply {
            try { setPersistenceEnabled(true) } catch (e: Exception) {}
        }
    }

    // References
    val usersRef = database.getReference("users")
    val tournamentsRef = database.getReference("tournaments")
    val teamsRef = database.getReference("teams")
    val playersRef = database.getReference("players")
    val matchesRef = database.getReference("matches")
    val liveScoresRef = database.getReference("liveScores")

    suspend fun saveUser(user: User) {
        try {
            usersRef.child(user.id).setValue(user).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving user", e)
        }
    }

    suspend fun createTournament(tournament: Tournament) {
        try {
            val id = tournamentsRef.push().key ?: return
            tournamentsRef.child(id).setValue(tournament.copy(id = id)).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error creating tournament", e)
        }
    }
    
    suspend fun createMatch(match: Match) {
        try {
            val id = matchesRef.push().key ?: return
            matchesRef.child(id).setValue(match.copy(id = id)).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error creating match", e)
        }
    }

    suspend fun updateLiveScore(score: LiveScore) {
        try {
            liveScoresRef.child(score.matchId).setValue(score).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating live score", e)
        }
    }
}
