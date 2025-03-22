package com.example.myapplication

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.Controller.WelcomeActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Check if the user is already logged in.
        val isLoggedIn = sharedPrefs.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // User is logged in, go directly to MainActivity.
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not logged in, show the onboarding (intro fragment) first.
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish() // Prevents returning to the SplashActivity when pressing back.
    }
}
