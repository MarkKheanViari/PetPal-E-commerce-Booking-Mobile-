package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapplication.Controller.WelcomeActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private lateinit var backBtn : ImageView
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotpass : TextView
    private lateinit var registerLink: TextView
    private lateinit var rememberMeCheckBox: CheckBox
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // If the user did not choose "Remember Me" previously, clear stored login data.
        if (!sharedPreferences.getBoolean("remember_me", false)) {
            sharedPreferences.edit().clear().apply()
        }

        // Check if user is already logged in (i.e. if user_id exists in SharedPreferences)
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
        forgotpass = findViewById(R.id.forgotPass)
        registerLink = findViewById(R.id.registerLink)
        rememberMeCheckBox = findViewById(R.id.rememberMe)

        forgotpass.paintFlags = forgotpass.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        forgotpass.setTextColor(getColor(R.color.smth_orange))

        registerLink.paintFlags = registerLink.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        registerLink.setTextColor(getColor(R.color.smth_orange))

        backBtn.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // Set up click listeners
        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            var isValid = true

            if (username.isEmpty()) {
                usernameInput.error = "Username is Required"
                usernameInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is Required"
                passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }

            if (isValid) {
                performLogin(username, password)
            }
        }

        usernameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // When user leaves username field
                val username = usernameInput.text.toString().trim()
                if (username.isBlank()) {
                    usernameInput.error = "Username is Required"
                    usernameInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    usernameInput.setBackgroundResource(R.drawable.login_design) // Reset background
                }
            }
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // When user leaves password field
                val password = passwordInput.text.toString().trim()
                if (password.isBlank()) {
                    passwordInput.error = "Password is Required"
                    passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    passwordInput.setBackgroundResource(R.drawable.login_design) // Reset background
                }
            }
        }

        usernameInput.addTextChangedListener {
            usernameInput.error = null
            usernameInput.setBackgroundResource(R.drawable.login_design)
        }

        passwordInput.addTextChangedListener {
            passwordInput.error = null
            passwordInput.setBackgroundResource(R.drawable.login_design)
        }




        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    // Function to reset errors and background

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
                    val jsonResponse = JSONObject(responseData)

                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            val userId = jsonResponse.optInt("user_id", -1)
                            Log.d("LoginActivity", "Received user_id: $userId")

                            if (userId != -1) {
                                // Save user data including email using optString with a default value.
                                // Also store the "remember_me" flag based on the checkbox.
                                sharedPreferences.edit().apply {
                                    putInt("user_id", userId)
                                    putString("username", jsonResponse.getString("username"))
                                    putString("user_email", jsonResponse.optString("email", "user@example.com"))
                                    putString("location", jsonResponse.optString("location", ""))
                                    putString("contact_number", jsonResponse.optString("contact_number", ""))
                                    putBoolean("remember_me", rememberMeCheckBox.isChecked)
                                    putBoolean("isLoggedIn", true)
                                    putBoolean("hasSeenIntro", true)
                                    apply()
                                }

                                Log.d("LoginActivity", "Stored user_id: ${sharedPreferences.getInt("user_id", -1)}")
                                startMainActivity()
                                finish()
                            } else {
                                Toast.makeText(this@LoginActivity, "Error: User ID is missing!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            // If login fails, check if the error message indicates a wrong password.
                            val errorMessage = jsonResponse.optString("message", "Login failed")
                            if (errorMessage.contains("wrong password", ignoreCase = true)) {
                                // Set an error on the password field so the hint displays "Wrong password"
                                passwordInput.error = "Wrong password"
                                passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
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