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

class GroomingActivity : AppCompatActivity() {

    private lateinit var datePicker: DatePicker
    private lateinit var groomTypeSpinner: Spinner
    private lateinit var paymentMethodSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grooming)

        datePicker = findViewById(R.id.datePicker)
        groomTypeSpinner = findViewById(R.id.groomTypeSpinner)
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner)

        val scheduleButton: Button = findViewById(R.id.scheduleButton)
        scheduleButton.setOnClickListener {
            submitAppointment()
        }

        val groomTypes = listOf(
            "Brushing", "Bathing", "Nail Trimming", "Ear Cleaning", "Teeth Brushing",
            "Deshedding Treatment", "Paw Pad Care", "Sanitary Trim", "Eye Cleaning",
            "Full Haircut/Styling", "Hand Stripping", "Flea & Tick Treatment", "Anal Gland Expression"
        )
        val groomAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groomTypes)
        groomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groomTypeSpinner.adapter = groomAdapter

        val paymentMethods = listOf("Cash (on site)", "GCash (online)")
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentMethodSpinner.adapter = paymentAdapter
    }

    private fun submitAppointment() {
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
            else -> "Unknown"
        }

        val groomType = groomTypeSpinner.selectedItem.toString()
        val paymentMethod = paymentMethodSpinner.selectedItem.toString()
        val selectedDate = "${datePicker.year}-${String.format("%02d", datePicker.month + 1)}-${String.format("%02d", datePicker.dayOfMonth)}"

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", userId)
            put("name", name)
            put("address", address)
            put("phone_number", phoneNumber)
            put("pet_name", petName)
            put("pet_breed", petBreed)
            put("groom_type", groomType)
            put("notes", notes)
            put("appointment_date", selectedDate)
            put("payment_method", paymentMethod)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/submit_grooming_appointment.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GroomingActivity", "Network error", e)
                runOnUiThread {
                    Toast.makeText(this@GroomingActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("GroomingActivity", "Response: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful && responseBody?.contains("success") == true) {
                        Toast.makeText(this@GroomingActivity, "Appointment Scheduled Successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@GroomingActivity, "Failed to schedule appointment.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
