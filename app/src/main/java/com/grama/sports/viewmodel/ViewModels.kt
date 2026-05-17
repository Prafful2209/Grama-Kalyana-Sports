package com.grama.sports.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.grama.sports.data.firebase.FirebaseModule
import com.grama.sports.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Legacy ViewModels — kept for backward compatibility only.
 * The main app now uses AppViewModel exclusively.
 *
 * ROOT CAUSE FIX for data-not-visible bug:
 * Old ScoreViewModel used separate paths per sport:
 *   liveScores/cricket/{matchId}
 *   liveScores/volleyball/{matchId}
 *   liveScores/kabaddi/{matchId}
 *
 * AppViewModel + SportsRepository use a SINGLE unified path:
 *   liveScores/{matchId}
 *
 * This mismatch caused admin writes to be invisible to fans.
 * All scoring now goes through AppViewModel only.
 */

class AuthViewModel : ViewModel() {
    private val auth = FirebaseModule.auth
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun loginAnonymously(onResult: (Boolean) -> Unit) {
        auth.signInAnonymously().addOnCompleteListener {
            _isLoggedIn.value = it.isSuccessful
            onResult(it.isSuccessful)
        }
    }

    fun loginWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoggedIn.value = task.isSuccessful
            onResult(task.isSuccessful, task.exception?.message)
        }
    }

    fun registerWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            _isLoggedIn.value = task.isSuccessful
            onResult(task.isSuccessful, task.exception?.message)
        }
    }

    fun logout() { auth.signOut(); _isLoggedIn.value = false }
}

class DashboardViewModel : ViewModel() {
    private val _liveMatches = MutableStateFlow<List<Match>>(emptyList())
    val liveMatches: StateFlow<List<Match>> = _liveMatches

    init {
        // FIX: Removed orderByChild("status").equalTo("Live") because it's CASE SENSITIVE.
        // Status is stored as "live" (lowercase). The old filter equalTo("Live") matched nothing,
        // causing fan screens to show empty lists. Now we listen to ALL matches and filter in memory.
        FirebaseModule.matchesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val all = snapshot.children.mapNotNull { it.getValue(Match::class.java) }
                _liveMatches.value = all.filter { it.status.lowercase() == "live" }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

class TournamentViewModel : ViewModel() {
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments

    init {
        FirebaseModule.tournamentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _tournaments.value = snapshot.children.mapNotNull { it.getValue(Tournament::class.java) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun createTournament(name: String, village: String, date: String, sportType: String) {
        val id = FirebaseModule.tournamentsRef.push().key ?: return
        FirebaseModule.tournamentsRef.child(id).setValue(
            Tournament(id = id, name = name, village = village, date = date, sportType = sportType)
        )
    }
}

/**
 * ScoreViewModel — FIXED to use UNIFIED path: liveScores/{matchId}
 * Previously used sport-specific sub-paths which caused fan sync failure.
 * Now delegates to AppViewModel-compatible paths via FirebaseModule.liveScoresRef.
 */
class ScoreViewModel : ViewModel() {
    private val _cricketScore = MutableStateFlow(CricketScore())
    val cricketScore: StateFlow<CricketScore> = _cricketScore

    fun observeCricketScore(matchId: String) {
        // FIXED PATH: was liveScores/cricket/{matchId} → now liveScores/{matchId}
        FirebaseModule.liveScoresRef.child(matchId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue(LiveScore::class.java) ?: return
                _cricketScore.value = CricketScore(
                    matchId = score.matchId,
                    teamAScore = score.teamAScore, teamAWickets = score.teamAWickets,
                    teamAOvers = score.teamAOvers, teamBScore = score.teamBScore,
                    teamBWickets = score.teamBWickets, teamBOvers = score.teamBOvers,
                    currentInnings = score.currentInnings
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateCricketScore(score: CricketScore) {
        // FIXED PATH: unified liveScores/{matchId}
        FirebaseModule.liveScoresRef.child(score.matchId).updateChildren(
            mapOf(
                "teamAScore" to score.teamAScore,
                "teamAWickets" to score.teamAWickets,
                "teamAOvers" to score.teamAOvers,
                "teamBScore" to score.teamBScore,
                "teamBWickets" to score.teamBWickets,
                "teamBOvers" to score.teamBOvers,
                "currentInnings" to score.currentInnings
            )
        )
    }
}

class PlayerViewModel : ViewModel() {
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    init {
        FirebaseModule.playersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _players.value = snapshot.children.mapNotNull { it.getValue(Player::class.java) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addPlayer(name: String, age: Int, jersey: Int) {
        val id = FirebaseModule.playersRef.push().key ?: return
        FirebaseModule.playersRef.child(id).setValue(Player(id = id, name = name, age = age, jerseyNumber = jersey))
    }
}
