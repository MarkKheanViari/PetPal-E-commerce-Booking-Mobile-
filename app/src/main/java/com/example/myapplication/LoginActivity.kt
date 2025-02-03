package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Clear any existing login data
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Check if user is already logged in
        if (sharedPreferences.contains("user_id")) {
            startMainActivity()
            finish()
            return
        }

        // Initialize views
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)

        // Set up click listeners
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin(username: String, password: String) {
        val jsonObject = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/mobile_login.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LoginActivity", "Login failed", e)
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData)

                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            // Save user data
                            sharedPreferences.edit().apply {
                                putInt("user_id", jsonResponse.getInt("user_id"))
                                putString("username", jsonResponse.getString("username"))
                                apply()
                            }

                            // Start MainActivity
                            startMainActivity()
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                jsonResponse.optString("message", "Login failed"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error processing response", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Error processing response: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}

