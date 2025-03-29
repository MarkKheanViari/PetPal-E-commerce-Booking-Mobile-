package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordStep2Activity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var selectedMethod: String
    private lateinit var recoveryValue: String
    private var generatedOtp: String? = null

    private lateinit var backBtn: ImageView
    private lateinit var otpLayout : TextInputLayout
    private lateinit var otpInput: TextInputEditText
    private lateinit var confirmOtpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password_step2)

        backBtn = findViewById(R.id.backBtn)
        otpLayout = findViewById(R.id.otpLayout)
        otpInput = findViewById(R.id.otpInput)
        confirmOtpButton = findViewById(R.id.confirmOtpButton)

        selectedMethod = intent.getStringExtra("RECOVERY_METHOD") ?: "Contact Number"
        recoveryValue = intent.getStringExtra("RECOVERY_VALUE") ?: ""
        generatedOtp = intent.getStringExtra("GENERATED_OTP")

        backBtn.setOnClickListener {
            finish()
        }

        confirmOtpButton.setOnClickListener {
            val enteredOtp = otpInput.text.toString().trim()
            if (enteredOtp.isEmpty()) {
                otpLayout.error = "Enter the OTP"
            } else {
                if (selectedMethod == "Email") {
                    verifyEmailOtp(recoveryValue, enteredOtp) { success ->
                        if (success) {
                            val intent = Intent(this, ForgotPasswordStep3Activity::class.java)
                            intent.putExtra("RECOVERY_METHOD", selectedMethod)
                            intent.putExtra("RECOVERY_VALUE", recoveryValue)
                            startActivity(intent)
                        }
                    }
                } else {
                    if (enteredOtp == generatedOtp) {
                        Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ForgotPasswordStep3Activity::class.java)
                        intent.putExtra("RECOVERY_METHOD", selectedMethod)
                        intent.putExtra("RECOVERY_VALUE", recoveryValue)
                        startActivity(intent)
                    } else {
                        otpLayout.error = "Invalid OTP"
                    }
                }
            }
        }

        // Fix: Use the correct addTextChangedListener with a lambda
        otpInput.addTextChangedListener {
            otpLayout.error = null
        }
    }

    private fun verifyEmailOtp(email: String, enteredOtp: String, callback: (Boolean) -> Unit) {
        val jsonObject = JSONObject().apply {
            put("email", email)
            put("otp", enteredOtp)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.12/backend/verify_otp.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPassword", "Failed to verify OTP: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordStep2Activity, "Failed to verify OTP: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "Verify OTP Response: $responseData")
                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseData ?: "{}")
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordStep2Activity, "OTP Verified!", Toast.LENGTH_SHORT).show()
                            callback(true)
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Failed to verify OTP")
                            otpInput.error = errorMessage
                            otpInput.setBackgroundResource(R.drawable.edittext_error_background)
                            callback(false)
                        }
                    } catch (e: JSONException) {
                        Log.e("ForgotPassword", "Failed to parse response as JSON: $responseData", e)
                        Toast.makeText(this@ForgotPasswordStep2Activity, "Unexpected server response: $responseData", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            }
        })
    }
}