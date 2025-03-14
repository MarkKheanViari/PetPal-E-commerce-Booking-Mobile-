package com.example.myapplication

import android.os.Bundle
import android.util.Log
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
    private lateinit var emailInput: EditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var resetButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        resetButton = findViewById(R.id.resetButton)

        // Back button: finish the activity to return to Login screen
        backBtn.setOnClickListener {
            finish()
        }

        // Reset button: validate input and perform password reset
        resetButton.setOnClickListener {
            // Clear previous error
            emailLayout.error = null
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
            } else {
                performPasswordReset(email)
            }
        }

        // Remove error as user types
        emailInput.addTextChangedListener {
            emailLayout.error = null
        }
    }

    private fun performPasswordReset(email: String) {
        // Prepare the JSON payload
        val jsonObject = JSONObject().apply {
            put("email", email)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        // Prepare the POST request
        val request = Request.Builder()
            .url("http://192.168.1.12/backend/forgot_password.php")
            .post(requestBody)
            .build()

        // Execute network call asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPasswordActivity", "Reset failed", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Reset failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseData = response.body?.string()
                    val jsonResponse = JSONObject(responseData ?: "")
                    runOnUiThread {
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password reset instructions sent to $email", Toast.LENGTH_LONG).show()
                            finish() // Return to Login screen
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Reset failed")
                            emailLayout.error = errorMessage
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ForgotPasswordActivity", "Error processing response", e)
                    runOnUiThread {
                        Toast.makeText(this@ForgotPasswordActivity, "Error processing response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
