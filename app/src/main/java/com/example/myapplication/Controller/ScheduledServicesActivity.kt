package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ScheduledServicesActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var scheduledServicesAdapter: ScheduledServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled_services)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        scheduledServicesAdapter = ScheduledServicesAdapter(this)
        viewPager.adapter = scheduledServicesAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Grooming" else "Veterinary"
        }.attach()

        fetchScheduledAppointments()
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
