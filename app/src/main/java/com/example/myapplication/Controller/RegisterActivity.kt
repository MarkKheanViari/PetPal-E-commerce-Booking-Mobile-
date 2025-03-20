package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapplication.Controller.WelcomeActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private var isPasswordVisible = false // Track visibility state

    private lateinit var backBtn: ImageView
    private lateinit var usernameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var contactInput: EditText
    private lateinit var ageInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
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
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        locationInput = findViewById(R.id.locationInput)
        contactInput = findViewById(R.id.contactInput)
        ageInput = findViewById(R.id.ageInput)
        passwordInput = findViewById(R.id.passwordInput)
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
                usernameInput.error = "Username cannot contain spaces"
                usernameInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }

            if (contact.length != 11) {
                contactInput.error = "Invalid contact number"
                contactInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            }

            if (age.contains(" ")) {
                ageInput.error = "Age cannot contain spaces"
                ageInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            } else {
                val ageInt = age.toIntOrNull()
                if (ageInt == null || ageInt < 9 || ageInt > 80) {
                    ageInput.error = "Age must be between 9 and 80"
                    ageInput.setBackgroundResource(R.drawable.edittext_error_background)
                    isValid = false
                }
            }

            if (password.length < 6 || password.length > 16) { // Fixed password length validation
                passwordInput.error = "Password must be between 6 and 16 characters"
                passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            } else if (password.contains(" ")) {
                passwordInput.error = "Password cannot contain spaces"
                passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
                isValid = false
            } else if (password != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                confirmPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
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
                    usernameInput.error = "Username is Required"
                    usernameInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    usernameInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = emailInput.text.toString().trim()
                if (email.isBlank()) {
                    emailInput.error = "Email is Required"
                    emailInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    emailInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val location = locationInput.text.toString().trim()
                if (location.isBlank()) {
                    locationInput.error = "Location is Required"
                    locationInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    locationInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        ageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val age = ageInput.text.toString().trim()
                if (age.isBlank()) {
                    ageInput.error = "Age is Required"
                    ageInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    ageInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        contactInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val contact = contactInput.text.toString().trim()
                if (contact.isBlank()) {
                    contactInput.error = "Contact Number is Required"
                    contactInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    contactInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val password = passwordInput.text.toString().trim()
                if (password.isBlank()) {
                    passwordInput.error = "Password is Required"
                    passwordInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    passwordInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        confirmPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val confirmPassword = confirmPasswordInput.text.toString().trim()
                if (confirmPassword.isBlank()) {
                    confirmPasswordInput.error = "Confirm Password is Required"
                    confirmPasswordInput.setBackgroundResource(R.drawable.edittext_error_background)
                } else {
                    confirmPasswordInput.setBackgroundResource(R.drawable.login_design)
                }
            }
        }

        // Text change listeners to clear errors
        usernameInput.addTextChangedListener {
            usernameInput.error = null
            usernameInput.setBackgroundResource(R.drawable.login_design)
        }

        emailInput.addTextChangedListener {
            emailInput.error = null
            emailInput.setBackgroundResource(R.drawable.login_design)
        }

        locationInput.addTextChangedListener {
            locationInput.error = null
            locationInput.setBackgroundResource(R.drawable.login_design)
        }

        contactInput.addTextChangedListener {
            contactInput.error = null
            contactInput.setBackgroundResource(R.drawable.login_design)
        }

        ageInput.addTextChangedListener {
            ageInput.error = null
            ageInput.setBackgroundResource(R.drawable.login_design)
        }

        passwordInput.addTextChangedListener {
            passwordInput.error = null
            passwordInput.setBackgroundResource(R.drawable.login_design)
        }

        confirmPasswordInput.addTextChangedListener {
            confirmPasswordInput.error = null
            confirmPasswordInput.setBackgroundResource(R.drawable.login_design)
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
        passwordInput.setCompoundDrawableClickListener {
            isPasswordVisible = !isPasswordVisible
            passwordInput.transformationMethod =
                if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
        }

        confirmPasswordInput.setCompoundDrawableClickListener {
            isPasswordVisible = !isPasswordVisible
            confirmPasswordInput.transformationMethod =
                if (isPasswordVisible) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            confirmPasswordInput.setSelection(confirmPasswordInput.text?.length ?: 0)
        }
    }

    private fun registerUser(username: String, email: String, location: String, contact: String, age: String, password: String) {
        val url = "http://192.168.168.55/backend/mobile_register.php"

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