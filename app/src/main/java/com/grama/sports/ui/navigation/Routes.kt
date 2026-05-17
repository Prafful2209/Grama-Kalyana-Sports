package com.grama.sports.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DASHBOARD = "dashboard"
    const val TOURNAMENT = "tournament"
    const val TOURNAMENT_LIST = "tournament_list"
    const val MATCH_SCHEDULER = "match_scheduler"
    const val TEAM_MANAGEMENT = "team_management"
    const val PLAYER_MANAGEMENT = "player_management"
    const val LIVE_SCORE = "live_score/{matchId}/{sportType}"
    const val FAN_VIEW = "fan_view/{matchId}/{sportType}"
    const val PLAYERS = "players"
    const val CAREER_RECORDS = "career_records"
    const val SETTINGS = "settings"

    // Helper functions for navigation
    fun liveScore(matchId: String, sportType: String) = "live_score/$matchId/$sportType"
    fun fanView(matchId: String, sportType: String) = "fan_view/$matchId/$sportType"

    // Compatibility helpers for different versions of the code
    fun createLiveScoreRoute(matchId: String, sportType: String) = liveScore(matchId, sportType)
    fun createFanViewRoute(matchId: String) = "fan_view/$matchId/General"
}
