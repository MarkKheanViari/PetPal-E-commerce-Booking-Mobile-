package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class ReceiptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        val receiptTextView = findViewById<TextView>(R.id.tvReceiptDetails)
        val closeButton = findViewById<Button>(R.id.btnCloseReceipt)
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val receiptString = sharedPreferences.getString("receipt_details", null)

        if (receiptString != null) {
            try {
                val receiptJson = JSONObject(receiptString)
                val receiptDetails = """
                    Service Type: ${receiptJson.optString("service_type", "N/A")}
                    Service Name: ${receiptJson.optString("service_name", "N/A")}
                    Name: ${receiptJson.optString("name", "N/A")}
                    Address: ${receiptJson.optString("address", "N/A")}
                    Phone: ${receiptJson.optString("phone_number", "N/A")}
                    Pet Name: ${receiptJson.optString("pet_name", "N/A")}
                    Pet Breed: ${receiptJson.optString("pet_breed", "N/A")}
                    Date: ${receiptJson.optString("appointment_date", "N/A")}
                    Time: ${receiptJson.optString("appointment_time", "N/A")}
                    Payment Method: ${receiptJson.optString("payment_method", "N/A")}
                    Notes: ${receiptJson.optString("notes", "N/A")}
                    Price: ${receiptJson.optString("price", "N/A")}
                """.trimIndent()
                receiptTextView.text = receiptDetails
            } catch (e: Exception) {
                receiptTextView.text = "Error loading receipt details."
            }
        } else {
            receiptTextView.text = "No receipt available."
        }

        closeButton.setOnClickListener {
            finish()
        }
    }
}
