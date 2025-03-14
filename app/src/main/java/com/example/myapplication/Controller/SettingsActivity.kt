package com.example.myapplication.Controller

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the settings layout (make sure the file name is settings.xml)
        setContentView(R.layout.settings)

        // Connect the toolbar's back button
        val backButton: ImageView = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish() // Return to the previous screen when tapped
        }

        // Optionally, you can set up click listeners for other settings items.
        // For example:
        // val profileDetails: TextView = findViewById(R.id.profile_details)
        // profileDetails.setOnClickListener {
        //     // Launch edit profile activity
        // }
    }
}
