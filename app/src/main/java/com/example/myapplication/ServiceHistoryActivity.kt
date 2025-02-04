package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ServiceHistoryActivity : AppCompatActivity() {
    private lateinit var serviceListView: ListView
    private lateinit var client: OkHttpClient
    private lateinit var historyAdapter: ArrayAdapter<String>
    private val serviceHistory = mutableListOf<HashMap<String, String>>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_history)

        serviceListView = findViewById(R.id.serviceListView)
        client = OkHttpClient()

        fetchServiceHistory()
    }

    private fun fetchServiceHistory() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please log in to view service history", Toast.LENGTH_LONG).show()
            return
        }

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/fetch_service_history.php?user_id=$userId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ServiceHistoryActivity, "Error fetching service history", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val historyArray = jsonResponse.getJSONArray("history")

                    serviceHistory.clear()

                    for (i in 0 until historyArray.length()) {
                        val service = historyArray.getJSONObject(i)
                        val item = hashMapOf(
                            "id" to service.getInt("id").toString(),
                            "service_name" to service.getString("service_name"),
                            "selected_date" to service.getString("selected_date"),
                            "status" to service.getString("status")
                        )
                        serviceHistory.add(item)
                    }

                    runOnUiThread {
                        val adapter = ServiceHistoryAdapter(this@ServiceHistoryActivity, serviceHistory)
                        serviceListView.adapter = adapter
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "Error processing data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    fun cancelServiceRequest(requestId: Int) {
        val url = "http://192.168.1.65/backend/cancel_service_request.php" // ✅ Ensure this backend exists

        val json = JSONObject()
        json.put("id", requestId)

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ServiceHistoryActivity, "❌ Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@ServiceHistoryActivity, "✅ Request cancelled", Toast.LENGTH_SHORT).show()
                    fetchServiceHistory() // ✅ Refresh the list after cancelling
                }
            }
        })
    }


}
