package com.grama.sports.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grama.sports.data.firebase.FirebaseModule
import com.grama.sports.models.*
import com.grama.sports.repository.SportsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Stack

class AppViewModel : ViewModel() {
    private val auth = FirebaseModule.auth
    private val repository = SportsRepository
    private val scoreHistory = Stack<LiveScore>()
    private var observationJob: Job? = null
    private var liveScoreJob: Job? = null
    private var timerJob: Job? = null

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isRealAdmin = MutableStateFlow(auth.currentUser != null && !auth.currentUser!!.isAnonymous)
    
    private val _isAdmin = MutableStateFlow(auth.currentUser != null && !auth.currentUser!!.isAnonymous)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _liveMatches = MutableStateFlow<List<Match>>(emptyList())
    val liveMatches: StateFlow<List<Match>> = _liveMatches

    private val _upcomingMatches = MutableStateFlow<List<Match>>(emptyList())
    val upcomingMatches: StateFlow<List<Match>> = _upcomingMatches

    private val _completedMatches = MutableStateFlow<List<Match>>(emptyList())
    val completedMatches: StateFlow<List<Match>> = _completedMatches

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _currentLiveScore = MutableStateFlow(LiveScore())
    val currentLiveScore: StateFlow<LiveScore> = _currentLiveScore

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val loggedIn = user != null
            _isLoggedIn.value = loggedIn
            val realAdmin = user != null && !user.isAnonymous
            _isRealAdmin.value = realAdmin
            _isAdmin.value = realAdmin 
            startObservingAllData()
        }
        startObservingAllData()
    }

    fun toggleAdminMode() {
        if (_isRealAdmin.value) {
            _isAdmin.value = !_isAdmin.value
        }
    }

    // ================================================================
    // AUTHENTICATION
    // ================================================================
    fun loginWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(true, null)
            else onResult(false, task.exception?.message)
        }
    }

    fun registerWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) onResult(true, null)
            else onResult(false, task.exception?.message)
        }
    }

    fun loginAnonymously(onResult: (Boolean) -> Unit) {
        auth.signInAnonymously().addOnCompleteListener { task -> 
            if (task.isSuccessful) {
                _isLoggedIn.value = true
                _isAdmin.value = false
            }
            onResult(task.isSuccessful) 
        }
    }

    fun logout() { 
        auth.signOut() 
        _isLoggedIn.value = false
        _isAdmin.value = false
    }

    // ================================================================
    // REALTIME DATA OBSERVATION
    // ================================================================
    private fun startObservingAllData() {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            _isLoading.value = true
            launch {
                repository.getMatches().collect { allMatches ->
                    _liveMatches.value = allMatches.filter { it.status.lowercase() == "live" }
                    _upcomingMatches.value = allMatches.filter {
                        it.status.lowercase() in listOf("scheduled", "upcoming")
                    }
                    _completedMatches.value = allMatches.filter { it.status.lowercase() == "completed" }
                    _isLoading.value = false
                }
            }
            launch {
                repository.getTournaments().collect { list -> _tournaments.value = list }
            }
            launch {
                repository.getTeams().collect { list -> _teams.value = list }
            }
            launch {
                repository.getPlayers().collect { list -> _players.value = list }
            }
        }
    }

    fun observeLiveScore(matchId: String) {
        liveScoreJob?.cancel()
        _currentLiveScore.value = LiveScore(matchId = matchId, statusText = "Connecting...")
        liveScoreJob = viewModelScope.launch {
            repository.observeLiveScore(matchId).collect { score ->
                if (score != null) {
                    val current = _currentLiveScore.value
                    // Preserve local timer state if we are running the clock as admin
                    if (timerJob?.isActive == true) {
                        _currentLiveScore.value = score.copy(
                            timer = current.timer,
                            raidTimer = current.raidTimer,
                            isTimerRunning = true
                        )
                    } else {
                        _currentLiveScore.value = score
                    }
                }
            }
        }
    }

    // ================================================================
    // MATCH MANAGEMENT
    // ================================================================
    fun createMatch(
        teamAId: String, teamBId: String,
        teamAName: String, teamBName: String,
        sport: String, date: String, venue: String,
        time: String = "", tournamentId: String = "",
        maxOvers: Int = 20,
        teamAPlayerIds: List<String> = emptyList(),
        teamBPlayerIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val match = Match(
                teamAId = teamAId, teamBId = teamBId,
                teamAName = teamAName, teamBName = teamBName,
                sportType = sport, date = date, time = time,
                venue = venue, status = "scheduled",
                tournamentId = tournamentId,
                maxOvers = maxOvers,
                teamAPlayerIds = teamAPlayerIds,
                teamBPlayerIds = teamBPlayerIds
            )
            repository.saveMatch(match)
            _isSaving.value = false
        }
    }

    fun rescheduleMatch(matchId: String, newDate: String, newTime: String, newVenue: String) {
        viewModelScope.launch {
            val match = _upcomingMatches.value.find { it.id == matchId } ?: return@launch
            val updatedMatch = match.copy(date = newDate, time = newTime, venue = newVenue)
            repository.saveMatch(updatedMatch)
        }
    }

    fun startMatch(match: Match) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.saveMatch(match.copy(status = "live"))
            val initialScore = LiveScore(
                matchId = match.id, 
                teamAName = match.teamAName,
                teamBName = match.teamBName,
                sportType = match.sportType,
                statusText = "Match Started", 
                tossWinner = match.tossWinner, 
                electedTo = match.electedTo,
                maxOvers = match.maxOvers,
                timer = if(match.sportType.contains("Kaba", ignoreCase = true)) "20:00" else "00:00",
                raidingTeam = "A",
                raidTimer = 30
            )
            repository.saveLiveScore(initialScore)
            _isSaving.value = false
        }
    }

    fun deleteMatch(matchId: String) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
        }
    }

    fun endMatch(matchId: String, result: String = "") {
        viewModelScope.launch {
            repository.updateMatchStatus(matchId, "completed", result)
            val final = _currentLiveScore.value.copy(statusText = result.ifBlank { "Match Completed" }, isTimerRunning = false)
            repository.saveLiveScore(final)
        }
    }

    fun createTournament(name: String, village: String, date: String, sportType: String) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.saveTournament(Tournament(name = name, village = village, date = date, sportType = sportType))
            _isSaving.value = false
        }
    }

    fun createTeam(name: String, village: String, sportType: String = "Cricket") {
        viewModelScope.launch {
            _isSaving.value = true
            repository.saveTeam(Team(name = name, village = village, sportType = sportType))
            _isSaving.value = false
        }
    }

    fun deleteTeam(teamId: String) {
        viewModelScope.launch {
            repository.deleteTeam(teamId)
        }
    }

    fun createPlayer(name: String, age: Int, teamId: String, teamName: String, jersey: Int, sportType: String = "Cricket") {
        viewModelScope.launch {
            _isSaving.value = true
            repository.savePlayer(Player(name = name, age = age, teamId = teamId, teamName = teamName, jerseyNumber = jersey, sportType = sportType))
            _isSaving.value = false
        }
    }

    fun deletePlayer(playerId: String) {
        viewModelScope.launch {
            repository.deletePlayer(playerId)
        }
    }

    // ================================================================
    // CRICKET SCORING LOGIC
    // ================================================================
    fun updateCricketScore(runs: Int, isWicket: Boolean = false, extraType: String? = null) {
        val current = _currentLiveScore.value
        val match = _liveMatches.value.find { it.id == current.matchId } 
            ?: _upcomingMatches.value.find { it.id == current.matchId }
            
        val isFirstInnings = current.currentInnings == 1

        var teamScore = if (isFirstInnings) current.teamAScore else current.teamBScore
        var teamWickets = if (isFirstInnings) current.teamAWickets else current.teamBWickets
        var currentOversStr = if (isFirstInnings) current.teamAOvers else current.teamBOvers
        var ballsInOver = current.ballsInOver
        var extras = current.extras
        var wide = current.wide; var noBall = current.noBall
        var byes = current.byes; var legByes = current.legByes
        
        var sRuns = current.strikerRuns; var sBalls = current.strikerBalls
        var sFours = current.strikerFours; var sSixes = current.strikerSixes
        var sName = current.strikerName
        
        var nsRuns = current.nonStrikerRuns; var nsBalls = current.nonStrikerBalls
        var nsFours = current.nonStrikerFours; var nsSixes = current.nonStrikerSixes
        var nsName = current.nonStrikerName
        
        var bName = current.bowlerName
        var bRuns = current.bowlerRunsConceded
        var bBalls = current.bowlerBallsBowled
        var bWickets = current.bowlerWickets
        
        val outPlayersList = current.outPlayers.toMutableList()
        val batsmenMap = current.batsmenStats.toMutableMap()
        val bowlersMap = current.bowlersStats.toMutableMap()

        when (extraType) {
            "Wide" -> { 
                teamScore += 1; extras += 1; wide += 1
                bRuns += 1 
            }
            "No Ball" -> { 
                teamScore += 1 + runs; extras += 1; noBall += 1
                sRuns += runs
                bRuns += 1 + runs
            }
            "Bye" -> { 
                teamScore += runs; extras += runs; byes += runs
                ballsInOver++; sBalls++; bBalls++
            }
            "Leg Bye" -> { 
                teamScore += runs; extras += runs; legByes += runs
                ballsInOver++; sBalls++; bBalls++
            }
            null -> {
                teamScore += runs; ballsInOver++
                sBalls++; sRuns += runs
                if (runs == 4) sFours++
                if (runs == 6) sSixes++
                
                bBalls++
                bRuns += runs
                
                if (isWicket) {
                    teamWickets++
                    bWickets++
                    if (sName != "OUT" && sName != "Striker") {
                        outPlayersList.add(sName)
                        batsmenMap[sName] = BatsmanLiveStats(sName, sRuns, sBalls, sFours, sSixes, true, "Wicket")
                    }
                    sName = "OUT"
                }
            }
        }

        if (sName != "OUT" && sName != "Striker") {
            batsmenMap[sName] = BatsmanLiveStats(sName, sRuns, sBalls, sFours, sSixes, false)
        }
        if (nsName != "OUT" && nsName != "Non-Striker") {
            batsmenMap[nsName] = batsmenMap[nsName] ?: BatsmanLiveStats(nsName, nsRuns, nsBalls, nsFours, nsSixes, false)
        }
        if (bName != "OUT" && bName != "Bowler") {
            bowlersMap[bName] = BowlerLiveStats(bName, bRuns, bBalls, bWickets)
        }

        var shouldRotate = false
        var currentOverCount = currentOversStr.split(".")[0].toIntOrNull() ?: 0
        if (ballsInOver >= 6) {
            currentOverCount++
            currentOversStr = "$currentOverCount.0"; ballsInOver = 0
            shouldRotate = true
            bName = "OUT" 
        } else if (extraType != "Wide" && extraType != "No Ball") {
            currentOversStr = "$currentOverCount.$ballsInOver"
        }
        
        if (!isWicket && sName != "OUT" && runs % 2 != 0 && (extraType == null || extraType == "No Ball" || extraType == "Bye" || extraType == "Leg Bye")) {
            shouldRotate = !shouldRotate
        }

        if (shouldRotate && sName != "OUT") {
            val tName = sName; sName = nsName; nsName = tName
            val tRuns = sRuns; sRuns = nsRuns; nsRuns = tRuns
            val tBalls = sBalls; sBalls = nsBalls; nsBalls = tBalls
            val tFours = sFours; sFours = nsFours; nsFours = tFours
            val tSixes = sSixes; sSixes = nsSixes; nsSixes = tSixes
        }

        var updated = current.copy(
            teamAScore = if (isFirstInnings) teamScore else current.teamAScore,
            teamBScore = if (!isFirstInnings) teamScore else current.teamBScore,
            teamAWickets = if (isFirstInnings) teamWickets else current.teamAWickets,
            teamBWickets = if (!isFirstInnings) teamWickets else current.teamBWickets,
            teamAOvers = if (isFirstInnings) currentOversStr else current.teamAOvers,
            teamBOvers = if (!isFirstInnings) currentOversStr else current.teamBOvers,
            ballsInOver = ballsInOver,
            extras = extras, wide = wide, noBall = noBall, byes = byes, legByes = legByes,
            strikerRuns = sRuns, strikerBalls = sBalls, strikerFours = sFours, strikerSixes = sSixes,
            strikerName = sName, nonStrikerName = nsName, bowlerName = bName,
            nonStrikerRuns = nsRuns, nonStrikerBalls = nsBalls, nonStrikerFours = nsFours, nonStrikerSixes = nsSixes,
            bowlerRunsConceded = bRuns, bowlerBallsBowled = bBalls, bowlerWickets = bWickets,
            outPlayers = outPlayersList, batsmenStats = batsmenMap, bowlersStats = bowlersMap,
            statusText = buildStatusText(runs, isWicket, extraType)
        )

        val battingPlayerCount = if (isFirstInnings) {
            match?.teamAPlayerIds?.size?.takeIf { it > 0 } ?: 11
        } else {
            match?.teamBPlayerIds?.size?.takeIf { it > 0 } ?: 11
        }
        val maxWickets = battingPlayerCount - 1
        
        val isOversLimitReached = currentOverCount >= current.maxOvers && ballsInOver == 0
        val isAllOut = teamWickets >= maxWickets
        val isTargetReached = !isFirstInnings && teamScore >= current.targetScore

        if (isOversLimitReached || isAllOut || isTargetReached) {
            if (isFirstInnings) {
                val target = teamScore + 1
                updated = updated.copy(
                    currentInnings = 2, targetScore = target, ballsInOver = 0, teamBOvers = "0.0",
                    strikerRuns = 0, strikerBalls = 0, strikerFours = 0, strikerSixes = 0,
                    nonStrikerRuns = 0, nonStrikerBalls = 0, nonStrikerFours = 0, nonStrikerSixes = 0,
                    bowlerRunsConceded = 0, bowlerBallsBowled = 0, bowlerWickets = 0,
                    strikerName = "OUT", nonStrikerName = "OUT", bowlerName = "OUT",
                    outPlayers = emptyList(), batsmenStats = emptyMap(), bowlersStats = emptyMap(),
                    statusText = "Innings Ended. Target: $target"
                )
            } else {
                val result = if (isTargetReached) {
                    val teamB = match?.teamBName ?: current.teamBName
                    val wicketsLeft = (maxWickets + 1) - teamWickets
                    "$teamB won by $wicketsLeft wickets"
                } else if (teamScore == current.targetScore - 1) {
                    "Match Tied"
                } else {
                    val teamA = match?.teamAName ?: current.teamAName
                    val runsMargin = current.targetScore - 1 - teamScore
                    "$teamA won by $runsMargin runs"
                }
                viewModelScope.launch { endMatch(current.matchId, result) }
                return
            }
        }

        scoreHistory.push(current)
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }
    
    fun updateCricketPlayers(striker: String, nonStriker: String, bowler: String) {
        val current = _currentLiveScore.value
        val batsmenMap = current.batsmenStats.toMutableMap()
        val bowlersMap = current.bowlersStats.toMutableMap()
        
        if (current.strikerName != "OUT" && current.strikerName != "Striker") {
            batsmenMap[current.strikerName] = BatsmanLiveStats(
                current.strikerName, current.strikerRuns, current.strikerBalls, current.strikerFours, current.strikerSixes, false
            )
        }
        if (current.nonStrikerName != "OUT" && current.nonStrikerName != "Non-Striker") {
            batsmenMap[current.nonStrikerName] = BatsmanLiveStats(
                current.nonStrikerName, current.nonStrikerRuns, current.nonStrikerBalls, current.nonStrikerFours, current.nonStrikerSixes, false
            )
        }
        if (current.bowlerName != "OUT" && current.bowlerName != "Bowler") {
            bowlersMap[current.bowlerName] = BowlerLiveStats(
                current.bowlerName, current.bowlerRunsConceded, current.bowlerBallsBowled, current.bowlerWickets
            )
        }
        
        val sStats = batsmenMap[striker] ?: BatsmanLiveStats(name = striker)
        val nsStats = batsmenMap[nonStriker] ?: BatsmanLiveStats(name = nonStriker)
        val bStats = bowlersMap[bowler] ?: BowlerLiveStats(name = bowler)

        val updated = current.copy(
            strikerName = striker,
            nonStrikerName = nonStriker,
            bowlerName = bowler,
            strikerRuns = sStats.runs, strikerBalls = sStats.balls, strikerFours = sStats.fours, strikerSixes = sStats.sixes,
            nonStrikerRuns = nsStats.runs, nonStrikerBalls = nsStats.balls, nonStrikerFours = nsStats.fours, nonStrikerSixes = nsStats.sixes,
            bowlerRunsConceded = bStats.runs, bowlerBallsBowled = bStats.balls, bowlerWickets = bStats.wickets,
            batsmenStats = batsmenMap, bowlersStats = bowlersMap
        )
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    fun switchInnings() {
        val current = _currentLiveScore.value
        if (current.currentInnings != 1) return
        val target = current.teamAScore + 1
        val updated = current.copy(
            currentInnings = 2, targetScore = target, ballsInOver = 0, teamBOvers = "0.0",
            strikerRuns = 0, strikerBalls = 0, strikerFours = 0, strikerSixes = 0,
            nonStrikerRuns = 0, nonStrikerBalls = 0, nonStrikerFours = 0, nonStrikerSixes = 0,
            bowlerRunsConceded = 0, bowlerBallsBowled = 0, bowlerWickets = 0,
            strikerName = "OUT", nonStrikerName = "OUT", bowlerName = "OUT",
            outPlayers = emptyList(), batsmenStats = emptyMap(), bowlersStats = emptyMap(),
            statusText = "Innings 2 Started — Target: $target"
        )
        scoreHistory.push(current)
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    private fun buildStatusText(runs: Int, isWicket: Boolean, extra: String?): String = when {
        isWicket -> "WICKET!"
        extra != null -> "$extra"
        runs == 6 -> "SIX! 🏏"
        runs == 4 -> "FOUR! 🏏"
        else -> "$runs Run${if (runs != 1) "s" else ""}"
    }

    // ================================================================
    // KABADDI PROFESSIONAL SCORING LOGIC
    // ================================================================
    fun updateKabaddiScore(isTeamA: Boolean, raid: Int, tackle: Int, bonus: Int, isAllOut: Boolean) {
        val current = _currentLiveScore.value
        val isRaidingTeam = (isTeamA && current.raidingTeam == "A") || (!isTeamA && current.raidingTeam == "B")
        
        if (isAllOut) {
            handleKabaddiAction(isTeamA, "All Out")
        } else if (raid > 0) {
            handleKabaddiAction(isTeamA, "Raid", points = raid)
        } else if (tackle > 0) {
            handleKabaddiAction(isTeamA, "Tackle")
        } else if (bonus > 0) {
            handleKabaddiAction(isTeamA, "Bonus")
        } else if (raid == 0 && !isAllOut && isRaidingTeam) {
            handleKabaddiAction(isTeamA, "Empty Raid")
        }
    }
    
    fun setRaidingTeam(team: String) {
        _currentLiveScore.update { it.copy(raidingTeam = team, raidTimer = 30) }
        viewModelScope.launch { repository.saveLiveScore(_currentLiveScore.value) }
    }
    
    fun setCurrentRaider(name: String) {
        _currentLiveScore.update { it.copy(currentRaiderName = name) }
        viewModelScope.launch { repository.saveLiveScore(_currentLiveScore.value) }
    }
    
    fun resetRaidTimer() {
        _currentLiveScore.update { it.copy(raidTimer = 30) }
        viewModelScope.launch { repository.saveLiveScore(_currentLiveScore.value) }
    }

    fun handleKabaddiAction(
        isTeamA: Boolean,
        actionType: String,
        points: Int = 0,
        playerName: String = ""
    ) {
        val current = _currentLiveScore.value
        scoreHistory.push(current.copy())
        
        var teamAScore = current.teamAScore
        var teamBScore = current.teamBScore
        var teamARaid = current.teamARaidPoints
        var teamBRaid = current.teamBRaidPoints
        var teamABonus = current.teamABonusPoints
        var teamBBonus = current.teamBBonusPoints
        var teamATackle = current.teamATacklePoints
        var teamBTackle = current.teamBTacklePoints
        var teamASuperTackles = current.teamASuperTackles
        var teamBSuperTackles = current.teamBSuperTackles
        var teamAAllOuts = current.teamAAllOuts
        var teamBAllOuts = current.teamBAllOuts
        var teamAOnField = current.teamAOnFieldCount
        var teamBOnField = current.teamBOnFieldCount
        var teamAQueue = current.teamAOutQueue.toMutableList()
        var teamBQueue = current.teamBOutQueue.toMutableList()
        val playerStats = current.kabaddiPlayerStats.toMutableMap()
        
        val actualPlayerName = if (playerName.isNotBlank()) playerName else current.currentRaiderName
        
        var status = ""
        var switchRaider = false

        when (actionType) {
            "Raid" -> {
                switchRaider = true
                if (isTeamA) { // Team A Raiding successfully
                    teamAScore += points; teamARaid += points
                    teamBOnField = (teamBOnField - points).coerceAtLeast(0)
                    repeat(points) { teamBQueue.add("Opponent") }
                    val revived = points.coerceAtMost(teamAQueue.size)
                    repeat(revived) { if (teamAQueue.isNotEmpty()) { teamAQueue.removeAt(0); teamAOnField++ } }
                    status = "${current.teamAName} Raid: +$points"
                    if (actualPlayerName.isNotBlank()) {
                        val p = playerStats[actualPlayerName] ?: KabaddiPlayerLiveStats(name = actualPlayerName)
                        playerStats[actualPlayerName] = p.copy(raidPoints = p.raidPoints + points)
                    }
                } else { // Team B Raiding successfully
                    teamBScore += points; teamBRaid += points
                    teamAOnField = (teamAOnField - points).coerceAtLeast(0)
                    repeat(points) { teamAQueue.add("Opponent") }
                    val revived = points.coerceAtMost(teamBQueue.size)
                    repeat(revived) { if (teamBQueue.isNotEmpty()) { teamBQueue.removeAt(0); teamBOnField++ } }
                    status = "${current.teamBName} Raid: +$points"
                    if (actualPlayerName.isNotBlank()) {
                        val p = playerStats[actualPlayerName] ?: KabaddiPlayerLiveStats(name = actualPlayerName)
                        playerStats[actualPlayerName] = p.copy(raidPoints = p.raidPoints + points)
                    }
                }
            }
            "Bonus" -> {
                if (isTeamA) { 
                    teamAScore += 1; teamABonus += 1; status = "${current.teamAName} Bonus" 
                    if (actualPlayerName.isNotBlank()) {
                        val p = playerStats[actualPlayerName] ?: KabaddiPlayerLiveStats(name = actualPlayerName)
                        playerStats[actualPlayerName] = p.copy(bonusPoints = p.bonusPoints + 1)
                    }
                } else { 
                    teamBScore += 1; teamBBonus += 1; status = "${current.teamBName} Bonus"
                    if (actualPlayerName.isNotBlank()) {
                        val p = playerStats[actualPlayerName] ?: KabaddiPlayerLiveStats(name = actualPlayerName)
                        playerStats[actualPlayerName] = p.copy(bonusPoints = p.bonusPoints + 1)
                    }
                }
            }
            "Tackle" -> { 
                switchRaider = true
                if (isTeamA) { // Team A Defender catches Team B raider
                    val isSuper = teamAOnField <= 3
                    val pts = if (isSuper) 2 else 1
                    teamAScore += pts; teamATackle += pts
                    if (isSuper) teamASuperTackles++
                    teamBOnField = (teamBOnField - 1).coerceAtLeast(0); teamBQueue.add("Raider")
                    if (teamAQueue.isNotEmpty()) { teamAQueue.removeAt(0); teamAOnField++ }
                    status = if(isSuper) "SUPER TACKLE! ${current.teamAName}" else "${current.teamAName} Tackle"
                } else { // Team B Defender catches Team A raider
                    val isSuper = teamBOnField <= 3
                    val pts = if (isSuper) 2 else 1
                    teamBScore += pts; teamBTackle += pts
                    if (isSuper) teamBSuperTackles++
                    teamAOnField = (teamAOnField - 1).coerceAtLeast(0); teamAQueue.add("Raider")
                    if (teamBQueue.isNotEmpty()) { teamBQueue.removeAt(0); teamBOnField++ }
                    status = if(isSuper) "SUPER TACKLE! ${current.teamBName}" else "${current.teamBName} Tackle"
                }
            }
            "Raider Out" -> { // Called when raider is caught or time ends
                switchRaider = true
                if (isTeamA) { // Team A raider is OUT -> Team B scores
                    teamBScore += 1; teamBTackle += 1; teamAOnField = (teamAOnField - 1).coerceAtLeast(0); teamAQueue.add("Raider")
                    if (teamBQueue.isNotEmpty()) { teamBQueue.removeAt(0); teamBOnField++ }
                    status = "${current.teamAName} Raider Out"
                } else { // Team B raider is OUT -> Team A scores
                    teamAScore += 1; teamATackle += 1; teamBOnField = (teamBOnField - 1).coerceAtLeast(0); teamBQueue.add("Raider")
                    if (teamAQueue.isNotEmpty()) { teamAQueue.removeAt(0); teamAOnField++ }
                    status = "${current.teamBName} Raider Out"
                }
            }
            "Empty Raid" -> {
                switchRaider = true
                status = "Empty Raid"
            }
            "All Out" -> {
                if (isTeamA) { teamAScore += 2; teamAAllOuts++; teamBOnField = 7; teamBQueue.clear(); status = "ALL OUT! ${current.teamBName} (+2)" }
                else { teamBScore += 2; teamBAllOuts++; teamAOnField = 7; teamAQueue.clear(); status = "ALL OUT! ${current.teamAName} (+2)" }
            }
            "Out of Bounds" -> {
                if (isTeamA) { 
                    teamBScore += 1; teamAOnField = (teamAOnField - 1).coerceAtLeast(0); teamAQueue.add("Player")
                    if (teamBQueue.isNotEmpty()) { teamBQueue.removeAt(0); teamBOnField++ }
                    status = "${current.teamAName} Out of Bounds"
                } else {
                    teamAScore += 1; teamBOnField = (teamBOnField - 1).coerceAtLeast(0); teamBQueue.add("Player")
                    if (teamAQueue.isNotEmpty()) { teamAQueue.removeAt(0); teamAOnField++ }
                    status = "${current.teamBName} Out of Bounds"
                }
            }
        }
        
        if (teamBOnField == 0 && actionType != "All Out") {
            teamAScore += 2; teamAAllOuts++; teamBOnField = 7; teamBQueue.clear(); status = "ALL OUT! ${current.teamBName} (+2)"
        } else if (teamAOnField == 0 && actionType != "All Out") {
            teamBScore += 2; teamBAllOuts++; teamAOnField = 7; teamAQueue.clear(); status = "ALL OUT! ${current.teamAName} (+2)"
        }
        
        val updated = current.copy(
            teamAScore = teamAScore, teamBScore = teamBScore,
            teamARaidPoints = teamARaid, teamBRaidPoints = teamBRaid,
            teamABonusPoints = teamABonus, teamBBonusPoints = teamBBonus,
            teamATacklePoints = teamATackle, teamBTacklePoints = teamBTackle,
            teamASuperTackles = teamASuperTackles, teamBSuperTackles = teamBSuperTackles,
            teamAAllOuts = teamAAllOuts, teamBAllOuts = teamBAllOuts,
            teamAOnFieldCount = teamAOnField, teamBOnFieldCount = teamBOnField,
            teamAOutQueue = teamAQueue, teamBOutQueue = teamBQueue,
            raidingTeam = if (switchRaider) (if (current.raidingTeam == "A") "B" else "A") else current.raidingTeam,
            statusText = status,
            raidTimer = if (switchRaider) 30 else current.raidTimer,
            kabaddiPlayerStats = playerStats,
            currentRaiderName = if (switchRaider) "" else current.currentRaiderName
        )
        _currentLiveScore.value = updated
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    fun switchKabaddiHalf() {
        val current = _currentLiveScore.value
        scoreHistory.push(current.copy())
        
        timerJob?.cancel()
        timerJob = null
        
        val updated = current.copy(
            currentHalf = if (current.currentHalf == 1) 2 else 1,
            timer = "20:00",
            statusText = "Half Switched",
            raidingTeam = "A", 
            raidTimer = 30,
            isTimerRunning = false,
            currentRaiderName = ""
        )
        _currentLiveScore.value = updated
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    // ================================================================
    // VOLLEYBALL PROFESSIONAL SCORING LOGIC
    // ================================================================
    fun updateVolleyballScore(isTeamA: Boolean) {
        handleVolleyballPoint(isTeamA, "Point")
    }

    fun endVolleyballSet() {
        val current = _currentLiveScore.value
        val winA = current.teamAScore > current.teamBScore
        val newASets = if (winA) current.teamASets + 1 else current.teamASets
        val newBSets = if (!winA) current.teamBSets + 1 else current.teamBSets
        
        val setScoreA = if (current.teamASetScores.isEmpty()) "${current.teamAScore}" else "${current.teamASetScores},${current.teamAScore}"
        val setScoreB = if (current.teamBSetScores.isEmpty()) "${current.teamBScore}" else "${current.teamBSetScores},${current.teamBScore}"
        
        val isMatchOver = newASets >= 3 || newBSets >= 3
        
        if (isMatchOver) {
            val winner = if (newASets >= 3) current.teamAName else current.teamBName
            endMatch(current.matchId, "$winner Won the Match!")
        } else {
            val updated = current.copy(
                teamAScore = 0, teamBScore = 0,
                teamASets = newASets, teamBSets = newBSets,
                currentSet = current.currentSet + 1,
                teamASetScores = setScoreA, teamBSetScores = setScoreB,
                statusText = "Set ${current.currentSet} Ended"
            )
            viewModelScope.launch { repository.saveLiveScore(updated) }
        }
    }

    fun handleVolleyballPoint(isTeamA: Boolean, pointType: String, playerName: String = "") {
        val current = _currentLiveScore.value
        scoreHistory.push(current.copy())
        
        var teamAScore = current.teamAScore
        var teamBScore = current.teamBScore
        val playerStats = current.volleyballPlayerStats.toMutableMap()
        val pStat = playerStats[playerName] ?: VolleyballPlayerLiveStats(name = playerName)
        
        if (isTeamA) {
            teamAScore += 1
            when (pointType) {
                "Ace" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, aces = pStat.aces + 1)
                "Kill" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, attacks = pStat.attacks + 1)
                "Block" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, blocks = pStat.blocks + 1)
            }
        } else {
            teamBScore += 1
            when (pointType) {
                "Ace" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, aces = pStat.aces + 1)
                "Kill" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, attacks = pStat.attacks + 1)
                "Block" -> playerStats[playerName] = pStat.copy(points = pStat.points + 1, blocks = pStat.blocks + 1)
            }
        }
        
        var updated = current.copy(
            teamAScore = teamAScore,
            teamBScore = teamBScore,
            volleyballPlayerStats = playerStats,
            servingTeam = if (isTeamA) "A" else "B",
            statusText = "Point ${if(isTeamA) current.teamAName else current.teamBName} ($pointType)"
        )
        
        val targetPoints = if (current.currentSet >= 5) 15 else 25 
        val isSetOver = (teamAScore >= targetPoints || teamBScore >= targetPoints) && 
                         Math.abs(teamAScore - teamBScore) >= 2
                         
        if (isSetOver) {
            val winA = teamAScore > teamBScore
            val newASets = if (winA) current.teamASets + 1 else current.teamASets
            val newBSets = if (!winA) current.teamBSets + 1 else current.teamBSets
            
            val setScoreA = if (current.teamASetScores.isEmpty()) "$teamAScore" else "${current.teamASetScores},$teamAScore"
            val setScoreB = if (current.teamBSetScores.isEmpty()) "$teamBScore" else "${current.teamBSetScores},$teamBScore"
            
            val isMatchOver = newASets >= 3 || newBSets >= 3 // Best of 5
            
            if (isMatchOver) {
                val winner = if (newASets >= 3) current.teamAName else current.teamBName
                viewModelScope.launch { endMatch(current.matchId, "$winner Won the Match!") }
            } else {
                updated = updated.copy(
                    teamAScore = 0, teamBScore = 0,
                    teamASets = newASets, teamBSets = newBSets,
                    currentSet = current.currentSet + 1,
                    teamASetScores = setScoreA, teamBSetScores = setScoreB,
                    statusText = "Set ${current.currentSet} Won by ${if(winA) current.teamAName else current.teamBName}"
                )
            }
        }
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    // ================================================================
    // GENERAL MATCH CONTROLS
    // ================================================================
    fun toggleTimer(matchId: String) {
        if (timerJob?.isActive == true) {
            timerJob?.cancel()
            timerJob = null
            _currentLiveScore.update { it.copy(isTimerRunning = false) }
            viewModelScope.launch { repository.saveLiveScore(_currentLiveScore.value) }
        } else {
            timerJob = viewModelScope.launch {
                var currentState = _currentLiveScore.value
                
                // Ensure timer is initialized for Kabaddi if it's currently empty or invalid
                if (currentState.sportType.contains("Kaba", ignoreCase = true) && (currentState.timer.isBlank() || !currentState.timer.contains(":"))) {
                    currentState = currentState.copy(timer = "20:00")
                }
                
                val parts = currentState.timer.split(":")
                if (parts.size != 2) return@launch
                
                var currentMin = parts[0].toIntOrNull() ?: 20
                var currentSec = parts[1].toIntOrNull() ?: 0
                var currentRaidTimer = currentState.raidTimer
                
                // Set running state locally and on server
                _currentLiveScore.update { it.copy(isTimerRunning = true, timer = String.format(Locale.ROOT, "%02d:%02d", currentMin, currentSec)) }
                repository.saveLiveScore(_currentLiveScore.value)

                while (true) {
                    delay(1000)
                    
                    // Re-fetch latest to pick up score/raidingTeam changes from other functions
                    val latest = _currentLiveScore.value
                    val isKabaddi = latest.sportType.contains("Kaba", ignoreCase = true)
                    
                    // If an external action (button press) reset the raid timer to 30, sync our local variable
                    if (latest.raidTimer == 30) {
                        currentRaidTimer = 30
                    }

                    if (currentSec > 0) {
                        currentSec--
                    } else if (currentMin > 0) {
                        currentMin--
                        currentSec = 59
                    } else {
                        // Timer reached 00:00
                        if (isKabaddi) {
                            if (latest.currentHalf == 1) {
                                val halfTime = latest.copy(timer = "20:00", currentHalf = 2, isTimerRunning = false, statusText = "Halftime")
                                _currentLiveScore.update { halfTime }
                                repository.saveLiveScore(halfTime)
                            } else {
                                val res = if (latest.teamAScore > latest.teamBScore) "${latest.teamAName} Won" else if (latest.teamBScore > latest.teamAScore) "${latest.teamBName} Won" else "Match Tied"
                                endMatch(latest.matchId, res)
                            }
                        }
                        timerJob = null
                        break
                    }

                    if (isKabaddi && currentRaidTimer > 0) {
                        currentRaidTimer--
                        if (currentRaidTimer == 0) {
                            handleKabaddiAction(latest.raidingTeam == "A", "Empty Raid")
                            currentRaidTimer = 30
                        }
                    }

                    val updatedTimerStr = String.format(Locale.ROOT, "%02d:%02d", currentMin, currentSec)
                    
                    // Update state and preserve isTimerRunning = true to keep the loop going
                    _currentLiveScore.update { 
                        it.copy(
                            timer = updatedTimerStr,
                            raidTimer = currentRaidTimer,
                            isTimerRunning = true
                        ) 
                    }
                    
                    // Push to Firebase in background to avoid blocking the timer tick
                    val stateToSave = _currentLiveScore.value
                    launch { repository.saveLiveScore(stateToSave) }
                }
            }
        }
    }

    fun resetTimer(matchId: String) {
        timerJob?.cancel()
        timerJob = null
        val c = _currentLiveScore.value
        val updated = c.copy(
            timer = if(c.sportType.contains("Kaba", ignoreCase = true)) "20:00" else "00:00", 
            isTimerRunning = false, 
            raidTimer = 30,
            currentRaiderName = ""
        )
        _currentLiveScore.value = updated
        viewModelScope.launch { repository.saveLiveScore(updated) }
    }

    fun undoScore() {
        if (scoreHistory.isNotEmpty()) {
            val previous = scoreHistory.pop()
            _currentLiveScore.value = previous
            viewModelScope.launch { repository.saveLiveScore(previous) }
        }
    }
}
