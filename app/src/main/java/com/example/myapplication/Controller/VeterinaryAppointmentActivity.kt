package com.example.myapplication

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.Calendar

class VeterinaryAppointmentActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var etName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPetName: EditText
    private lateinit var etPetBreed: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnPickDate: Button
    private lateinit var spinnerPaymentMethod: Spinner
    private lateinit var checkupTypeField: EditText
    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veterinary_appointment)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        checkupTypeField = findViewById(R.id.checkupTypeField)
        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        etPhone = findViewById(R.id.etPhone)
        etPetName = findViewById(R.id.etPetName)
        etPetBreed = findViewById(R.id.etPetBreed)
        etNotes = findViewById(R.id.etNotes)
        btnPickDate = findViewById(R.id.btnPickDate)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        val scheduleButton: Button = findViewById(R.id.btnScheduleAppointment)

        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECTED_TAB", "menu_service")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Get service name from intent and display it as non-editable, bold, and centered
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        serviceName?.let {
            checkupTypeField.setText(it)
            checkupTypeField.isFocusable = false
            checkupTypeField.gravity = Gravity.CENTER
            checkupTypeField.setTextAppearance(android.R.style.TextAppearance_Medium)
        }

        // Handle date picker with restriction on past dates
        btnPickDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                btnPickDate.text = selectedDate
            }, year, month, day)

            // Set the minimum date to today to prevent selecting past dates
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            datePickerDialog.show()
        }

        // Handle appointment submission
        scheduleButton.setOnClickListener {
            Log.d("Appointment", "Schedule Appointment clicked")
            submitAppointment()
        }
    }

    private fun submitAppointment() {
        val url = "http://192.168.1.65/backend/schedule_appointment.php"

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

        // Prepare data with service_name, ensuring non-null values
        val params = mapOf(
            "mobile_user_id" to mobileUserId,
            "service_type" to "Veterinary",
            "service_name" to checkupTypeField.text.toString().trim(),
            "name" to etName.text.toString().trim(),
            "address" to (address ?: ""),
            "phone_number" to (phoneNumber ?: ""),
            "pet_name" to etPetName.text.toString().trim(),
            "pet_breed" to etPetBreed.text.toString().trim(),
            "appointment_date" to selectedDate!!,
            "payment_method" to spinnerPaymentMethod.selectedItem.toString().trim(),
            "notes" to etNotes.text.toString().trim()
        )

        val jsonObject = JSONObject(params)

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("Appointment", "Appointment scheduled: $response")
                Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()

                // Show receipt popup
                showReceiptDialog(params)
                clearFields()
            },
            { error ->
                error.printStackTrace()
                val statusCode = error.networkResponse?.statusCode
                Log.e("Appointment", "Error scheduling (status code $statusCode): ${error.message}")
                val data = error.networkResponse?.data
                if (data != null) {
                    val body = String(data, Charsets.UTF_8)
                    Log.e("Appointment", "Error body: $body")
                }
                Toast.makeText(this, "Failed to schedule appointment", Toast.LENGTH_SHORT).show()
                clearFields()
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showReceiptDialog(params: Map<String, String>) {
        Log.d("ReceiptDialog", "Params: $params")

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.layout_receipt_dialog)
        dialog.setCancelable(false)

        val window = dialog.window
        val layoutParams = window?.attributes ?: WindowManager.LayoutParams()
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        window?.attributes = layoutParams
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        window?.setGravity(Gravity.CENTER)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        window?.decorView?.setPadding(0, 0, 0, 0)

        dialog.findViewById<TextView>(R.id.tvReceiptServiceType).text = "Service Type: ${params["service_type"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Service Type: ${params["service_type"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptServiceName).text = "Service Name: ${params["service_name"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Service Name: ${params["service_name"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptName).text = "Name: ${params["name"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Name: ${params["name"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptAddress).text = "Address: ${params["address"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Address: ${params["address"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptPhone).text = "Phone: ${params["phone_number"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Phone: ${params["phone_number"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptPetName).text = "Pet Name: ${params["pet_name"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Pet Name: ${params["pet_name"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptPetBreed).text = "Pet Breed: ${params["pet_breed"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Pet Breed: ${params["pet_breed"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptDate).text = "Date: ${params["appointment_date"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Date: ${params["appointment_date"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptPaymentMethod).text = "Payment Method: ${params["payment_method"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Payment Method: ${params["payment_method"]}")

        dialog.findViewById<TextView>(R.id.tvReceiptNotes).text = "Notes: ${params["notes"] ?: "N/A"}"
        Log.d("ReceiptDialog", "Notes: ${params["notes"]}")

        dialog.findViewById<Button>(R.id.btnCloseReceipt).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearFields() {
        etName.text.clear()
        etAddress.text.clear()
        etPhone.text.clear()
        etPetName.text.clear()
        etPetBreed.text.clear()
        etNotes.text.clear()
        btnPickDate.text = "Pick Date"
        spinnerPaymentMethod.setSelection(0)
        selectedDate = null
    }
}