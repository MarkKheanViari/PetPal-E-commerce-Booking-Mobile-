package com.example.myapplication

import Service
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
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

class ServiceAdapter(
    private val context: Context, // ✅ Fix: Use Context instead of MainActivity
    private var services: MutableList<Service>, // ✅ Fix: Ensure services is properly initialized
    private val onAvailClick: (Service, String) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = services.size

    override fun getItem(position: Int): Any = services[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.service_item, parent, false)

        try {
            val service = services[position]

            val serviceName = view.findViewById<TextView>(R.id.serviceName)
            val serviceDescription = view.findViewById<TextView>(R.id.serviceDescription)
            val statusText = view.findViewById<TextView>(R.id.statusText)
            val availButton = view.findViewById<Button>(R.id.availButton)
            val selectDateButton = view.findViewById<Button>(R.id.selectDateButton)
            val selectedDateText = view.findViewById<TextView>(R.id.selectedDateText)

            serviceName.text = service.serviceName
            serviceDescription.text = service.description
            statusText.text = "Status: ${service.status}"

            // Hide select date options initially
            selectDateButton.visibility = View.GONE
            selectedDateText.visibility = View.GONE

            availButton.setOnClickListener {
                val intent = Intent(context, ServiceAvailActivity::class.java)
                intent.putExtra("SERVICE_NAME", service.serviceName)
                intent.putExtra("SERVICE_ID", service.id) // ✅ Ensure service ID is passed
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }





            selectDateButton.setOnClickListener {
                showDatePicker(service, selectedDateText)
            }

        } catch (e: Exception) {
            Log.e("ServiceAdapter", "Error setting up view", e)
            Toast.makeText(context, "Error displaying service: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun showDatePicker(service: Service, selectedDateText: TextView) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 0) // Start from today

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                selectedDateText.text = "Selected Date: $selectedDate"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
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
