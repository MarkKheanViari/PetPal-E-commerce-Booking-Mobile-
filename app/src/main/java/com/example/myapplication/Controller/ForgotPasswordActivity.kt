package com.example.myapplication

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var changePasswordButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize SharedPreferences (assuming the same file used in Login/Registration)
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)

        // Back button returns to the previous screen
        backBtn.setOnClickListener { finish() }

        // Remove error messages as user types
        newPasswordInput.addTextChangedListener { newPasswordLayout.error = null }
        confirmPasswordInput.addTextChangedListener { confirmPasswordLayout.error = null }

        // Change Password button click listener
        changePasswordButton.setOnClickListener {
            // Clear any previous error messages
            newPasswordLayout.error = null
            confirmPasswordLayout.error = null

            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Validate inputs
            if (newPassword.isEmpty()) {
                newPasswordLayout.error = "New password is required"
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = "Please confirm your new password"
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Retrieve the user id from SharedPreferences
            val userId = sharedPreferences.getInt("user_id", -1)
            if (userId == -1) {
                Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Call the backend API to update the password in the database
            performPasswordChange(userId, newPassword)
        }
    }

    private fun performPasswordChange(userId: Int, newPassword: String) {
        // Prepare JSON payload with user id and new password
        val jsonObject = JSONObject().apply {
            put("user_id", userId)
            put("new_password", newPassword)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        // Replace the URL with your actual backend endpoint URL
        val request = Request.Builder()
            .url("http://192.168.1.65/backend/mobile_change_password.php")
            .post(requestBody)
            .build()

        // Execute network call asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Password change failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData ?: "")
                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password changed successfully", Toast.LENGTH_LONG).show()
                            finish() // Return to previous screen (e.g. Login)
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Password change failed")
                            Toast.makeText(this@ForgotPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ForgotPasswordActivity, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
