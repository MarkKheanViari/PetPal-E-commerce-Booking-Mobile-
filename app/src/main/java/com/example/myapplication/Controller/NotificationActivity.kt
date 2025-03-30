package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class NotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

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

        // Set up the RecyclerView with the custom adapter
        val recyclerView = findViewById<RecyclerView>(R.id.notificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = NotificationAdapter(storedNotifications)

        // Handle empty state view
        val emptyContainer = findViewById<LinearLayout>(R.id.emptyContainer)
        if (storedNotifications.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyContainer.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyContainer.visibility = View.GONE
        }
    }

    // RecyclerView Adapter using the notif_item layout
    class NotificationAdapter(private val notifications: List<String>) :
        RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val notifInfo: TextView = itemView.findViewById(R.id.notifInfo)
            val notifContent: TextView = itemView.findViewById(R.id.notifContent)
            val notifDate: TextView = itemView.findViewById(R.id.notifDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.notif_item, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            try {
                val notificationJson = JSONObject(notifications[position])
                holder.notifInfo.text = notificationJson.optString("info", "Info")
                holder.notifContent.text = notificationJson.optString("content", "Content")
                holder.notifDate.text = notificationJson.optString("date", "Date")
            } catch (e: Exception) {
                // Fallback: display the raw string as notification content if JSON parsing fails
                holder.notifInfo.text = "Notification"
                holder.notifContent.text = notifications[position]
                holder.notifDate.text = ""
            }
        }

        override fun getItemCount(): Int = notifications.size
    }
}
