package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var contactInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize fields
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        locationInput = findViewById(R.id.locationInput)
        ageInput = findViewById(R.id.ageInput)
        contactInput = findViewById(R.id.contactInput)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmPasswordInput.text.toString()
        val location = locationInput.text.toString().trim()
        val age = ageInput.text.toString().trim()
        val contact = contactInput.text.toString().trim()

        // Validate fields
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || location.isEmpty() || age.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "❌ Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "❌ Please enter a valid email.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "❌ Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!termsCheckbox.isChecked) {
            Toast.makeText(this, "❌ You must accept the Terms and Conditions.", Toast.LENGTH_SHORT).show()
            return
        }

        // Proceed with registration
        registerUser(username, email, password, location, age, contact)
    }

    private fun registerUser(username: String, email: String, password: String, location: String, age: String, contact: String) {
        val url = "http://192.168.1.13/backend/mobile_register.php"

        val jsonObject = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("location", location)
            put("age", age)
            put("contact_number", contact)  // Change this to "contact_number"
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    Log.d("RegisterActivity", "📦 Response: $responseBody")
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@RegisterActivity, "✅ Registration successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "❌ ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

}
