package com.grama.sports.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseModule {
    private const val TAG = "FirebaseModule"
    
    // Explicitly using the regional URL to ensure Admin and Fan modules hit the same instance
    private const val DATABASE_URL = "https://gramakalyanasports-44ff0-default-rtdb.asia-southeast1.firebasedatabase.app"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    val database: FirebaseDatabase by lazy { 
        Log.d(TAG, "Initializing Firebase Database: $DATABASE_URL")
        val db = FirebaseDatabase.getInstance(DATABASE_URL)
        try { 
            db.setPersistenceEnabled(true) 
        } catch (e: Exception) { 
            Log.w(TAG, "Persistence already enabled")
        }
        db
    }

    // Shared nodes for both Admin and Fan
    val tournamentsRef by lazy { database.getReference("tournaments") }
    val teamsRef by lazy { database.getReference("teams") }
    val playersRef by lazy { database.getReference("players") }
    val matchesRef by lazy { database.getReference("matches") }
    val liveScoresRef by lazy { database.getReference("liveScores") }
}
