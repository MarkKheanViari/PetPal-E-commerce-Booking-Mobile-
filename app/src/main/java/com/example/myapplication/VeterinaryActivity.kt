package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class VeterinaryActivity : AppCompatActivity() {

    private lateinit var datePicker: DatePicker
    private lateinit var checkupTypeSpinner: Spinner
    private lateinit var paymentMethodSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veterinary)

        // Initialize UI Components
        datePicker = findViewById(R.id.datePicker)
        checkupTypeSpinner = findViewById(R.id.checkupTypeSpinner)
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner)

        val scheduleButton: Button = findViewById(R.id.scheduleButton)
        scheduleButton.setOnClickListener {
            submitVeterinaryAppointment()
        }

        // Initialize Checkup Type Spinner
        val checkupTypes = arrayOf(
            "── Routine Checkups ──",
            "Wellness Exam", "Vaccination Check", "Parasite Screening", "Dental Checkup", "Nutritional Consultation",

            "── Specialized Checkups ──",
            "Blood Tests & Lab Work", "X-ray & Ultrasound", "Allergy Testing", "Skin & Coat Exam", "Eye & Ear Exam",

            "── Senior Pet Checkups ──",
            "Arthritis & Joint Health Check", "Kidney & Liver Function Tests", "Heart Health Check",

            "── Emergency & Illness Checkups ──",
            "Injury or Trauma Exam", "Gastrointestinal Check", "Behavioral Consultation"
        )

        val checkupAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, checkupTypes)
        checkupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        checkupTypeSpinner.adapter = checkupAdapter

        // ✅ Initialize Payment Method Spinner
        val paymentMethods = arrayOf("Cash (on site)", "GCash (online)")
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentMethodSpinner.adapter = paymentAdapter
    }

    private fun submitVeterinaryAppointment() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val name = findViewById<EditText>(R.id.nameInput).text.toString().trim()
        val address = findViewById<EditText>(R.id.addressInput).text.toString().trim()
        val phoneNumber = findViewById<EditText>(R.id.phoneNumberInput).text.toString().trim()
        val petName = findViewById<EditText>(R.id.petNameInput).text.toString().trim()
        val notes = findViewById<EditText>(R.id.notesInput).text.toString().trim()

        val petBreed = when {
            findViewById<CheckBox>(R.id.catCheckbox).isChecked -> "Cat"
            findViewById<CheckBox>(R.id.dogCheckbox).isChecked -> "Dog"
            else -> ""
        }

        val checkupType = checkupTypeSpinner.selectedItem.toString()
        val paymentMethod = paymentMethodSpinner.selectedItem.toString()

        // ✅ Convert selected date to MM/DD/YYYY format
        val selectedDate = String.format("%02d/%02d/%04d", datePicker.month + 1, datePicker.dayOfMonth, datePicker.year)

        Log.d("VeterinaryActivity", "Formatted Date Sent: $selectedDate") // Debugging log

        if (name.isEmpty() || address.isEmpty() || phoneNumber.isEmpty() || petName.isEmpty() || petBreed.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("user_id", userId)
            put("name", name)
            put("address", address)
            put("phone_number", phoneNumber)
            put("pet_name", petName)
            put("pet_breed", petBreed)
            put("checkup_type", checkupType)
            put("notes", notes)
            put("appointment_date", selectedDate) // ✅ Send MM/DD/YYYY format
            put("payment_method", paymentMethod)
        }

        Log.d("VeterinaryActivity", "Final JSON Sent: $jsonObject") // Debugging log

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/submit_veterinary_appointment.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VeterinaryActivity", "Network error", e)
                runOnUiThread {
                    Toast.makeText(this@VeterinaryActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("VeterinaryActivity", "Response: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody?.contains("success") == true) {
                        Toast.makeText(this@VeterinaryActivity, "Appointment Scheduled Successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@VeterinaryActivity, "Failed to schedule appointment.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


}
