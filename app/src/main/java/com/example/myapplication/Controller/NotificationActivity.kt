package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // Setup the toolbar and its navigation click
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Create a list of dummy notifications.
        // Replace these with your actual notifications as needed.
        val notifications = listOf(
            "Order Confirmed: Your order for 'Product X' has been confirmed.",
            "Service Scheduled: Your grooming appointment is set for 2025-03-20.",
            "Update: New features have been added to the app."
        )

        // Setup the ListView to display notifications
        val listView = findViewById<ListView>(R.id.notificationsListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, notifications)
        listView.adapter = adapter
    }
}
