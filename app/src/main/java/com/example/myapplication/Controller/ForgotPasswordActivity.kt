package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
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

    private var isPasswordVisible = false // Track visibility state

    private lateinit var backBtn: ImageView
    private lateinit var phoneNumberInput : EditText
    private lateinit var otpInput : EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
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

        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        otpInput = findViewById(R.id.otpInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)

        // Back button returns to the previous screen
        backBtn.setOnClickListener { finish() }

        // Remove error messages as user types
        newPasswordInput.addTextChangedListener {
            newPasswordInput.error = null
            newPasswordInput.setBackgroundResource(R.drawable.login_design)
        }
        confirmPasswordInput.addTextChangedListener {
            confirmPasswordInput.error = null
            confirmPasswordInput.setBackgroundResource(R.drawable.login_design)
        }

        // Change Password button click listener
        changePasswordButton.setOnClickListener {
            // Clear any previous error messages
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true

            // Validate inputs
            if (newPassword.isEmpty()) {
                newPasswordInput.error = "New password is required"
                newPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            } else if (newPassword < 6.toString() && newPassword > 16.toString()) {
                newPasswordInput.error = "Password must be between 6 or 16 characters"
                newPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            } else if (newPassword.contains(" ")) {
                newPasswordInput.error = "Password cannot contain spaces"
                newPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }
            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.error = "Please confirm your new password"
                confirmPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }
            if (newPassword != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }

            // Retrieve the user id from SharedPreferences
            val userId = sharedPreferences.getInt("user_id", -1)
            if (userId == -1) {
                Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_LONG).show()
                isValid = false
            }


            // Call the backend API to update the password in the database
            if (isValid) {
                performPasswordChange(userId, newPassword)
            }
        }

        setupPasswordToggle()
    }

    private fun EditText.setCompoundDrawableClickListener(onDrawableEndClick: () -> Unit) {
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= (right - paddingEnd - drawableEnd.bounds.width())) {
                    onDrawableEndClick()
                    performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupPasswordToggle() {
        newPasswordInput.setCompoundDrawableClickListener {
            isPasswordVisible = !isPasswordVisible
            newPasswordInput.transformationMethod =
                if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            newPasswordInput.setSelection(newPasswordInput.text?.length ?: 0)
        }

        confirmPasswordInput.setCompoundDrawableClickListener {
            isPasswordVisible = !isPasswordVisible
            confirmPasswordInput.transformationMethod =
                if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            confirmPasswordInput.setSelection(confirmPasswordInput.text?.length ?: 0)
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
