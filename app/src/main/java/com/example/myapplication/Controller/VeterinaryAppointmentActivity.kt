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
import androidx.core.widget.addTextChangedListener
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

class VeterinaryAppointmentActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var receiptIcon: ImageView
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
    private lateinit var checkupTypeField: EditText
    private var selectedDate: String? = null
    private lateinit var servicePrice: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_veterinary_appointment)

        // Initialize views
        backBtn = findViewById(R.id.backBtn)
        receiptIcon = findViewById(R.id.appointment_order)
        checkupTypeField = findViewById(R.id.checkupTypeField)
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

        // Retrieve service price, start_time, and end_time from the Intent
        servicePrice = intent.getStringExtra("SERVICE_PRICE") ?: "500.00"
        val startTime = intent.getStringExtra("START_TIME")
        val endTime = intent.getStringExtra("END_TIME")

        // Back button: navigate back to MainActivity with Service tab selected
        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECTED_TAB", "menu_service")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // When receipt icon is clicked, launch ReceiptActivity to view all saved appointments
        receiptIcon.setOnClickListener {
            val intent = Intent(this, ReceiptActivity::class.java)
            startActivity(intent)
        }

        // Display service name as non-editable, bold, and centered
        val serviceName = intent.getStringExtra("SERVICE_NAME")
        serviceName?.let {
            checkupTypeField.setText(it)
            checkupTypeField.isFocusable = false
            checkupTypeField.gravity = Gravity.CENTER
            checkupTypeField.setTextAppearance(android.R.style.TextAppearance_Medium)
        }

        // Optionally load a service image (if provided)
        val imageUrl = intent.getStringExtra("SERVICE_IMAGE")
        val serviceImageView: ImageView = findViewById(R.id.serviceImage)
        Glide.with(this)
            .load("http://192.168.43.55/backend/$imageUrl")
            .placeholder(R.drawable.cat)
            .into(serviceImageView)

        // Set up date picker (disable past dates)
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

        // Set up the time picker spinner dynamically based on start_time and end_time
        spinnerPickTime = findViewById(R.id.spinnerPickTime)
        val timeSlots = generateTimeSlots(startTime, endTime)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPickTime.adapter = adapter

        spinnerPickTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTime = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTime = null
            }
        }
        etNameInput.addTextChangedListener{
            etNameLayout.error = null
        }
        etPhoneInput.addTextChangedListener{
            etPhoneLayout.error = null
        }
        etAddressInput.addTextChangedListener{
            etAddresslayout.error = null
        }
        etPetNameInput.addTextChangedListener{
            etPetNameLayout.error = null
        }
        etPetBreedInput.addTextChangedListener{
            etPetBreedLayout.error = null
        }

        etAddressInput.setOnClickListener{
            etAddresslayout.helperText = "House Number, Street Name, Barangay, City/Municipality, Province"
        }

        etNotesInput.addTextChangedListener{
            etNoteslayout.error = null
        }
        
        // Handle appointment submission
        scheduleButton.setOnClickListener {
            val name = etNameInput.text.toString().trim()
            val phoneNumber = etPhoneInput.text.toString().trim()
            val address = etAddressInput.text.toString().trim()
            val petName = etPetNameInput.text.toString().trim()
            val petBreed = etPetBreedInput.text.toString().trim()
            val notes = etNotesInput.text.toString().trim()

            var isValid = true

            if (name.isEmpty()) {
                etNameLayout.error = "Please enter your name"
                isValid = false
            }

            if (phoneNumber.isEmpty()) {
                etPhoneLayout.error = "Please enter your phone number"
                isValid = false
            } else if (phoneNumber.length != 11) {
                etPhoneLayout.error = "Please enter a valid 11-digit phone number"
                isValid = false
            }

            if (address.isEmpty()) {
                etAddresslayout.error = "Please enter your address"
                isValid = false
            }

            if (petName.isEmpty()) {
                etPetNameLayout.error = "Please enter your pet's name"
                isValid = false
            }

            if (petBreed.isEmpty()) {
                etPetBreedLayout.error = "Please enter your pet's breed"
                isValid = false
            }

            if (selectedDate.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a date!", Toast.LENGTH_SHORT).show()
                isValid = false
            }
            if (isValid) {
                Log.d("Appointment", "Schedule Appointment clicked")
                submitAppointment()
            }
        }
    }

    // Generate time slots between startTime and endTime in 1-hour increments
    private fun generateTimeSlots(startTime: String?, endTime: String?): List<String> {
        val timeSlots = mutableListOf<String>()
        if (startTime == null || endTime == null) {
            timeSlots.add("Not specified")
            return timeSlots
        }

        try {
            // Parse start and end times (format: "HH:mm")
            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }

            var startHour = startParts[0]
            val startMinute = startParts[1]
            var endHour = endParts[0]
            val endMinute = endParts[1]

            // Convert to minutes for easier comparison
            var currentMinutes = startHour * 60 + startMinute
            val endMinutes = endHour * 60 + endMinute

            // Ensure start time is before end time
            if (currentMinutes >= endMinutes) {
                timeSlots.add("Invalid time range")
                return timeSlots
            }

            // Generate time slots in 1-hour increments
            while (currentMinutes <= endMinutes) {
                val hour = currentMinutes / 60
                val minute = currentMinutes % 60
                val period = if (hour < 12) "AM" else "PM"
                val displayHour = when {
                    hour == 0 -> 12 // 12 AM
                    hour > 12 -> hour - 12 // Convert to 12-hour format
                    else -> hour
                }
                val timeString = String.format("%d:%02d %s", displayHour, minute, period)
                timeSlots.add(timeString)

                // Increment by 1 hour (60 minutes)
                currentMinutes += 60
            }
        } catch (e: Exception) {
            Log.e("VeterinaryAppointment", "Error generating time slots: ${e.message}")
            timeSlots.add("Error generating time slots")
        }

        return timeSlots
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

        // Create parameters (note: service_type is Veterinary)
        val params = mapOf(
            "mobile_user_id" to mobileUserId,
            "service_type" to "Veterinary",
            "service_name" to checkupTypeField.text.toString().trim(),
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
            val url = "http://192.168.43.55/backend/paymongo_appointment_checkout.php"
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
            request.retryPolicy = DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            Volley.newRequestQueue(this).add(request)
        } else {
            Log.d("Appointment", "Non-GCash payment method selected: $paymentMethod, using schedule_appointment.php")
            val url = "http://192.168.43.55/backend/schedule_appointment.php"
            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    Log.d("Appointment", "Appointment scheduled: $response")
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Appointment Scheduled!", Toast.LENGTH_SHORT).show()
                        saveReceiptDetails(params)
                        addLocalNotification("Appointment Scheduled: ${checkupTypeField.text} on $selectedDate at $appointmentTime")
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

    private fun saveReceiptDetails(params: Map<String, String>) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val existingReceipts = sharedPreferences.getString("receipt_details_list", null)
        val receiptsArray = if (existingReceipts != null) JSONArray(existingReceipts) else JSONArray()
        receiptsArray.put(JSONObject(params))
        sharedPreferences.edit().putString("receipt_details_list", receiptsArray.toString()).apply()
    }

    private fun addLocalNotification(message: String) {
        val sp = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val existingString = sp.getString("notifications", "[]")
        val array = JSONArray(existingString)
        array.put(message)
        sp.edit().putString("notifications", array.toString()).apply()
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
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECTED_TAB", "menu_service")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
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