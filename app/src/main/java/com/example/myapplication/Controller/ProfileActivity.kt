package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        // ✅ Find the "Scheduled Services" button
        val scheduledServices = findViewById<TextView>(R.id.scheduled_services)

        // ✅ Set click listener to open ScheduledServicesActivity
        scheduledServices.setOnClickListener {
            val intent = Intent(this, ScheduledServicesActivity::class.java)
            startActivity(intent)
        }
    }
}
