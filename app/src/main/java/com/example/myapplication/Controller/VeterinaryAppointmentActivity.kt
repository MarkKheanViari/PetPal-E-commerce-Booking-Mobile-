package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Calendar

class VeterinaryAppointmentActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPetName: EditText
    private lateinit var etPetBreed: EditText
    private lateinit var etNotes: EditText            // Example "notes" field
    private lateinit var btnPickDate: Button
    private lateinit var spinnerPaymentMethod: Spinner
    private lateinit var checkupTypeField: EditText
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veterinary_appointment)

        // Initialize views
        checkupTypeField = findViewById(R.id.checkupTypeField)
        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        etPhone = findViewById(R.id.etPhone)
        etPetName = findViewById(R.id.etPetName)
        etPetBreed = findViewById(R.id.etPetBreed)
        etNotes = findViewById(R.id.etNotes)        // Example "notes" field
        btnPickDate = findViewById(R.id.btnPickDate)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        val scheduleButton: Button = findViewById(R.id.btnScheduleAppointment)

        // Get service name from intent and display it as non-editable, bold, and centered
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        serviceName?.let {
            checkupTypeField.setText(it)
            checkupTypeField.isFocusable = false
            checkupTypeField.gravity = Gravity.CENTER
            checkupTypeField.setTextAppearance(android.R.style.TextAppearance_Medium)
        }

        // Handle date picker
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                btnPickDate.text = selectedDate
            }, year, month, day)

            datePickerDialog.show()
        }

        // Handle appointment submission
        scheduleButton.setOnClickListener {
            Log.d("Appointment", "Schedule Appointment clicked")
            submitAppointment()
        }
    }

    private fun submitAppointment() {
        val url = "http://192.168.168.55/backend/schedule_appointment.php"

        // Get user details from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1).toString()
        val savedLocation = sharedPreferences.getString("location", "")
        val savedPhoneNumber = sharedPreferences.getString("contact_number", "")

        if (mobileUserId == "-1") {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a date!", Toast.LENGTH_SHORT).show()
            return
        }

        // Auto-fill address & phone number if blank
        val address = if (etAddress.text.toString().trim().isEmpty()) savedLocation else etAddress.text.toString().trim()
        val phoneNumber = if (etPhone.text.toString().trim().isEmpty()) savedPhoneNumber else etPhone.text.toString().trim()

        // Prepare data with service_name
        val params = mapOf(
            "mobile_user_id" to mobileUserId,
            "service_type" to "Veterinary",
            "service_name" to checkupTypeField.text.toString().trim(),
            "name" to etName.text.toString().trim(),
            "address" to address,
            "phone_number" to phoneNumber,
            "pet_name" to etPetName.text.toString().trim(),
            "pet_breed" to etPetBreed.text.toString().trim(),
            "appointment_date" to selectedDate!!,
            "payment_method" to spinnerPaymentMethod.selectedItem.toString().trim(),
            "notes" to etNotes.text.toString().trim()        // Example "notes" param
        )

        val jsonObject = JSONObject(params)

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("Appointment", "Appointment scheduled: $response")
                Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()
                clearFields() // Clear fields on success
            },
            { error ->
                // Print detailed error info for debugging
                error.printStackTrace()

                val statusCode = error.networkResponse?.statusCode
                Log.e("Appointment", "Error scheduling (status code $statusCode): ${error.message}")

                val data = error.networkResponse?.data
                if (data != null) {
                    val body = String(data, Charsets.UTF_8)
                    Log.e("Appointment", "Error body: $body")
                }

                Toast.makeText(this, "Failed to schedule appointment", Toast.LENGTH_SHORT).show()
                clearFields() // Also clear fields on error if desired
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    // Helper function to clear all user-input fields
    private fun clearFields() {
        etName.text.clear()
        etAddress.text.clear()
        etPhone.text.clear()
        etPetName.text.clear()
        etPetBreed.text.clear()
        etNotes.text.clear()         // Clear notes
        btnPickDate.text = "Pick Date"
        spinnerPaymentMethod.setSelection(0)
        selectedDate = null
    }
}
