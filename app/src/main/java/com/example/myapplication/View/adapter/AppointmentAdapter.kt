package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class AppointmentAdapter(
    private val context: Context,
    private var appointments: MutableList<Appointment>,
    private val onAppointmentCanceled: () -> Unit // Callback to refresh data after canceling
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceName: TextView = view.findViewById(R.id.serviceName)
        val appointmentDate: TextView = view.findViewById(R.id.appointmentDate)
        val price: TextView = view.findViewById(R.id.price)
        val status: TextView = view.findViewById(R.id.status) // ✅ Status TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.appointment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.serviceName.text = appointment.serviceName
        holder.appointmentDate.text = appointment.appointmentDate
        holder.price.text = "₱${appointment.price}"
        holder.status.text = appointment.status

        // ✅ Change text color based on status
        when (appointment.status.lowercase()) {
            "pending" -> holder.status.setTextColor(Color.parseColor("#FFD700")) // Yellow
            "declined" -> holder.status.setTextColor(Color.parseColor("#FF0000")) // Red
            "approved" -> holder.status.setTextColor(Color.parseColor("#4CAF50")) // Green
            else -> holder.status.setTextColor(Color.parseColor("#757575")) // Default Gray
        }

        // ✅ Make the appointment clickable for cancellation
        holder.itemView.setOnClickListener {
            if (appointment.status.lowercase() == "approved" || appointment.status.lowercase() == "pending") {
                showCancelDialog(appointment, position)
            } else {
                Toast.makeText(context, "❌ You can't cancel a declined or completed appointment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = appointments.size

    // ✅ Show confirmation dialog before canceling an appointment
    private fun showCancelDialog(appointment: Appointment, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Cancel Appointment")
            .setMessage("You will cancel your appointment for ${appointment.serviceName}. Do you want to proceed?")
            .setPositiveButton("Yes") { _, _ -> cancelAppointment(appointment, position) }
            .setNegativeButton("No", null)
            .show()
    }

    // ✅ Send cancellation request to backend using appointment details
    private fun cancelAppointment(appointment: Appointment, position: Int) {
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1) // ✅ Get user ID from SharedPreferences

        if (mobileUserId == -1) {
            Toast.makeText(context, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.1.65/backend/cancel_service_request.php"

        val requestData = JSONObject().apply {
            put("mobile_user_id", mobileUserId) // ✅ Sending user ID
            put("service_name", appointment.serviceName)
            put("service_type", appointment.serviceType)
            put("appointment_date", appointment.appointmentDate)
        }

        Log.d("CANCEL_REQUEST", "Sending request: $requestData") // ✅ Debugging

        val request = JsonObjectRequest(Request.Method.POST, url, requestData,
            { response ->
                Log.d("CANCEL_RESPONSE", "Response: $response") // ✅ Debugging

                if (response.getBoolean("success")) {
                    Toast.makeText(context, "✅ Appointment canceled!", Toast.LENGTH_SHORT).show()
                    appointments[position] = appointment.copy(status = "Declined") // ✅ Mark as Declined instead of removing
                    notifyItemChanged(position)
                } else {
                    Toast.makeText(context, "❌ Failed to cancel: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("CANCEL_ERROR", "❌ Error: ${error.message}")
                Toast.makeText(context, "❌ Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        Volley.newRequestQueue(context).add(request)
    }
}
