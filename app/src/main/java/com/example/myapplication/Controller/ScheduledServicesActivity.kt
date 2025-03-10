package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar  // ✅ Correct Toolbar Import
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // ✅ Fix: Now it correctly uses AppCompat Toolbar


        Log.d("TOOLBAR_SETUP", "Toolbar has been set as Support ActionBar") // ✅ Debugging

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        scheduledServicesAdapter = ScheduledServicesAdapter(this)
        viewPager.adapter = scheduledServicesAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Grooming" else "Veterinary"
        }.attach()

        fetchScheduledAppointments()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scheduled_services_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("TOOLBAR_CLICK", "Clicked menu item: ${item.itemId}") // ✅ Debugging

        return when (item.itemId) {
            R.id.menu_clear_history -> {
                Log.d("TOOLBAR_CLICK", "Clear History clicked!") // ✅ Confirm click is detected
                showClearHistoryDialog()
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

        val url = "http://192.168.1.12/backend/clear_service_history.php"

        val request = JsonObjectRequest(Request.Method.POST, url, JSONObject().apply {
            put("mobile_user_id", mobileUserId)
        }, { response ->
            if (response.getBoolean("success")) {
                Toast.makeText(this, "✅ Service history cleared!", Toast.LENGTH_SHORT).show()
                fetchScheduledAppointments() // Refresh UI
            } else {
                Toast.makeText(this, "❌ Failed: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
            }
        }, { error ->
            Toast.makeText(this, "❌ Network error: ${error.message}", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(request)
    }

    private fun fetchScheduledAppointments() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Debug: Log user_id to ensure correct data is being sent
        println("DEBUG: Fetching appointments for user_id: $mobileUserId")

        val url = "http://192.168.1.12/backend/fetch_appointments.php?mobile_user_id=$mobileUserId"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
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
                                status = item.getString("status")
                            )
                            appointmentsList.add(appointment)
                        }

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
