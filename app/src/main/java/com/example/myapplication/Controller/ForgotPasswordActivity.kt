package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private val client = OkHttpClient()
    private var generatedOtp: String? = null

    private lateinit var backBtn: ImageView
    private lateinit var phoneNumberInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var getOtpButton: Button
    private lateinit var confirmOtpButton: Button
    private lateinit var changePasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        otpInput = findViewById(R.id.otpInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        getOtpButton = findViewById(R.id.getOtpButton)
        confirmOtpButton = findViewById(R.id.confirmOtpButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)

        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        getOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberInput.text.toString().trim()
            if (phoneNumber.length != 11) {
                phoneNumberInput.error = "Enter a valid 11-digit phone number"
                phoneNumberInput.setBackgroundResource(R.drawable.edittext_error_background)
            } else {
                requestOtp(phoneNumber)
            }
        }

        confirmOtpButton.setOnClickListener {
            val enteredOtp = otpInput.text.toString().trim()
            if (enteredOtp.isEmpty()) {
                otpInput.error = "Enter the OTP"
                otpInput.setBackgroundResource(R.drawable.edittext_error_background)
            } else if (enteredOtp == generatedOtp) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                enablePasswordFields(true)
            } else {
                otpInput.error = "Invalid OTP"
                otpInput.setBackgroundResource(R.drawable.edittext_error_background)
            }
        }

        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true
            if (newPassword.length < 6 || newPassword.length > 16) {
                newPasswordInput.error = "Password must be 6-16 characters"
                newPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }
            if (newPassword != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }
            if (isValid) {
                resetPassword(phoneNumberInput.text.toString().trim(), newPassword)
            }
        }

        // Clear errors on text change
        phoneNumberInput.addTextChangedListener {
            phoneNumberInput.error = null
            phoneNumberInput.setBackgroundResource(R.drawable.login_design)
        }
        otpInput.addTextChangedListener {
            otpInput.error = null
            otpInput.setBackgroundResource(R.drawable.login_design)
        }
        newPasswordInput.addTextChangedListener {
            newPasswordInput.error = null
            newPasswordInput.setBackgroundResource(R.drawable.login_design)
        }
        confirmPasswordInput.addTextChangedListener {
            confirmPasswordInput.error = null
            confirmPasswordInput.setBackgroundResource(R.drawable.login_design)
        }

        setupPasswordToggle()
        enablePasswordFields(false) // Initially disable password fields
    }

    private fun enablePasswordFields(enable: Boolean) {
        newPasswordInput.isEnabled = enable
        confirmPasswordInput.isEnabled = enable
        changePasswordButton.isEnabled = enable
    }

    private fun requestOtp(phoneNumber: String) {
        generatedOtp = (100000..999999).random().toString()
        val accountSid = "AC3c220916700e2b965cc6370150ebff1e"
        val authToken = "7bc53fe182735c1f8bb5c34b246946b8"
        val twilioPhoneNumber = "+18108183776"
        val message = "Your OTP for password reset is: $generatedOtp"

        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
        val formBody = FormBody.Builder()
            .add("To", "+63$phoneNumber") // Assuming PH number, adjust country code as needed
            .add("From", twilioPhoneNumber)
            .add("Body", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .header("Authorization", Credentials.basic(accountSid, authToken))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPassword", "Failed to send OTP", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "OTP Response: $responseData")
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Failed to send OTP", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun resetPassword(phoneNumber: String, newPassword: String) {
        val jsonObject = JSONObject().apply {
            put("contact_number", phoneNumber)
            put("new_password", newPassword)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/mobile_reset_password.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPassword", "Reset password failed", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordActivity, "Reset failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "Reset Response: $responseData")
                val jsonResponse = JSONObject(responseData ?: "")
                runOnUiThread {
                    if (jsonResponse.optBoolean("success", false)) {
                        Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, jsonResponse.optString("message", "Reset failed"), Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
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
}