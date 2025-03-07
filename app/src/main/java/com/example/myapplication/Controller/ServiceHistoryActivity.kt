package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ServiceHistoryActivity : AppCompatActivity() {

    private lateinit var serviceListView: ListView
    private lateinit var emptyStateText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var clearHistoryButton: Button
    private lateinit var historyAdapter: ServiceHistoryAdapter
    private val client = OkHttpClient()
    private var userId: Int = -1

    private var servicesList: MutableList<HashMap<String, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_history)

        serviceListView = findViewById(R.id.serviceListView)
        emptyStateText = findViewById(R.id.emptyStateText)
        progressBar = findViewById(R.id.progressBar)
        clearHistoryButton = findViewById(R.id.clearHistoryButton) // ✅ Initialize button


        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "❌ Please login to view history", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        clearHistoryButton.setOnClickListener {
            clearServiceHistory()
        }

        fetchServiceHistory()
    }

    private fun fetchServiceHistory() {
        val url = "http://192.168.43.215/backend/fetch_service_history.php?user_id=$userId"
        val request = Request.Builder().url(url).get().build()

        runOnUiThread { progressBar.visibility = View.VISIBLE }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ServiceHistoryActivity, "❌ Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Service History", "✅ Raw Response: $responseBody")

                runOnUiThread { progressBar.visibility = View.GONE }

                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        emptyStateText.visibility = View.VISIBLE
                        clearHistoryButton.visibility = View.GONE
                        Toast.makeText(this@ServiceHistoryActivity, "❌ No service history found", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonArray = JSONArray(responseBody)
                    servicesList.clear()

                    for (i in 0 until jsonArray.length()) {
                        val service = jsonArray.getJSONObject(i)
                        val historyItem = hashMapOf(
                            "id" to service.getString("id"),
                            "service_name" to service.getString("service_name"),
                            "selected_date" to service.getString("selected_date"),
                            "status" to service.getString("status")
                        )
                        servicesList.add(historyItem)
                    }

                    runOnUiThread {
                        if (servicesList.isEmpty()) {
                            emptyStateText.visibility = View.VISIBLE
                            clearHistoryButton.visibility = View.GONE
                        } else {
                            emptyStateText.visibility = View.GONE
                            clearHistoryButton.visibility = View.VISIBLE
                            historyAdapter = ServiceHistoryAdapter(this@ServiceHistoryActivity, servicesList) { serviceId ->
                                cancelServiceRequest(serviceId)
                            }
                            serviceListView.adapter = historyAdapter
                        }
                    }

                } catch (e: JSONException) {
                    Log.e("Service History", "❌ JSON Parsing Error: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "❌ Error Processing Data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun clearServiceHistory() {
        val url = "http://192.168.43.215/backend/clear_service_history.php"
        val jsonBody = """
    {
        "mobile_user_id": $userId
    }
    """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody) // ✅ Change to POST to match working Postman request
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Clear History", "❌ Network Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ServiceHistoryActivity, "❌ Network Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Clear History", "✅ Full Server Response: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "❌ No response from server", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)

                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@ServiceHistoryActivity, "✅ Service history cleared!", Toast.LENGTH_LONG).show()
                            servicesList.clear()
                            historyAdapter.notifyDataSetChanged()
                            clearHistoryButton.visibility = View.GONE
                            emptyStateText.visibility = View.VISIBLE
                        } else {
                            val errorMessage = jsonResponse.optString("error", "Unknown error")
                            Toast.makeText(this@ServiceHistoryActivity, "❌ Failed: $errorMessage", Toast.LENGTH_LONG).show()
                            Log.e("Clear History", "❌ Server Error Message: $errorMessage")
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Clear History", "❌ JSON Parsing Error: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "❌ Error processing request", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }



    fun cancelServiceRequest(serviceId: Int) {
        val url = "http://192.168.43.215/backend/cancel_service_request.php"
        val jsonBody = """
    {
        "service_id": $serviceId
    }
    """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Cancel Request", "❌ Network Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ServiceHistoryActivity, "❌ Network Error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Cancel Request", "✅ Response: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "❌ No response from server", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)

                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@ServiceHistoryActivity, "✅ Service request cancelled!", Toast.LENGTH_LONG).show()
                            updateCancelledStatus(serviceId) // ✅ Update UI
                        } else {
                            Toast.makeText(this@ServiceHistoryActivity, "❌ Failed to cancel request", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("Cancel Request", "❌ JSON Parsing Error: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "❌ Error processing request", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun updateCancelledStatus(serviceId: Int) {
        for (service in servicesList) {
            if (service["id"]?.toInt() == serviceId) {
                service["status"] = "cancelled"
                break
            }
        }
        runOnUiThread {
            historyAdapter.notifyDataSetChanged() // ✅ Refresh UI immediately
        }
    }


}
