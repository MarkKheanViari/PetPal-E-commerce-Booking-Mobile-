package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONObject

class ScheduledServicesActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var scheduledServicesAdapter: ScheduledServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled_services)

        // 1) Find and set up the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 2) Enable the "Up" (back) button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 3) Handle clicks on the navigation icon
        toolbar.setNavigationOnClickListener {
            onBackPressed() // or finish() if you prefer
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        scheduledServicesAdapter = ScheduledServicesAdapter(this)
        viewPager.adapter = scheduledServicesAdapter

        // Attach TabLayout and ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Grooming" else "Veterinary"
        }.attach()

        // Fetch scheduled appointments
        fetchScheduledAppointments()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scheduled_services_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("TOOLBAR_CLICK", "Clicked menu item: ${item.itemId}")
        return when (item.itemId) {
            R.id.menu_clear_history -> {
                Log.d("TOOLBAR_CLICK", "Clear History clicked!")
                showClearHistoryDialog()
                true
            }
            // Handle the Up/Back button click if you prefer this approach:
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Service History")
            .setMessage("Are you sure you want to clear all past services?")
            .setPositiveButton("Yes") { _, _ -> clearServiceHistory() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearServiceHistory() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.80.63/backend/clear_service_history.php"
        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            JSONObject().apply {
                put("mobile_user_id", mobileUserId)
            },
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(this, "✅ Service history cleared!", Toast.LENGTH_SHORT).show()
                    fetchScheduledAppointments() // Refresh UI
                } else {
                    Toast.makeText(this, "❌ Failed: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "❌ Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchScheduledAppointments() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.80.63/backend/fetch_appointments.php?mobile_user_id=$mobileUserId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    if (response.getBoolean("success")) {
                        val appointmentsArray = response.getJSONArray("appointments")
                        val appointmentsList = mutableListOf<Appointment>()

                        for (i in 0 until appointmentsArray.length()) {
                            val item = appointmentsArray.getJSONObject(i)
                            val appointment = Appointment(
                                serviceName = item.getString("service_name"),
                                serviceType = item.getString("service_type"),
                                appointmentDate = item.getString("appointment_date"),
                                price = item.getString("price"),
                                status = item.getString("status"),
                                image = item.optString("image", "")
                            )
                            appointmentsList.add(appointment)
                        }

                        // At this point, you’d pass appointmentsList to your adapter or handle it as needed
                        scheduledServicesAdapter.notifyDataSetChanged()

                    } else {
                        Toast.makeText(this, "No scheduled services found", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Error Parsing Data: ${e.message}", Toast.LENGTH_SHORT).show()
                    println("DEBUG: Error Parsing JSON: ${e.message}")
                }
            },
            { error ->
                Toast.makeText(this, "❌ Error Fetching Data: ${error.message}", Toast.LENGTH_SHORT).show()
                println("DEBUG: Volley Error: ${error.networkResponse?.statusCode} - ${error.message}")
            }
        )
        Volley.newRequestQueue(this).add(request)
    }
}
