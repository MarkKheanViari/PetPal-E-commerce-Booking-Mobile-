package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat

class ServiceHistoryAdapter(
    private val context: Context,
    private val services: List<HashMap<String, String>>
) : BaseAdapter() {

    override fun getCount(): Int = services.size

    override fun getItem(position: Int): Any = services[position]

    override fun getItemId(position: Int): Long = services[position]["id"]!!.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.service_history_item, parent, false)

        val serviceName = view.findViewById<TextView>(R.id.serviceName)
        val serviceDate = view.findViewById<TextView>(R.id.serviceDate)
        val serviceStatus = view.findViewById<Button>(R.id.serviceStatus)

        val service = services[position]

        serviceName.text = service["service_name"]
        serviceDate.text = "Date: ${service["selected_date"]}"

        val status = service["status"]!!.lowercase() // Convert to lowercase for consistency

        when (status) {
            "pending" -> {
                serviceStatus.text = "Pending"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))

                // ✅ Make button clickable for cancel request
                serviceStatus.setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Cancel Service Request?")
                        .setMessage("Are you sure you want to cancel this request?")
                        .setPositiveButton("Yes") { _, _ ->
                            (context as ServiceHistoryActivity).cancelServiceRequest(service["id"]!!.toInt()) // ✅ Calls cancel request function
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            "confirmed", "approved" -> {
                serviceStatus.text = "Approved"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            }
            "declined" -> {
                serviceStatus.text = "Declined"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            else -> {
                serviceStatus.text = "Unknown"  // ✅ Debugging log
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                Log.e("ServiceHistoryAdapter", "Unknown status received: $status") // ✅ Logs unknown status for debugging
            }
        }
        return view
    }

}
