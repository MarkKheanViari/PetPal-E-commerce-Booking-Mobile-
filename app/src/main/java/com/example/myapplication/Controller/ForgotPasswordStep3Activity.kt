package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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

class ForgotPasswordStep3Activity : AppCompatActivity() {

    private var isPasswordVisible = false
    private val client = OkHttpClient()
    private lateinit var selectedMethod: String
    private lateinit var recoveryValue: String

    private lateinit var backBtn: ImageView
    private lateinit var newPasswordLayout : TextInputLayout
    private lateinit var confirmPasswordLayout : TextInputLayout
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var changePasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password_step3)

        backBtn = findViewById(R.id.backBtn)
        newPasswordLayout = findViewById(R.id.newPasswordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        changePasswordButton = findViewById(R.id.changePasswordButton)

        selectedMethod = intent.getStringExtra("RECOVERY_METHOD") ?: "Contact Number"
        recoveryValue = intent.getStringExtra("RECOVERY_VALUE") ?: ""

        backBtn.setOnClickListener {
            finish()
        }

        changePasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true
            if (newPassword.length < 6 || newPassword.length > 16) {
                newPasswordLayout.error = "Password must be 6-16 characters"
                isValid = false
            }
            if (newPassword != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                isValid = false
            }
            if (isValid) {
                resetPassword(recoveryValue, newPassword)
            }
        }

        newPasswordInput.addTextChangedListener {
            newPasswordLayout.error = null
        }
        confirmPasswordInput.addTextChangedListener {
            confirmPasswordLayout.error = null
        }
    }

    private fun resetPassword(recoveryValue: String, newPassword: String) {
        val jsonObject = JSONObject().apply {
            if (selectedMethod == "Email") put("email", recoveryValue)
            else put("contact_number", recoveryValue)
            put("new_password", newPassword)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.80.63/backend/mobile_reset_password.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPassword", "Failed to reset password: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordStep3Activity, "Failed to reset password: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "Reset Password Response: $responseData")
                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseData ?: "{}")
                        if (jsonResponse.optBoolean("success", false)) {
                            val message = if (jsonResponse.optBoolean("new_user", false)) {
                                "Password reset successful. New account created."
                            } else {
                                "Password reset successful!"
                            }
                            Toast.makeText(this@ForgotPasswordStep3Activity, message, Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@ForgotPasswordStep3Activity, LoginActivity::class.java))
                            finish()
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Failed to reset password")
                            Log.e("ForgotPassword", "Server error: $errorMessage")
                            Toast.makeText(this@ForgotPasswordStep3Activity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        Log.e("ForgotPassword", "Failed to parse response as JSON: $responseData", e)
                        Toast.makeText(this@ForgotPasswordStep3Activity, "Unexpected server response: $responseData", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
    /*
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
                if (isPasswordVisible) android.text.method.HideReturnsTransformationMethod.getInstance()
                else android.text.method.PasswordTransformationMethod.getInstance()
            newPasswordInput.setSelection(newPasswordInput.text?.length ?: 0)
        }

        confirmPasswordInput.setCompoundDrawableClickListener {
            isPasswordVisible = !isPasswordVisible
            confirmPasswordInput.transformationMethod =
                if (isPasswordVisible) android.text.method.HideReturnsTransformationMethod.getInstance()
                else android.text.method.PasswordTransformationMethod.getInstance()
            confirmPasswordInput.setSelection(confirmPasswordInput.text?.length ?: 0)
        }
    } */
}