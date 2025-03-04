package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
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
    private lateinit var btnPickDate: Button
    private lateinit var spinnerPaymentMethod: Spinner
    private lateinit var checkupTypeField: EditText
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veterinary_appointment)

        // ✅ Initialize views
        checkupTypeField = findViewById(R.id.checkupTypeField)
        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        etPhone = findViewById(R.id.etPhone)
        etPetName = findViewById(R.id.etPetName)
        etPetBreed = findViewById(R.id.etPetBreed)
        btnPickDate = findViewById(R.id.btnPickDate)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        val scheduleButton: Button = findViewById(R.id.btnScheduleAppointment)

        // ✅ Set selected service name
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        serviceName?.let { checkupTypeField.setText(it) }

        // ✅ Handle date picker
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

        // ✅ Handle appointment submission
        scheduleButton.setOnClickListener {
            Log.d("Appointment", "✅ Schedule Appointment clicked")
            submitAppointment()
        }
    }

    private fun submitAppointment() {
        val url = "http://192.168.1.65/backend/schedule_appointment.php"

        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1).toString()

        if (mobileUserId == "-1") {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDate.isNullOrEmpty()) {
            Toast.makeText(this, "❌ Please select a date!", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Prepare data (Add service_name)
        val params = mapOf(
            "mobile_user_id" to mobileUserId,
            "service_type" to "Veterinary",
            "service_name" to checkupTypeField.text.toString().trim(),  // ✅ Add service_name
            "name" to etName.text.toString().trim(),
            "address" to etAddress.text.toString().trim(),
            "phone_number" to etPhone.text.toString().trim(),
            "pet_name" to etPetName.text.toString().trim(),
            "pet_breed" to etPetBreed.text.toString().trim(),
            "appointment_date" to selectedDate!!,
            "payment_method" to spinnerPaymentMethod.selectedItem.toString().trim()
        )

        val jsonObject = JSONObject(params)

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("Appointment", "✅ Appointment scheduled: $response")
                Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()
            },
            { error ->
                Log.e("Appointment", "❌ Error scheduling: ${error.message}")
                Toast.makeText(this, "Failed to schedule appointment", Toast.LENGTH_SHORT).show()
            })

        // ✅ Add request to queue
        Volley.newRequestQueue(this).add(request)
    }
}
