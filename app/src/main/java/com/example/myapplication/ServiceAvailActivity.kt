package com.example.myapplication

import android.content.Context
import android.content.Intent
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
    private lateinit var daySpinner: Spinner
    private lateinit var monthSpinner: Spinner
    private lateinit var yearSpinner: Spinner
    private lateinit var contactNumberInput: EditText
    private lateinit var submitButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_avail)

        // ✅ Fix: Get `service_id` as a String first for debugging
        val serviceId = intent.getIntExtra("SERVICE_ID", -1) // ✅ Must match key exactly
        Log.d("ServiceAvailActivity", "📥 Received Service ID: $serviceId")


        if (serviceId == -1) {
            Toast.makeText(this, "❌ Service not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Fix: Ensure `user_id` is correctly retrieved
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "❌ Please login to avail services", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Initialize UI components
        serviceNameText = findViewById(R.id.serviceNameText)
        daySpinner = findViewById(R.id.daySpinner)
        monthSpinner = findViewById(R.id.monthSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
        contactNumberInput = findViewById(R.id.contactNumberInput)
        submitButton = findViewById(R.id.submitButton)

        populateDateFields()

        // ✅ Fetch Service Details for Display
        fetchServiceDetails(serviceId)

        // ✅ Handle Submit Button Click
        submitButton.setOnClickListener {
            val selectedDate = "${yearSpinner.selectedItem}-${monthSpinner.selectedItem}-${daySpinner.selectedItem}"
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

    // ✅ NEW FUNCTION: Fetch Service Details
    private fun fetchServiceDetails(serviceId: Int) {
        val url = "http://192.168.1.13/backend/fetch_services.php"
        Log.d("Service Details", "🔄 Fetching service details for ID: $serviceId") // ✅ Debugging Log

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Service Details", "❌ Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Service Details", "✅ API Response: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    Log.e("Service Details", "❌ Empty response")
                    return
                }

                try {
                    val jsonArray = org.json.JSONArray(responseBody)
                    for (i in 0 until jsonArray.length()) {
                        val serviceJson = jsonArray.getJSONObject(i)
                        val fetchedId = serviceJson.getInt("id")
                        Log.d("Service Details", "🔎 Checking Service ID: $fetchedId")

                        if (fetchedId == serviceId) {
                            runOnUiThread {
                                serviceNameText.text = serviceJson.getString("service_name")
                            }
                            return
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(this@ServiceAvailActivity, "❌ Service not found!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: JSONException) {
                    Log.e("Service Details", "❌ JSON Parsing Error: ${e.message}")
                }
            }
        })
    }


    private fun sendServiceRequest(serviceId: Int, userId: Int, selectedDate: String) {
        val jsonObject = JSONObject().apply {
            put("mobile_user_id", userId)
            put("service_id", serviceId)
            put("selected_date", selectedDate)
            put("status", "pending")
        }

        Log.d("Service Request", "📨 Sending JSON: $jsonObject")

        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("http://192.168.1.13/backend/request_service.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Service Request", "❌ Failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ServiceAvailActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Service Request", "✅ Raw Response: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    Log.e("Service Request", "❌ API returned empty response")
                    runOnUiThread {
                        Toast.makeText(this@ServiceAvailActivity, "Error: No response from server", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    Log.d("Service Request", "✅ Parsed JSON: $jsonResponse")

                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ServiceAvailActivity, "✅ Service request sent!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this@ServiceAvailActivity, "❌ ${jsonResponse.optString("message", "Unknown error")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Service Request", "❌ JSON Parsing Error: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ServiceAvailActivity, "❌ Invalid server response", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
