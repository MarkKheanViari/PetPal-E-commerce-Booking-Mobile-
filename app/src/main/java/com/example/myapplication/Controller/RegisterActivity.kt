package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.ViewGroup
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

    private var popupWindow: PopupWindow? = null
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

            // Validate username: must not be empty and letters only
            if (username.isEmpty()) {
                usernameLayout.error = "Username is Required"
                isValid = false
            } else if (!username.matches(Regex("^[A-Za-z]+\$"))) {
                usernameLayout.error = "Username must consist of letters only (no spaces, numbers, or symbols)"
                isValid = false
            }

            // Validate email: must not be empty, no spaces, must match email pattern,
            // and the domain must be one of the allowed: @gmail.com, @yahoo.com, or @phinmaed.com
            if (email.isEmpty()) {
                emailLayout.error = "Email is Required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Invalid email address"
                isValid = false
            } else if (email.contains(" ")) {
                emailLayout.error = "Email cannot contain spaces"
                isValid = false
            } else {
                val allowedDomains = listOf("@gmail.com")
                // Check if the email ends with one of the allowed domains (ignoring case)
                if (allowedDomains.none { email.lowercase().endsWith(it) }) {
                    emailLayout.error = "Email must end with a valid email address"
                    isValid = false
                }
            }

            // Validate location
            if (location.isEmpty()) {
                locationLayout.error = "Location is Required"
                isValid = false
            }

            // Validate contact: must be 11 characters
            if (contact.isEmpty()) {
                contactLayout.error = "Contact Number is Required"
                isValid = false
            } else if (contact.length != 11) {
                contactLayout.error = "Invalid contact number"
                isValid = false
            }

            // Validate age: must not be empty, no spaces and between 9 and 80
            if (age.isEmpty()) {
                ageLayout.error = "Age is Required"
                isValid = false
            } else if (age.contains(" ")) {
                ageLayout.error = "Age cannot contain spaces"
                isValid = false
            } else {
                val ageInt = age.toIntOrNull()
                if (ageInt == null || ageInt < 9 || ageInt > 80) {
                    ageLayout.error = "Age must be between 9 and 80"
                    isValid = false
                }
            }

            // Validate password and confirm password
            if (password.isEmpty() || confirmPassword.isEmpty()) {
                passwordLayout.error = "Password is Required"
                isValid = false
            } else if (password.length < 6 || password.length > 16) {
                passwordLayout.error = "Password must be between 6 to 16 characters"
                isValid = false
            } else if (password.contains(" ")) {
                passwordLayout.error = "Password cannot contain spaces"
                isValid = false
            } else if (password != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
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

        // Set up text view underlines and colors for links
        termsLink.paintFlags = termsLink.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        termsLink.setTextColor(getColor(R.color.smth_orange))
        privacyLink.paintFlags = privacyLink.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        privacyLink.setTextColor(getColor(R.color.smth_orange))
        loginSugg.paintFlags = loginSugg.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        loginSugg.setTextColor(getColor(R.color.smth_orange))

        // Focus listeners to validate fields when focus is lost
        usernameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && usernameInput.text.toString().trim().isEmpty()) {
                usernameLayout.error = "Username is Required"
            }
        }
        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && emailInput.text.toString().trim().isEmpty()) {
                emailLayout.error = "Email is Required"
            }
        }
        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && locationInput.text.toString().trim().isEmpty()) {
                locationLayout.error = "Location is Required"
            }
        }
        ageInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && ageInput.text.toString().trim().isEmpty()) {
                ageLayout.error = "Age is Required"
            }
        }
        contactInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && contactInput.text.toString().trim().isEmpty()) {
                contactLayout.error = "Contact Number is Required"
            }
        }
        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && passwordInput.text.toString().trim().isEmpty()) {
                passwordLayout.error = "Password is Required"
            }
        }
        confirmPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && confirmPasswordInput.text.toString().trim().isEmpty()) {
                confirmPasswordLayout.error = "Confirm Password is Required"
            }
        }

        // Clear errors on text change
        usernameInput.addTextChangedListener { usernameLayout.error = null }
        emailInput.addTextChangedListener { emailLayout.error = null }
        locationInput.addTextChangedListener { locationLayout.error = null }
        contactInput.addTextChangedListener { contactLayout.error = null }
        ageInput.addTextChangedListener { ageLayout.error = null }
        passwordInput.addTextChangedListener { passwordLayout.error = null }
        confirmPasswordInput.addTextChangedListener { confirmPasswordLayout.error = null }

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

        // Popup for location input example
        locationInput.setOnClickListener {
            popupWindow?.dismiss() // Dismiss any existing popup first
            val popupView = layoutInflater.inflate(R.layout.popup_address_format, null)
            val popupText = popupView.findViewById<TextView>(R.id.popupText)
            popupText.text = "Proper Address Format:\nStreet, City, State, ZIP Code\nExample: 123 Main St, Springfield, IL, 62701"
            popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                elevation = 8f
                isOutsideTouchable = true
                showAsDropDown(locationLayout, 0, 0)
            }
        }
    }

    private fun registerUser(username: String, email: String, location: String, contact: String, age: String, password: String) {
<<<<<<< Updated upstream
        val url = "http://192.168.43.55/backend/mobile_register.php"

=======
        val url = "http://192.168.80.63/backend/mobile_register.php"
>>>>>>> Stashed changes
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
