package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.AppNavigation
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped/failed", e)
        }

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.w(TAG, "enableEdgeToEdge failed", e)
        }

        setContent {
            AppNavigation()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
