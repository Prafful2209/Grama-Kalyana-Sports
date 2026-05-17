package com.grama.sports.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.grama.sports.data.firebase.FirebaseModule
import com.grama.sports.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * SportsRepository — Single source of truth for all Firebase operations.
 */
object SportsRepository {
    private const val TAG = "SportsRepository"

    init {
        monitorConnection()
    }

    private fun monitorConnection() {
        FirebaseModule.database.getReference(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    Log.i(TAG, "✅ Firebase: Connected to Realtime Database")
                } else {
                    Log.w(TAG, "❌ Firebase: Disconnected from Database")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Connection listener cancelled: ${error.message}")
            }
        })
    }

    // ================================================================
    // MATCHES
    // ================================================================

    suspend fun saveMatch(match: Match): String? {
        return try {
            val id = if (match.id.isBlank()) FirebaseModule.matchesRef.push().key ?: return null else match.id
            val finalMatch = match.copy(id = id, status = match.status.lowercase())
            Log.d(TAG, "Saving match[$id]: ${match.teamAName} vs ${match.teamBName}, status=${finalMatch.status}")
            FirebaseModule.matchesRef.child(id).setValue(finalMatch).await()
            Log.i(TAG, "✅ Match saved: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Match save failed: ${e.message}", e)
            null
        }
    }

    suspend fun deleteMatch(matchId: String) {
        try {
            FirebaseModule.matchesRef.child(matchId).removeValue().await()
            FirebaseModule.liveScoresRef.child(matchId).removeValue().await()
            Log.i(TAG, "✅ Match and its scores deleted: $matchId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Match deletion failed: ${e.message}")
        }
    }

    fun getMatches(): Flow<List<Match>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "📡 Matches snapshot received. Count: ${snapshot.childrenCount}")
                val items = snapshot.children.mapNotNull { child ->
                    val match = child.getValue(Match::class.java)
                    if (match == null) {
                        Log.w(TAG, "⚠️ Failed to parse match at key: ${child.key}")
                    }
                    match
                }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Matches listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        FirebaseModule.matchesRef.addValueEventListener(listener)
        awaitClose { FirebaseModule.matchesRef.removeEventListener(listener) }
    }

    // ================================================================
    // TOURNAMENTS
    // ================================================================

    suspend fun saveTournament(tournament: Tournament): String? {
        return try {
            val id = if (tournament.id.isBlank()) FirebaseModule.tournamentsRef.push().key ?: return null else tournament.id
            FirebaseModule.tournamentsRef.child(id).setValue(tournament.copy(id = id)).await()
            id
        } catch (e: Exception) {
            null
        }
    }

    fun getTournaments(): Flow<List<Tournament>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Tournament::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        FirebaseModule.tournamentsRef.addValueEventListener(listener)
        awaitClose { FirebaseModule.tournamentsRef.removeEventListener(listener) }
    }

    // ================================================================
    // TEAMS
    // ================================================================

    suspend fun saveTeam(team: Team): String? {
        return try {
            val id = if (team.id.isBlank()) FirebaseModule.teamsRef.push().key ?: return null else team.id
            FirebaseModule.teamsRef.child(id).setValue(team.copy(id = id)).await()
            id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteTeam(teamId: String) {
        try {
            FirebaseModule.teamsRef.child(teamId).removeValue().await()
            Log.i(TAG, "✅ Team deleted: $teamId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Team deletion failed: ${e.message}")
        }
    }

    fun getTeams(): Flow<List<Team>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Team::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        FirebaseModule.teamsRef.addValueEventListener(listener)
        awaitClose { FirebaseModule.teamsRef.removeEventListener(listener) }
    }

    // ================================================================
    // PLAYERS
    // ================================================================

    suspend fun savePlayer(player: Player): String? {
        return try {
            val id = if (player.id.isBlank()) FirebaseModule.playersRef.push().key ?: return null else player.id
            FirebaseModule.playersRef.child(id).setValue(player.copy(id = id)).await()
            id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePlayer(playerId: String) {
        try {
            FirebaseModule.playersRef.child(playerId).removeValue().await()
            Log.i(TAG, "✅ Player deleted: $playerId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Player deletion failed: ${e.message}")
        }
    }

    fun getPlayers(): Flow<List<Player>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { it.getValue(Player::class.java) }
                trySend(items)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        FirebaseModule.playersRef.addValueEventListener(listener)
        awaitClose { FirebaseModule.playersRef.removeEventListener(listener) }
    }

    // ================================================================
    // LIVE SCORES — UNIFIED PATH: /liveScores/{matchId}
    // ================================================================

    fun observeLiveScore(matchId: String): Flow<LiveScore?> = callbackFlow {
        val ref = FirebaseModule.liveScoresRef.child(matchId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue(LiveScore::class.java)
                trySend(score)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ LiveScore listener cancelled for match[$matchId]: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveLiveScore(score: LiveScore) {
        if (score.matchId.isBlank()) return
        try {
            val updated = score.copy(timestamp = System.currentTimeMillis())
            FirebaseModule.liveScoresRef.child(score.matchId).setValue(updated).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ LiveScore save failed: ${e.message}")
        }
    }

    suspend fun updateMatchStatus(matchId: String, status: String, result: String? = null) {
        try {
            val updates = mutableMapOf<String, Any>("status" to status.lowercase())
            if (!result.isNullOrBlank()) updates["result"] = result
            FirebaseModule.matchesRef.child(matchId).updateChildren(updates).await()
            Log.i(TAG, "✅ Match[$matchId] updated → $status ${if (result != null) "with result: $result" else ""}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Match update failed: ${e.message}", e)
        }
    }
}
