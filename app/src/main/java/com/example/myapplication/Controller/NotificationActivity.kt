package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.json.JSONArray

class NotificationActivity : AppCompatActivity() {

    private lateinit var backBtn : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        /*
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = "Notifications"
        toolbar.setNavigationOnClickListener { onBackPressed() }
         */

        backBtn = findViewById(R.id.backBtn)

        backBtn.setOnClickListener {
            finish()
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
