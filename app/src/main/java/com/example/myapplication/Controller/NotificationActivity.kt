package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Show the back arrow and hide the default title
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        // When the navigation (up) button is clicked, go back
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Load stored notifications from SharedPreferences
        val sp = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val jsonString = sp.getString("notifications", "[]")
        val storedNotifications = mutableListOf<String>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            storedNotifications.add(jsonArray.getString(i))
        }

        val listView = findViewById<ListView>(R.id.notificationsListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, storedNotifications)
        listView.adapter = adapter
    }
}
