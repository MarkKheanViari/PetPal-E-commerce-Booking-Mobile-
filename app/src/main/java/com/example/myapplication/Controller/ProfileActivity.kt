package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var usernameValue: TextView
    private lateinit var emailValue: TextView
    private lateinit var createdAtValue: TextView
    private lateinit var locationValue: TextView
    private lateinit var ageValue: TextView
    private lateinit var contactNumberValue: TextView
    private lateinit var sharedPreferences: SharedPreferences // Make sharedPreferences a class property
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        usernameValue = findViewById(R.id.usernameValue)
        emailValue = findViewById(R.id.emailValue)
        createdAtValue = findViewById(R.id.createdAtValue)
        locationValue = findViewById(R.id.locationValue)
        ageValue = findViewById(R.id.ageValue)
        contactNumberValue = findViewById(R.id.contactNumberValue)

        // Set up back button
        backBtn.setOnClickListener {
            finish()
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        // First, load from SharedPreferences as a fallback
        val userId = sharedPreferences.getInt("user_id", -1)
        val username = sharedPreferences.getString("username", "Unknown") ?: "Unknown"
        val email = sharedPreferences.getString("user_email", "Unknown") ?: "Unknown"

        // Set the SharedPreferences data as a fallback
        usernameValue.text = username
        emailValue.text = email
        createdAtValue.text = "Not available"
        locationValue.text = "Not available"
        ageValue.text = "Not available"
        contactNumberValue.text = "Not available"

        // Fetch the latest data from the server
        if (userId != -1) {
            fetchUserDataFromServer(userId)
        } else {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchUserDataFromServer(userId: Int) {
        val jsonObject = JSONObject().apply {
            put("user_id", userId)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/mobile_get_user.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfileActivity", "Failed to fetch user data", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to fetch user data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string()
                    Log.d("ProfileActivity", "Response: $responseData")
                    val jsonResponse = JSONObject(responseData ?: "")
                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            val user = jsonResponse.getJSONObject("user")
                            usernameValue.text = user.optString("username", "Unknown")
                            emailValue.text = user.optString("email", "Unknown")
                            createdAtValue.text = user.optString("created_at", "Not available")
                            locationValue.text = user.optString("location", "Not available")
                            ageValue.text = user.optInt("age", 0).toString()
                            contactNumberValue.text = user.optString("contact_number", "Not available")

                            // Update SharedPreferences with the latest data
                            with(sharedPreferences.edit()) {
                                putString("username", user.optString("username", "Unknown"))
                                putString("user_email", user.optString("email", "Unknown"))
                                apply()
                            }
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Failed to fetch user data")
                            Toast.makeText(this@ProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Error processing response", e)
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}