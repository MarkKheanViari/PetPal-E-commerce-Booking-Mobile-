package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
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
        val serviceImage: ImageView = view.findViewById(R.id.serviceImage)
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

        // Load image
        Glide.with(context)
            .load("http://192.168.1.12/backend/${appointment.image}")
            .placeholder(R.drawable.cat)
            .into(holder.serviceImage)

        // ✅ Update status colors, including "Cancelled"
        when (appointment.status.lowercase()) {
            "pending" -> holder.status.setTextColor(Color.parseColor("#FFD700"))
            "declined" -> holder.status.setTextColor(Color.parseColor("#FF0000"))
            "approved" -> holder.status.setTextColor(Color.parseColor("#4CAF50"))
            "cancelled" -> holder.status.setTextColor(Color.parseColor("#FF4500")) // OrangeRed for Cancelled
            else -> holder.status.setTextColor(Color.parseColor("#757575"))
        }

        // Show details dialog on click
        holder.itemView.setOnClickListener {
            showAppointmentDetailsDialog(appointment, position)
        }
    }

    override fun getItemCount(): Int = appointments.size

    // Show a dialog with appointment details
    private fun showAppointmentDetailsDialog(appointment: Appointment, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Appointment Details")

        // Build the message with all relevant details
        val details = StringBuilder()
        details.append("Service: ${appointment.serviceName}\n")
        details.append("Type: ${appointment.serviceType}\n")
        details.append("Date: ${appointment.appointmentDate}\n")
        details.append("Price: ₱${appointment.price}\n")
        details.append("Status: ${appointment.status}\n")
        if (!appointment.petName.isNullOrEmpty()) {
            details.append("Pet Name: ${appointment.petName}\n")
        }
        if (!appointment.petBreed.isNullOrEmpty()) {
            details.append("Pet Breed: ${appointment.petBreed}\n")
        }
        if (!appointment.notes.isNullOrEmpty()) {
            details.append("Notes: ${appointment.notes}\n")
        }
        if (!appointment.paymentMethod.isNullOrEmpty()) {
            details.append("Payment Method: ${appointment.paymentMethod}\n")
        }

        builder.setMessage(details.toString())

        // Add a "Cancel Appointment" button if status is "Pending" or "Approved"
        if (appointment.status.lowercase() in listOf("approved", "pending")) {
            builder.setNegativeButton("Cancel Appointment") { _, _ ->
                showCancelDialog(appointment, position)
            }
        }

        builder.setPositiveButton("OK") { _, _ -> /* Dismiss dialog */ }
        builder.show()
    }

    // Show confirmation dialog before canceling an appointment
    private fun showCancelDialog(appointment: Appointment, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel your appointment for ${appointment.serviceName}?")
            .setPositiveButton("Yes") { _, _ -> cancelAppointment(appointment, position) }
            .setNegativeButton("No", null)
            .show()
    }

    // Send cancellation request to backend
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

        Log.d("CANCEL_REQUEST", "Sending request: $requestData") // Debugging

        val request = JsonObjectRequest(Request.Method.POST, url, requestData,
            { response ->
                Log.d("CANCEL_RESPONSE", "Response: $response") // Debugging

                if (response.getBoolean("success")) {
                    Toast.makeText(context, "✅ Appointment canceled!", Toast.LENGTH_SHORT).show()

                    // ✅ Check the action taken by the backend
                    val action = response.getString("action")
                    if (action == "deleted") {
                        // If Pending, remove the item from the list
                        appointments.removeAt(position)
                        notifyItemRemoved(position)
                    } else if (action == "updated") {
                        // If Approved, update the status to Cancelled
                        appointments[position] = appointment.copy(status = "Cancelled")
                        notifyItemChanged(position)
                    }

                    // Call the callback to refresh the parent list
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