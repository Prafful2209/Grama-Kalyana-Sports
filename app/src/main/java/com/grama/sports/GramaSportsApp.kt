package com.grama.sports

import android.app.Application
import com.google.firebase.FirebaseApp

class GramaSportsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
