package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ServiceHistoryAdapter(
    private val context: Context,
    private val services: List<HashMap<String, String>>,
    private val onCancelRequest: (Int) -> Unit // 🔥 Callback for canceling requests
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
        val cancelButton = view.findViewById<Button>(R.id.cancelButton) // ✅ Cancel Button

        val service = services[position]
        val status = service["status"]!!.lowercase()

        serviceName.text = service["service_name"]
        serviceDate.text = "Date: ${service["selected_date"]}"

        // 🔥 Handle status colors
        when (status) {
            "pending" -> {
                serviceStatus.text = "PENDING"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                cancelButton.visibility = View.VISIBLE

                // ✅ Handle Cancel Click
                cancelButton.setOnClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Cancel Service Request?")
                        .setMessage("Are you sure you want to cancel this request?")
                        .setPositiveButton("Yes") { _, _ ->
                            onCancelRequest(service["id"]!!.toInt()) // ✅ API Call
                            service["status"] = "cancelled" // ✅ Update the status in memory
                            notifyDataSetChanged() // ✅ Refresh UI
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
            "confirmed", "approved" -> {
                serviceStatus.text = "APPROVED"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                cancelButton.visibility = View.GONE
            }
            "declined" -> {
                serviceStatus.text = "DECLINED"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
                cancelButton.visibility = View.GONE
            }
            "cancelled" -> { // ✅ New Condition for Cancelled
                serviceStatus.text = "CANCELLED"
                serviceStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                cancelButton.visibility = View.GONE
            }
        }

        return view
    }

}



