package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
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

class ForgotPasswordStep1Activity : AppCompatActivity() {

    private var selectedMethod: String = "Contact Number" // Default selection
    private val client = OkHttpClient()
    private var generatedOtp: String? = null

    private lateinit var backBtn: ImageView
    private lateinit var recoveryMethodSpinner: Spinner
    private lateinit var recoveryLayout: TextInputLayout
    private lateinit var recoveryInput: TextInputEditText
    private lateinit var getOtpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password_step1)

        // Initialize all views including recoveryLayout first
        backBtn = findViewById(R.id.backBtn)
        recoveryMethodSpinner = findViewById(R.id.recoveryMethodSpinner)
        recoveryLayout = findViewById(R.id.recoveryLayout) // Now initialized before use
        recoveryInput = findViewById(R.id.recoveryInput)
        getOtpButton = findViewById(R.id.getOtpButton)

        // Setup Spinner *after* initializing views
        val methods = arrayOf("Contact Number", "Email")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recoveryMethodSpinner.adapter = adapter
        recoveryMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedMethod = methods[position]
                recoveryInput.hint = if (selectedMethod == "Email") "Enter your email" else "Enter your phone number"
                recoveryLayout.startIconDrawable = if (selectedMethod == "Email") getDrawable(R.drawable.email) else getDrawable(R.drawable.contact)
                recoveryInput.inputType = if (selectedMethod == "Email") InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS else InputType.TYPE_CLASS_PHONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Rest of your code remains the same...
        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        getOtpButton.setOnClickListener {
            val recoveryValue = recoveryInput.text.toString().trim()
            when (selectedMethod) {
                "Contact Number" -> {
                    if (recoveryValue.length != 11) {
                        recoveryLayout.error = "Enter a valid 11-digit phone number"
                    } else {
                        requestOtpViaTwilio(recoveryValue) { success ->
                            if (success) {
                                val intent = Intent(this, ForgotPasswordStep2Activity::class.java)
                                intent.putExtra("RECOVERY_METHOD", selectedMethod)
                                intent.putExtra("RECOVERY_VALUE", recoveryValue)
                                intent.putExtra("GENERATED_OTP", generatedOtp)
                                startActivity(intent)
                            }
                        }
                    }
                }
                "Email" -> {
                    if (!Patterns.EMAIL_ADDRESS.matcher(recoveryValue).matches()) {
                        recoveryLayout.error = "Enter a valid email address"
                    } else {
                        requestOtpViaEmail(recoveryValue) { success ->
                            if (success) {
                                val intent = Intent(this, ForgotPasswordStep2Activity::class.java)
                                intent.putExtra("RECOVERY_METHOD", selectedMethod)
                                intent.putExtra("RECOVERY_VALUE", recoveryValue)
                                intent.putExtra("GENERATED_OTP", generatedOtp)
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
        }

        recoveryInput.addTextChangedListener {
            recoveryLayout.error = null
        }
    }

    private fun requestOtpViaTwilio(phoneNumber: String, callback: (Boolean) -> Unit) {
        generatedOtp = (100000..999999).random().toString()
        val accountSid = "AC3c220916700e2b965cc6370150ebff1e"
        val authToken = "7bc53fe182735c1f8bb5c34b246946b8"
        val twilioPhoneNumber = "+18108183776"
        val message = "Your OTP for password reset is: $generatedOtp"

        val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
        val formBody = FormBody.Builder()
            .add("To", "+63$phoneNumber")
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
                Log.e("ForgotPassword", "Failed to send OTP via Twilio", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordStep1Activity, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "Twilio Response: $responseData")
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordStep1Activity, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
                        callback(true)
                    } else {
                        Toast.makeText(this@ForgotPasswordStep1Activity, "Failed to send OTP", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            }
        })
    }

    private fun requestOtpViaEmail(email: String, callback: (Boolean) -> Unit) {
        generatedOtp = (100000..999999).random().toString()
        val jsonObject = JSONObject().apply {
            put("email", email)
            put("otp", generatedOtp)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/send_otp_email.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ForgotPassword", "Failed to send OTP via Email", e)
                runOnUiThread {
                    Toast.makeText(this@ForgotPasswordStep1Activity, "Failed to send OTP: ${e.message}", Toast.LENGTH_LONG).show()
                    callback(false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                Log.d("ForgotPassword", "Email OTP Response: $responseData")
                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(responseData ?: "{}")
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ForgotPasswordStep1Activity, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                            callback(true)
                        } else {
                            Toast.makeText(this@ForgotPasswordStep1Activity, jsonResponse.optString("message", "Failed to send OTP"), Toast.LENGTH_LONG).show()
                            callback(false)
                        }
                    } catch (e: JSONException) {
                        Log.e("ForgotPassword", "Failed to parse response as JSON", e)
                        Toast.makeText(this@ForgotPasswordStep1Activity, "Unexpected server response: $responseData", Toast.LENGTH_LONG).show()
                        callback(false)
                    }
                }
            }
        })
    }
}