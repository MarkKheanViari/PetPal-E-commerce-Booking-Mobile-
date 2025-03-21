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
    private lateinit var servicePrice: String

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

        // Retrieve the service price from the Intent
        servicePrice = intent.getStringExtra("SERVICE_PRICE") ?: "500.00"

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

            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // Handle appointment submission
        scheduleButton.setOnClickListener {
            Log.d("Appointment", "Schedule Appointment clicked")
            submitAppointment()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            when (uri.path) {
                "/appointment/success" -> {
                    val appointmentId = uri.getQueryParameter("appointment_id")
                    Toast.makeText(this, "✅ Payment successful for Appointment #$appointmentId", Toast.LENGTH_LONG).show()
                    finish()
                }
                "/appointment/cancel" -> {
                    Toast.makeText(this, "⚠ Payment canceled", Toast.LENGTH_LONG).show()
                    // Stay in the activity to allow retry
                }
            }
        }
    }

    private fun submitAppointment() {
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

        // Prepare appointment data
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
            "notes" to etNotes.text.toString().trim(),
            "price" to servicePrice
        )

        val jsonObject = JSONObject(params)

        // Check payment method
        val paymentMethod = spinnerPaymentMethod.selectedItem.toString().trim()
        Log.d("Appointment", "Selected Payment Method: '$paymentMethod'")

        if (paymentMethod.equals("GCASH", ignoreCase = true)) {
            Log.d("Appointment", "GCash payment method selected, initiating PayMongo flow")
            val url = "http://192.168.1.12/backend/paymongo_appointment_checkout.php"
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    Log.d("Appointment", "PayMongo response received: $response")
                    if (response.optBoolean("success", false)) {
                        val checkoutUrl = response.optString("checkout_url", "")
                        Log.d("Appointment", "Checkout URL: $checkoutUrl")
                        if (checkoutUrl.isNotEmpty()) {
                            Log.d("Appointment", "Redirecting to WebViewActivity with URL: $checkoutUrl")
                            val intent = Intent(this, WebViewActivity::class.java)
                            intent.putExtra("url", checkoutUrl)
                            intent.putExtra("source", "appointment") // Indicate this is for an appointment
                            startActivity(intent)
                        } else {
                            Log.e("Appointment", "Missing checkout URL in response")
                            Toast.makeText(this, "❌ Error: Missing checkout URL", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMessage = response.optString("message", "Unknown error")
                        Log.e("Appointment", "PayMongo request failed: $errorMessage")
                        Toast.makeText(this, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    error.printStackTrace()
                    val statusCode = error.networkResponse?.statusCode
                    val errorBody = error.networkResponse?.data?.let { String(it, Charsets.UTF_8) }
                    Log.e("Appointment", "Error with PayMongo: ${error.message}, Status Code: $statusCode, Body: $errorBody")
                    Toast.makeText(this, "Failed to initiate GCash payment: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
            Volley.newRequestQueue(this).add(request)
        } else {
            Log.d("Appointment", "Non-GCash payment method selected: $paymentMethod, using schedule_appointment.php")
            val url = "http://192.168.1.12/backend/schedule_appointment.php"
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    Log.d("Appointment", "Appointment scheduled: $response")
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()
                        showReceiptDialog(params)
                        clearFields()
                    } else {
                        val errorMessage = response.optString("message", "Unknown error")
                        Log.e("Appointment", "Failed to schedule appointment: $errorMessage")
                        Toast.makeText(this, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    error.printStackTrace()
                    Log.e("Appointment", "Error scheduling: ${error.message}")
                    Toast.makeText(this, "Failed to schedule appointment", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
            )
            Volley.newRequestQueue(this).add(request)
        }
    }

    private fun showReceiptDialog(params: Map<String, String>) {
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
        dialog.findViewById<TextView>(R.id.tvReceiptServiceName).text = "Service Name: ${params["service_name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptName).text = "Name: ${params["name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptAddress).text = "Address: ${params["address"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPhone).text = "Phone: ${params["phone_number"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetName).text = "Pet Name: ${params["pet_name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetBreed).text = "Pet Breed: ${params["pet_breed"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptDate).text = "Date: ${params["appointment_date"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPaymentMethod).text = "Payment Method: ${params["payment_method"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptNotes).text = "Notes: ${params["notes"] ?: "N/A"}"

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