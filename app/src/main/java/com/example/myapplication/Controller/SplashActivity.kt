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
        sharedPrefs.edit().putBoolean("hasSeenIntro", true).apply()
        val hasSeenIntro = sharedPrefs.getBoolean("hasSeenIntro", false)
        Log.d("SplashActivity", "hasSeenIntro = $hasSeenIntro")

        if (hasSeenIntro) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish()
    }
}
