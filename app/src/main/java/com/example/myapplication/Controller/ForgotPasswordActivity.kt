package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var contactInput: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var otpInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var resetPasswordButton: Button
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        contactInput = findViewById(R.id.contactInput)
        sendOtpButton = findViewById(R.id.sendOtpButton)
        otpInput = findViewById(R.id.otpInput)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)

        // Back button listener
        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Send OTP button listener
        sendOtpButton.setOnClickListener {
            val contact = contactInput.text.toString().trim()
            if (contact.length != 11) {
                contactInput.error = "Invalid contact number (11 digits required)"
                return@setOnClickListener
            }
            showLoadingDialog()
            sendOtp(contact)
        }

        // Reset Password button listener
        resetPasswordButton.setOnClickListener {
            val contact = contactInput.text.toString().trim()
            val otp = otpInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()

            if (otp.isEmpty()) {
                otpInput.error = "OTP is required"
                return@setOnClickListener
            }
            if (newPassword.isEmpty() || newPassword.length < 6 || newPassword.length > 16) {
                newPasswordInput.error = "Password must be between 6 and 16 characters"
                return@setOnClickListener
            }
            showLoadingDialog()
            verifyOtpAndResetPassword(contact, otp, newPassword)
        }
    }

    private fun sendOtp(contact: String) {
        val jsonObject = JSONObject().apply {
            put("contact_number", contact)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/send_otp.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    dismissLoadingDialog()
                    Toast.makeText(this@ForgotPasswordActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    dismissLoadingDialog()
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordActivity, "OTP sent successfully!", Toast.LENGTH_SHORT).show()
                            otpInput.visibility = View.VISIBLE
                            newPasswordInput.visibility = View.VISIBLE
                            resetPasswordButton.visibility = View.VISIBLE
                            sendOtpButton.visibility = View.GONE
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, jsonResponse.optString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun verifyOtpAndResetPassword(contact: String, otp: String, newPassword: String) {
        val jsonObject = JSONObject().apply {
            put("contact_number", contact)
            put("otp", otp)
            put("new_password", newPassword)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/verify_otp.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    dismissLoadingDialog()
                    Toast.makeText(this@ForgotPasswordActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    dismissLoadingDialog()
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, jsonResponse.optString("message"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private var loadingDialog: AlertDialog? = null

    private fun showLoadingDialog() {
        loadingDialog?.dismiss()
        val builder = AlertDialog.Builder(this)
        builder.setView(R.layout.dialog_loading)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }
}