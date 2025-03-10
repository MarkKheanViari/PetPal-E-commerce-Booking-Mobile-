package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var backBtn: ImageView
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Clear stored login data if "Remember Me" was not checked
        if (!sharedPreferences.getBoolean("remember_me", false)) {
            sharedPreferences.edit().clear().apply()
        }

        // Auto-login if user is already logged in
        if (sharedPreferences.contains("user_id")) {
            startMainActivity()
            finish()
            return
        }

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        rememberMeCheckBox = findViewById(R.id.rememberMe)

        // Set up click listeners
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

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
            .url("http://192.168.1.12/backend/mobile_login.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("LoginActivity", "Login failed", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData ?: "")

                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            val userId = jsonResponse.optInt("user_id", -1)
                            Log.d("LoginActivity", "Received user_id: $userId")

                            if (userId != -1) {
                                // Retrieve username and email from the response
                                val returnedUsername = jsonResponse.getString("username")
                                val returnedEmail = jsonResponse.optString("email", "user@example.com")

                                // Store user data including email in SharedPreferences
                                sharedPreferences.edit().apply {
                                    putInt("user_id", userId)
                                    putString("username", returnedUsername)
                                    putString("user_email", returnedEmail)
                                    // You can also store additional fields here.
                                    putBoolean("remember_me", rememberMeCheckBox.isChecked)
                                    putBoolean("isLoggedIn", true)
                                    apply()
                                }

                                // Optional: Show a welcome message
                                Toast.makeText(this@LoginActivity, "Welcome, $returnedUsername!", Toast.LENGTH_LONG).show()

                                // Navigate to the main screen
                                startMainActivity()
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "Error: User ID is missing!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Login failed")
                            if (errorMessage.contains("wrong password", ignoreCase = true)) {
                                passwordInput.error = "Wrong password"
                            } else {
                                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error processing response", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}
