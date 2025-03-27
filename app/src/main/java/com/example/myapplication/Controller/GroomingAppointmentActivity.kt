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
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class GroomingAppointmentActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var receiptIcon: ImageView // Receipt icon in the toolbar
    private lateinit var etNameLayout: TextInputLayout
    private lateinit var etNameInput: TextInputEditText
    private lateinit var etAddresslayout: TextInputLayout
    private lateinit var etAddressInput: TextInputEditText
    private lateinit var etPhoneLayout: TextInputLayout
    private lateinit var etPhoneInput: TextInputEditText
    private lateinit var etPetNameLayout: TextInputLayout
    private lateinit var etPetNameInput: TextInputEditText
    private lateinit var etPetBreedLayout: TextInputLayout
    private lateinit var etPetBreedInput: TextInputEditText
    private lateinit var etNoteslayout: TextInputLayout
    private lateinit var etNotesInput: TextInputEditText
    private lateinit var btnPickDate: Button
    private lateinit var spinnerPaymentMethod: Spinner
    private var selectedTime: String? = null
    private lateinit var spinnerPickTime: Spinner
    private lateinit var groomTypeField: EditText
    private var selectedDate: String? = null
    private lateinit var servicePrice: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grooming_appointment)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        receiptIcon = findViewById(R.id.appointment_order) // Receipt icon from toolbar
        groomTypeField = findViewById(R.id.groomTypeField)
        etNameLayout = findViewById(R.id.etNameLayout)
        etNameInput = findViewById(R.id.etNameInput)
        etAddresslayout = findViewById(R.id.etAddressLayout)
        etAddressInput = findViewById(R.id.etAddressInput)
        etPhoneLayout = findViewById(R.id.etPhoneLayout)
        etPhoneInput = findViewById(R.id.etPhoneInput)
        etPetNameLayout = findViewById(R.id.etPetNameLayout)
        etPetNameInput = findViewById(R.id.etPetNameInput)
        etPetBreedLayout = findViewById(R.id.etPetBreedLayout)
        etPetBreedInput = findViewById(R.id.etPetBreedInput)
        etNoteslayout = findViewById(R.id.etNotesLayout)
        etNotesInput = findViewById(R.id.etNotesInput)
        btnPickDate = findViewById(R.id.btnPickDate)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        val scheduleButton: Button = findViewById(R.id.btnScheduleAppointment)

        // Retrieve the service price from the Intent
        servicePrice = intent.getStringExtra("SERVICE_PRICE") ?: "500.00"

        // Back button navigation
        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECTED_TAB", "menu_service")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Set click listener for receipt icon to view saved receipt details later
        receiptIcon.setOnClickListener {
            val intent = Intent(this, ReceiptActivity::class.java)
            startActivity(intent)
        }

        // Display service name as non-editable, bold, and centered
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        serviceName?.let {
            groomTypeField.setText(it)
            groomTypeField.isFocusable = false
            groomTypeField.gravity = Gravity.CENTER
            groomTypeField.setTextAppearance(android.R.style.TextAppearance_Medium)
        }

        // Handle date picker (disable past dates)
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

        spinnerPickTime = findViewById(R.id.spinnerPickTime)
        spinnerPickTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTime = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTime = null
            }
        }

        val imageUrl = intent.getStringExtra("SERVICE_IMAGE")
        val serviceImageView: ImageView = findViewById(R.id.serviceImage)

        Glide.with(this)
            .load("http://192.168.1.65/backend/$imageUrl") // Replace with your IP/domain if different
            .placeholder(R.drawable.cat)
            .into(serviceImageView)

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun convertTo24HourFormat(time: String): String {
        val (hourMin, period) = time.split(" ")
        var (hour, min) = hourMin.split(":").map { it.toInt() }
        if (period == "PM" && hour != 12) hour += 12
        if (period == "AM" && hour == 12) hour = 0
        return String.format("%02d:%02d:00", hour, min)
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
                }
            }
        }
    }

    private fun submitAppointment() {
        // Retrieve user details from SharedPreferences
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

        if (selectedTime.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a time!", Toast.LENGTH_SHORT).show()
            return
        }

        val address = if (etAddressInput.text.toString().trim().isEmpty()) savedLocation else etAddressInput.text.toString().trim()
        val phoneNumber = if (etPhoneInput.text.toString().trim().isEmpty()) savedPhoneNumber else etPhoneInput.text.toString().trim()
        val appointmentTime = convertTo24HourFormat(selectedTime!!)

        val params = mapOf(
            "mobile_user_id" to mobileUserId,
            "service_type" to "Grooming",
            "service_name" to groomTypeField.text.toString().trim(),
            "name" to etNameInput.text.toString().trim(),
            "address" to (address ?: ""),
            "phone_number" to (phoneNumber ?: ""),
            "pet_name" to etPetNameInput.text.toString().trim(),
            "pet_breed" to etPetBreedInput.text.toString().trim(),
            "appointment_date" to selectedDate!!,
            "appointment_time" to appointmentTime,
            "payment_method" to spinnerPaymentMethod.selectedItem.toString().trim(),
            "notes" to etNotesInput.text.toString().trim(),
            "price" to servicePrice
        )

        val jsonObject = JSONObject(params)
        val paymentMethod = spinnerPaymentMethod.selectedItem.toString().trim()
        Log.d("Appointment", "Selected Payment Method: '$paymentMethod'")

        if (paymentMethod.equals("GCASH", ignoreCase = true)) {
            Log.d("Appointment", "GCash payment method selected, initiating PayMongo flow")
            val url = "http://192.168.1.65/backend/paymongo_appointment_checkout.php"
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    Log.d("Appointment", "PayMongo response received: $response")
                    if (response.optBoolean("success", false)) {
                        val checkoutUrl = response.optString("checkout_url", "")
                        Log.d("Appointment", "Checkout URL: $checkoutUrl")
                        if (checkoutUrl.isNotEmpty()) {
                            val intent = Intent(this, WebViewActivity::class.java)
                            intent.putExtra("url", checkoutUrl)
                            intent.putExtra("source", "appointment")
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
            // Optional: extend timeout if needed
            request.retryPolicy = DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            Volley.newRequestQueue(this).add(request)
        } else {
            Log.d("Appointment", "Non-GCash payment method selected: $paymentMethod, using schedule_appointment.php")
            val url = "http://192.168.1.65/backend/schedule_appointment.php"
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    Log.d("Appointment", "Appointment scheduled: $response")
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()
                        // Append this appointment to the list of receipts (allowing multiple appointments)
                        saveReceiptDetails(params)
                        // Immediately show the receipt dialog for this appointment
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

    // Append the new appointment details to a JSON array stored in SharedPreferences
    private fun saveReceiptDetails(params: Map<String, String>) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val existingReceipts = sharedPreferences.getString("receipt_details_list", null)
        val receiptsArray = if (existingReceipts != null) JSONArray(existingReceipts) else JSONArray()
        receiptsArray.put(JSONObject(params))
        sharedPreferences.edit().putString("receipt_details_list", receiptsArray.toString()).apply()
    }

    // Create and display a dialog showing the details for the current appointment
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

        dialog.findViewById<TextView>(R.id.tvReceiptServiceType).text =
            "Service Type: ${params["service_type"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptServiceName).text =
            "Service Name: ${params["service_name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptName).text =
            "Name: ${params["name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptAddress).text =
            "Address: ${params["address"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPhone).text =
            "Phone: ${params["phone_number"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetName).text =
            "Pet Name: ${params["pet_name"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetBreed).text =
            "Pet Breed: ${params["pet_breed"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptDate).text =
            "Date: ${params["appointment_date"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptTime).text =
            "Time: ${params["appointment_time"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptPaymentMethod).text =
            "Payment Method: ${params["payment_method"] ?: "N/A"}"
        dialog.findViewById<TextView>(R.id.tvReceiptNotes).text =
            "Notes: ${params["notes"] ?: "N/A"}"

        dialog.findViewById<Button>(R.id.btnCloseReceipt).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearFields() {
        etNameInput.text?.clear()
        etAddressInput.text?.clear()
        etPhoneInput.text?.clear()
        etPetNameInput.text?.clear()
        etPetBreedInput.text?.clear()
        etNotesInput.text?.clear()
        btnPickDate.text = "Pick Date"
        spinnerPaymentMethod.setSelection(0)
        spinnerPickTime.setSelection(0)
        selectedDate = null
        selectedTime = null
    }
}
