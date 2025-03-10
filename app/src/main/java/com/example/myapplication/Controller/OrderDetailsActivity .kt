package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class OrderDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details) // Ensure this matches your layout name

        // Find the toolbar defined in your XML
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Optional: Enable the home "up" button if you want default behavior too
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set the click listener for the navigation icon (back button)
        toolbar.setNavigationOnClickListener {
            // This will finish the current activity and go back to the previous one
            finish()
        }
    }
}
