package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the settings layout
        setContentView(R.layout.settings)
        // Optionally, you can set up a toolbar or other settings UI elements here.
    }
}
