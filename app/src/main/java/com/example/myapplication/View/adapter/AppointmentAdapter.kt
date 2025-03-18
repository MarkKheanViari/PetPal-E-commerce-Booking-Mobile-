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
        val status: TextView = view.findViewById(R.id.status)
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

        // ✅ Clickable only for "Approved" & "Pending"
        holder.itemView.setOnClickListener {
            if (appointment.status.lowercase() == "approved" || appointment.status.lowercase() == "pending") {
                showCancelDialog(appointment, position)
            } else {
                Toast.makeText(context, "❌ You can't cancel a Declined appointment.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = appointments.size

    // ✅ Show confirmation dialog before canceling an appointment
    private fun showCancelDialog(appointment: Appointment, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel your appointment for ${appointment.serviceName}?")
            .setPositiveButton("Yes") { _, _ -> cancelAppointment(appointment, position) }
            .setNegativeButton("No", null)
            .show()
    }

    // ✅ Send cancellation request to backend
    private fun cancelAppointment(appointment: Appointment, position: Int) {
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(context, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.1.12/backend/cancel_service_request.php"

        val requestData = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
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

                    // ✅ Update status in list instead of removing
                    appointments[position] = appointment.copy(status = "Declined")
                    notifyItemChanged(position) // ✅ Refresh UI for this item

                    // ✅ Call the callback function to refresh the parent list
                    onAppointmentCanceled()
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
