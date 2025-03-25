package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapplication.Controller.WelcomeActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {


    private lateinit var backBtn: ImageView
    private lateinit var usernameLayout : TextInputLayout
    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var locationLayout: TextInputLayout
    private lateinit var locationInput: TextInputEditText
    private lateinit var contactLayout: TextInputLayout
    private lateinit var contactInput: TextInputEditText
    private lateinit var ageLayout: TextInputLayout
    private lateinit var ageInput: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var termsLink: TextView
    private lateinit var privacyLink: TextView
    private lateinit var registerButton: Button
    private lateinit var loginSugg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        usernameLayout = findViewById(R.id.usernameLayout)
        usernameInput = findViewById(R.id.usernameInput)
        emailLayout = findViewById(R.id.emailLayout)
        emailInput = findViewById(R.id.emailInput)
        locationLayout = findViewById(R.id.locationLayout)
        locationInput = findViewById(R.id.locationInput)
        contactLayout = findViewById(R.id.contactLayout)
        contactInput = findViewById(R.id.contactInput)
        ageLayout = findViewById(R.id.ageLayout)
        ageInput = findViewById(R.id.ageInput)
        passwordLayout = findViewById(R.id.passwordLayout)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        termsCheckbox = findViewById(R.id.termsCheckbox)
        termsLink = findViewById(R.id.termsLink)
        privacyLink = findViewById(R.id.privacyPolicyLink)
        registerButton = findViewById(R.id.registerButton)
        loginSugg = findViewById(R.id.loginSugg)

        backBtn.setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // Register button listener
        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val contact = contactInput.text.toString().trim()
            val age = ageInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true

            if (username.contains(" ")) {
                usernameLayout.error = "Username cannot contain spaces"
                isValid = false
            } else if (username.length < 8 || username.length > 16) {
                usernameLayout.error = "Username must be between 8 to 16 characters"
                isValid = false
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Invalid email address"
                isValid = false
            } else if (email.contains(" ")) {
                emailLayout.error = "Email cannot contain spaces"
                isValid = false
            }

            if (contact.length != 11) {
                contactLayout.error = "Invalid contact number"
                isValid = false
            }

            if (age.contains(" ")) {
                ageLayout.error = "Age cannot contain spaces"
                isValid = false
            } else {
                val ageInt = age.toIntOrNull()
                if (ageInt == null || ageInt < 9 || ageInt > 80) {
                    ageLayout.error = "Age must be between 9 and 80"
                    isValid = false
                }
            }

            if (password.length < 6 || password.length > 16) {
                passwordLayout.error = "Password must be between 6 to 16 characters"
                isValid = false
            } else if (password.contains(" ")) {
                passwordLayout.error = "Password cannot contain spaces"
                isValid = false
            } else if (password != confirmPassword) {
                passwordLayout.error = "Passwords do not match"
                isValid = false
            }

            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (isValid) {
                registerUser(username, email, location, contact, age, password)
            }
        }

        termsLink.paintFlags = termsLink.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        termsLink.setTextColor(getColor(R.color.smth_orange))

        privacyLink.paintFlags = privacyLink.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        privacyLink.setTextColor(getColor(R.color.smth_orange))

        loginSugg.paintFlags = loginSugg.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        loginSugg.setTextColor(getColor(R.color.smth_orange))

        // Focus listeners to validate fields
        usernameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val username = usernameInput.text.toString().trim()
                if (username.isBlank()) {
                    usernameLayout.error = "Username is Required"
                }
            }
        }

        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = emailInput.text.toString().trim()
                if (email.isBlank()) {
                    emailLayout.error = "Email is Required"
                }
            }
        }

        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val location = locationInput.text.toString().trim()
                if (location.isBlank()) {
                    locationLayout.error = "Location is Required"
                }
            }
        }

        ageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val age = ageInput.text.toString().trim()
                if (age.isBlank()) {
                    ageLayout.error = "Age is Required"
                }
            }
        }

        contactInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val contact = contactInput.text.toString().trim()
                if (contact.isBlank()) {
                    contactLayout.error = "Contact Number is Required"
                }
            }
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = passwordInput.text.toString().trim()
                if (password.isBlank()) {
                    passwordLayout.error = "Password is Required"
                }
            }
        }

        confirmPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val confirmPassword = confirmPasswordInput.text.toString().trim()
                if (confirmPassword.isBlank()) {
                    confirmPasswordLayout.error = "Confirm Password is Required"
                }
            }
        }

        // Text change listeners to clear errors
        usernameInput.addTextChangedListener {
            usernameLayout.error = null
        }

        emailInput.addTextChangedListener {
            emailLayout.error = null
        }

        locationInput.addTextChangedListener {
            locationLayout.error = null
        }

        contactInput.addTextChangedListener {
            contactLayout.error = null
        }

        ageInput.addTextChangedListener {
            ageLayout.error = null
        }

        passwordInput.addTextChangedListener {
            passwordLayout.error = null
        }

        confirmPasswordInput.addTextChangedListener {
            confirmPasswordLayout.error = null
        }

        // Click listeners for Terms, Privacy Policy, and Login suggestion
        termsLink.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }

        privacyLink.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        loginSugg.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser(username: String, email: String, location: String, contact: String, age: String, password: String) {
        val url = "http://192.168.1.15/backend/mobile_register.php"

        val jsonObject = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("password", password)
            put("location", location)
            put("age", age)
            put("contact_number", contact)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)
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