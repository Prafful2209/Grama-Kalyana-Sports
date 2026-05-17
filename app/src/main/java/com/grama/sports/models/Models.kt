package com.grama.sports.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "fan", // "admin" or "fan"
    val village: String = ""
)

@IgnoreExtraProperties
data class Tournament(
    val id: String = "",
    val name: String = "",
    val village: String = "",
    val date: String = "",
    val status: String = "upcoming", // upcoming, live, completed
    val sportType: String = "Cricket",
    val bannerUrl: String = "",
    val description: String = ""
)

@IgnoreExtraProperties
data class Team(
    val id: String = "",
    val name: String = "",
    val village: String = "",
    val logoUrl: String = "",
    val tournamentId: String = "",
    val sportType: String = "Cricket",
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)

@IgnoreExtraProperties
data class Player(
    val id: String = "",
    val name: String = "",
    val age: Int = 0,
    val teamId: String = "",
    val teamName: String = "",
    val jerseyNumber: Int = 0,
    val sportType: String = "Cricket",
    val role: String = "", 
    val photoUrl: String = "",

    // All-time career stats - Cricket
    val cricketRuns: Int = 0,
    val cricketBalls: Int = 0,
    val cricketFours: Int = 0,
    val cricketSixes: Int = 0,
    val cricketMatches: Int = 0,
    val cricketNotOuts: Int = 0,
    val cricketHighScore: Int = 0,
    val cricketWickets: Int = 0,
    val cricketOversBowled: String = "0.0",
    val cricketMaidens: Int = 0,
    val cricketRunsConceded: Int = 0,

    // All-time career stats - Kabaddi
    val kabaddiRaidPoints: Int = 0,
    val kabaddiTacklePoints: Int = 0,
    val kabaddiBonusPoints: Int = 0,
    val kabaddiSuperRaids: Int = 0,
    val kabaddiSuperTackles: Int = 0,
    val kabaddiMatches: Int = 0,

    // All-time career stats - Volleyball
    val volleyballPoints: Int = 0,
    val volleyballAces: Int = 0,
    val volleyballBlocks: Int = 0,
    val volleyballAttacks: Int = 0,
    val volleyballMatches: Int = 0,

    val totalPoints: Int = 0,
    val totalMatches: Int = 0
)

@IgnoreExtraProperties
data class Match(
    val id: String = "",
    val tournamentId: String = "",
    val teamAId: String = "",
    val teamBId: String = "",
    val teamAName: String = "",
    val teamBName: String = "",
    val sportType: String = "Cricket",
    val status: String = "scheduled", // scheduled, live, halftime, paused, completed
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val result: String = "", 
    val tossWinner: String = "",
    val electedTo: String = "", 
    val maxOvers: Int = 20,
    val teamAPlayerIds: List<String> = emptyList(),
    val teamBPlayerIds: List<String> = emptyList(),
    val winnerId: String = ""
)

@IgnoreExtraProperties
data class BatsmanLiveStats(
    val name: String = "",
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val outDesc: String = ""
)

@IgnoreExtraProperties
data class BowlerLiveStats(
    val name: String = "",
    val runs: Int = 0,
    val balls: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0
)

@IgnoreExtraProperties
data class KabaddiPlayerLiveStats(
    val name: String = "",
    val raidPoints: Int = 0,
    val tacklePoints: Int = 0,
    val bonusPoints: Int = 0,
    val superRaids: Int = 0,
    val superTackles: Int = 0,
    val isOnField: Boolean = true
)

@IgnoreExtraProperties
data class VolleyballPlayerLiveStats(
    val name: String = "",
    val points: Int = 0,
    val aces: Int = 0,
    val blocks: Int = 0,
    val attacks: Int = 0,
    val serviceErrors: Int = 0,
    val defensiveSaves: Int = 0
)

@IgnoreExtraProperties
data class LiveScore(
    val matchId: String = "",
    val teamAName: String = "Team A",
    val teamBName: String = "Team B",
    val sportType: String = "Cricket",
    val statusText: String = "Match Not Started",
    val timestamp: Long = System.currentTimeMillis(),

    val teamAScore: Int = 0,
    val teamBScore: Int = 0,

    // ============ CRICKET ============
    val teamAWickets: Int = 0,
    val teamBWickets: Int = 0,
    val teamAOvers: String = "0.0",
    val teamBOvers: String = "0.0",
    val currentInnings: Int = 1,
    val ballsInOver: Int = 0,
    val tossWinner: String = "",
    val electedTo: String = "", 
    val targetScore: Int = 0,
    val maxOvers: Int = 20,
    val extras: Int = 0,
    val wide: Int = 0,
    val noBall: Int = 0,
    val byes: Int = 0,
    val legByes: Int = 0,
    val strikerName: String = "Striker",
    val nonStrikerName: String = "Non-Striker",
    val bowlerName: String = "Bowler",
    val strikerRuns: Int = 0,
    val strikerBalls: Int = 0,
    val strikerFours: Int = 0,
    val strikerSixes: Int = 0,
    val nonStrikerRuns: Int = 0,
    val nonStrikerBalls: Int = 0,
    val nonStrikerFours: Int = 0,
    val nonStrikerSixes: Int = 0,
    val bowlerRunsConceded: Int = 0,
    val bowlerBallsBowled: Int = 0,
    val bowlerWickets: Int = 0,
    val batsmenStats: Map<String, BatsmanLiveStats> = emptyMap(),
    val bowlersStats: Map<String, BowlerLiveStats> = emptyMap(),
    val outPlayers: List<String> = emptyList(),

    // ============ VOLLEYBALL ============
    val teamASets: Int = 0,
    val teamBSets: Int = 0,
    val currentSet: Int = 1,
    val teamASetScores: String = "", // e.g. "25,19"
    val teamBSetScores: String = "", // e.g. "22,25"
    val servingTeam: String = "", // "A" or "B"
    val volleyballPlayerStats: Map<String, VolleyballPlayerLiveStats> = emptyMap(),

    // ============ KABADDI ============
    val teamARaidPoints: Int = 0,
    val teamBRaidPoints: Int = 0,
    val teamABonusPoints: Int = 0,
    val teamBBonusPoints: Int = 0,
    val teamATacklePoints: Int = 0,
    val teamBTacklePoints: Int = 0,
    val teamASuperTackles: Int = 0,
    val teamBSuperTackles: Int = 0,
    val teamAAllOuts: Int = 0,
    val teamBAllOuts: Int = 0,
    val teamAOnFieldCount: Int = 7,
    val teamBOnFieldCount: Int = 7,
    val timer: String = "20:00",
    val isTimerRunning: Boolean = false,
    val raidTimer: Int = 30,
    val currentHalf: Int = 1, // 1 or 2
    val currentRaiderName: String = "",
    val raidingTeam: String = "A", // "A" or "B"
    val teamAOutQueue: List<String> = emptyList(),
    val teamBOutQueue: List<String> = emptyList(),
    val kabaddiPlayerStats: Map<String, KabaddiPlayerLiveStats> = emptyMap()
)

@IgnoreExtraProperties
data class CricketScore(
    val matchId: String = "",
    val teamAScore: Int = 0,
    val teamAWickets: Int = 0,
    val teamAOvers: String = "0.0",
    val teamBScore: Int = 0,
    val teamBWickets: Int = 0,
    val teamBOvers: String = "0.0",
    val currentInnings: Int = 1
)
