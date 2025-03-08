package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AddAddressActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var regionEditText: EditText
    private lateinit var postalCodeEditText: EditText
    private lateinit var streetEditText: EditText
    private lateinit var workButton: Button
    private lateinit var homeButton: Button
    private lateinit var defaultAddressSwitch: Switch
    private lateinit var submitButton: Button
    private val client = OkHttpClient()
    private var labelAs = "Home"  // Default label

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_address)  // Ensure this is your layout file

        // Initialize UI elements
        fullNameEditText = findViewById(R.id.fullNameEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        regionEditText = findViewById(R.id.regionEditText)
        postalCodeEditText = findViewById(R.id.postalCodeEditText)
        streetEditText = findViewById(R.id.streetEditText)
        workButton = findViewById(R.id.workButton)
        homeButton = findViewById(R.id.homeButton)
        defaultAddressSwitch = findViewById(R.id.defaultAddressSwitch)
        submitButton = findViewById(R.id.submitButton)

        // Handle label selection
        workButton.setOnClickListener {
            labelAs = "Work"
            workButton.setBackgroundResource(R.drawable.selected_button_background)
            homeButton.setBackgroundResource(R.drawable.default_button_background)
        }

        homeButton.setOnClickListener {
            labelAs = "Home"
            homeButton.setBackgroundResource(R.drawable.selected_button_background)
            workButton.setBackgroundResource(R.drawable.default_button_background)
        }

        // Handle submit button click
        submitButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()
            val region = regionEditText.text.toString()
            val postalCode = postalCodeEditText.text.toString()
            val street = streetEditText.text.toString()
            val isDefault = if (defaultAddressSwitch.isChecked) "1" else "0"

            if (fullName.isNotEmpty() && phoneNumber.isNotEmpty() && region.isNotEmpty() && postalCode.isNotEmpty() && street.isNotEmpty()) {
                saveAddress(fullName, phoneNumber, region, postalCode, street, labelAs, isDefault)
            } else {
                Toast.makeText(this, "❌ Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun saveAddress(fullName: String, phoneNumber: String, region: String, postalCode: String, street: String, labelAs: String, isDefault: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to continue", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.58.55/backend/add_address.php"
        val json = """
        {
          "user_id": "$mobileUserId",
          "full_name": "$fullName",
          "phone_number": "$phoneNumber",
          "region": "$region",
          "postal_code": "$postalCode",
          "street": "$street",
          "label": "$labelAs",
          "is_default": "$isDefault"
        }
    """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddAddressActivity, "❌ Failed to save address", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@AddAddressActivity, "✅ Address saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        })
    }

}
