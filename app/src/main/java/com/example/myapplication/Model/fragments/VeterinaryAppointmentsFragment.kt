package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class VeterinaryAppointmentsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointmentList = mutableListOf<Appointment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_veterinary_appointments, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        appointmentAdapter = AppointmentAdapter(requireContext(), appointmentList) {
            fetchAppointments() // ✅ Refresh list after cancellation
        }
        recyclerView.adapter = appointmentAdapter

        fetchAppointments()

        return view
    }

    private fun fetchAppointments() {
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(requireContext(), "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.1.65/backend/fetch_appointments.php?mobile_user_id=$mobileUserId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                Log.d("API_RESPONSE", "Veterinary Appointments: $response") // ✅ Debug API response

                if (response.getBoolean("success")) {
                    val appointmentsArray = response.getJSONArray("appointments")
                    appointmentList.clear()

                    for (i in 0 until appointmentsArray.length()) {
                        val item = appointmentsArray.getJSONObject(i)

                        // ✅ Only fetch Veterinary appointments
                        if (item.getString("service_type") == "Veterinary") {
                            val appointment = Appointment(
                                serviceName = item.getString("service_name"),
                                serviceType = item.getString("service_type"),
                                appointmentDate = item.getString("appointment_date"),
                                price = item.getString("price"),
                                status = item.getString("status"),
                                image = item.optString("image", "")
                            )
                            appointmentList.add(appointment)
                        }
                    }

                    appointmentAdapter.notifyDataSetChanged() // ✅ Refresh RecyclerView
                } else {
                    Toast.makeText(requireContext(), "No veterinary appointments found", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("API_ERROR", "❌ Veterinary Fetch Error: ${error.message}")
                Toast.makeText(requireContext(), "❌ Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(requireContext()).add(request)
    }
}
