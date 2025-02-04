package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ServiceHistoryActivity : AppCompatActivity() {
    private lateinit var serviceListView: ListView
    private lateinit var client: OkHttpClient
    private lateinit var historyAdapter: ArrayAdapter<String>
    private val serviceHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_history)

        serviceListView = findViewById(R.id.serviceListView)
        client = OkHttpClient()

        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, serviceHistory)
        serviceListView.adapter = historyAdapter

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
                        val serviceName = service.getString("service_name")
                        val date = service.getString("selected_date")
                        val status = service.getString("status")

                        serviceHistory.add("$serviceName - $date - Status: $status")
                    }

                    runOnUiThread {
                        historyAdapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ServiceHistoryActivity, "Error processing data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
