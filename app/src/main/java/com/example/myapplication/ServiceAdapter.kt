package com.example.myapplication

import android.app.DatePickerDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import Service

class ServiceAdapter(
    private val context: MainActivity,
    private var services: MutableList<Service>,
    private val onAvailClick: (Service, String) -> Unit
) : BaseAdapter() {

    private val client = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getCount(): Int = services.size

    override fun getItem(position: Int): Any = services[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.service_item, parent, false)

        try {
            val service = services[position]

            view.findViewById<TextView>(R.id.serviceName).text = service.serviceName
            view.findViewById<TextView>(R.id.serviceDescription).text = service.description

            val availButton = view.findViewById<Button>(R.id.availButton)
            val statusText = view.findViewById<TextView>(R.id.statusText)

            val userId = context.getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
                .getInt("user_id", -1)

            when {
                service.userId == userId && service.status.isNotEmpty() -> {
                    availButton.visibility = View.GONE
                    statusText.apply {
                        visibility = View.VISIBLE
                        text = "Status: ${service.status}"
                        setTextColor(when (service.status.lowercase()) {
                            "pending" -> context.getColor(android.R.color.holo_orange_dark)
                            "confirmed" -> context.getColor(android.R.color.holo_green_dark)
                            "declined" -> context.getColor(android.R.color.holo_red_dark)
                            else -> context.getColor(android.R.color.darker_gray)
                        })
                    }
                }
                service.status.isNotEmpty() -> {
                    availButton.visibility = View.GONE
                    statusText.apply {
                        visibility = View.VISIBLE
                        text = "Not Available"
                        setTextColor(context.getColor(android.R.color.darker_gray))
                    }
                }
                else -> {
                    availButton.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            if (userId != -1) {
                                showDatePicker(service)
                            } else {
                                Toast.makeText(context, "Please login to avail services", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    statusText.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("ServiceAdapter", "Error setting up view", e)
            Toast.makeText(context, "Error displaying service: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun showDatePicker(service: Service) {
        val calendar = Calendar.getInstance()

        // Don't allow past dates
        calendar.add(Calendar.DAY_OF_MONTH, 0) // Start from today

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                checkDateAvailability(service, selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun checkDateAvailability(service: Service, selectedDate: String) {
        val jsonObject = JSONObject().apply {
            put("service_id", service.id)
            put("selected_date", selectedDate)
        }

        Log.d("ServiceAdapter", "Checking availability for date: $selectedDate")
        Log.d("ServiceAdapter", "Request body: ${jsonObject.toString()}")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.161.55/backend/check_date_availability.php")
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ServiceAdapter", "Network error", e)
                context.runOnUiThread {
                    Toast.makeText(
                        context,
                        "Failed to check date availability: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseString = response.body?.string()
                    Log.d("ServiceAdapter", "Raw response: $responseString")

                    if (responseString.isNullOrEmpty()) {
                        throw Exception("Empty response from server")
                    }

                    val jsonResponse = JSONObject(responseString)

                    context.runOnUiThread {
                        if (jsonResponse.optBoolean("available", false)) {
                            // Show confirmation dialog
                            android.app.AlertDialog.Builder(context)
                                .setTitle("Confirm Service Request")
                                .setMessage("Would you like to request this service for $selectedDate?")
                                .setPositiveButton("Yes") { _, _ ->
                                    onAvailClick(service, selectedDate)
                                }
                                .setNegativeButton("No", null)
                                .show()
                        } else {
                            val message = jsonResponse.optString("message", "Date is not available")
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ServiceAdapter", "Error processing response", e)
                    context.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Error processing response: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    fun clear() {
        services.clear()
        notifyDataSetChanged()
    }

    fun updateServices(newServices: List<Service>) {
        services.clear()
        services.addAll(newServices)
        notifyDataSetChanged()
    }
}