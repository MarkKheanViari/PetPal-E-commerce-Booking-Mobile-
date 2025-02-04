package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class ServiceAvailActivity : AppCompatActivity() {

    private lateinit var serviceNameText: TextView
    private lateinit var customerNameInput: EditText
    private lateinit var daySpinner: Spinner
    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var contactNumberInput: EditText
    private lateinit var submitButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        Log.d("DEBUG", "User ID Retrieved: $userId")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_avail)  // ✅ Ensure correct XML is loaded

        // ✅ Link UI elements correctly
        serviceNameText = findViewById(R.id.serviceNameText)
        customerNameInput = findViewById(R.id.customerNameInput)
        daySpinner = findViewById(R.id.daySpinner)
        monthSpinner = findViewById(R.id.monthSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        contactNumberInput = findViewById(R.id.contactNumberInput)
        submitButton = findViewById(R.id.submitButton)

        // ✅ Get service details
        val serviceName = intent.getStringExtra("SERVICE_NAME") ?: "Unknown Service"
        val serviceId = intent.getIntExtra("SERVICE_ID", -1)
        serviceNameText.text = serviceName

        // ✅ Populate dropdowns
        populateDateFields()

        // ✅ Handle submit button click
        submitButton.setOnClickListener {
            val customerName = customerNameInput.text.toString()
            val selectedDay = daySpinner.selectedItem.toString()
            val selectedMonth = monthSpinner.selectedItem.toString()
            val selectedYear = yearSpinner.selectedItem.toString()
            val contactNumber = contactNumberInput.text.toString()

            val selectedDate = "$selectedYear-$selectedMonth-$selectedDay"

            // ✅ Get user ID from SharedPreferences
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val userId = sharedPreferences.getInt("user_id", -1)

            if (userId == -1 || serviceId == -1) {
                Toast.makeText(this, "Error: Missing user or service data", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ✅ Send request to backend
            sendServiceRequest(serviceId, userId, selectedDate)
        }
    }

    private fun populateDateFields() {
        val days = (1..31).map { it.toString() }
        val months = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
        val years = (Calendar.getInstance().get(Calendar.YEAR)..(Calendar.getInstance().get(Calendar.YEAR) + 5))
            .map { it.toString() }

        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        monthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        yearSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
    }

    private fun sendServiceRequest(serviceId: Int, userId: Int, selectedDate: String) {
        val jsonObject = JSONObject().apply {
            put("service_id", serviceId)
            put("user_id", userId)
            put("selected_date", selectedDate)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/request_service.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ServiceAvailActivity, "❌ Failed to request service", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Log.d("Service Request", "✅ Raw Response: $responseBody") // Add this to check API response

                if (responseBody.isNullOrEmpty()) {
                    Log.e("Service Request", "❌ API returned empty response")
                    runOnUiThread {
                        Toast.makeText(this@ServiceAvailActivity, "Error: No response from server", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ServiceAvailActivity, "✅ Service request sent", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this@ServiceAvailActivity, jsonResponse.optString("message", "Error"), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Service Request", "❌ JSON Parsing Error: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ServiceAvailActivity, "Invalid server response", Toast.LENGTH_LONG).show()
                    }
                }
            }

        })
    }


}
