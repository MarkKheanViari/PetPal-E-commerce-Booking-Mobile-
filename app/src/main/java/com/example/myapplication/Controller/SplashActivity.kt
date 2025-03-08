package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Check if the user has seen the intro screens.
        val hasSeenIntro = sharedPrefs.getBoolean("hasSeenIntro", false)

        // Check if the user is already logged in.
        val isLoggedIn = sharedPrefs.getBoolean("isLoggedIn", false)

        Log.d("SplashActivity", "hasSeenIntro = $hasSeenIntro, isLoggedIn = $isLoggedIn")

        when {
            isLoggedIn -> {
                // User is already logged in, go directly to the main activity.
                startActivity(Intent(this, MainActivity::class.java))
            }
            hasSeenIntro -> {
                // User has seen intro but not logged in, go to login.
                startActivity(Intent(this, LoginActivity::class.java))
            }
            else -> {
                // First time user, show onboarding.
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
        }
        finish()
    }
}
