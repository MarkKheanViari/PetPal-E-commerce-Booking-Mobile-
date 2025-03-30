package com.example.myapplication

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class ReceiptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // Set up the toolbar and its back button functionality
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish() // Finish the activity when the back button is pressed
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvReceipts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load receipts from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val receiptsString = sharedPreferences.getString("receipt_details_list", null)
        val receiptsArray = if (receiptsString != null) JSONArray(receiptsString) else JSONArray()

        // Set up the adapter with a click listener that shows the full receipt dialog
        val adapter = ReceiptAdapter(receiptsArray) { receipt ->
            showReceiptDetailsDialog(receipt)
        }
        recyclerView.adapter = adapter
    }

    // This function creates and shows a dialog with full receipt details for one appointment
    private fun showReceiptDetailsDialog(receipt: JSONObject) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        // You can reuse your existing layout_receipt_dialog.xml if desired.
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
            "Service Type: ${receipt.optString("service_type", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptServiceName).text =
            "Service Name: ${receipt.optString("service_name", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptName).text =
            "Name: ${receipt.optString("name", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptAddress).text =
            "Address: ${receipt.optString("address", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptPhone).text =
            "Phone: ${receipt.optString("phone_number", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetName).text =
            "Pet Name: ${receipt.optString("pet_name", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptPetBreed).text =
            "Pet Breed: ${receipt.optString("pet_breed", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptDate).text =
            "Date: ${receipt.optString("appointment_date", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptTime).text =
            "Time: ${receipt.optString("appointment_time", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptPaymentMethod).text =
            "Payment Method: ${receipt.optString("payment_method", "N/A")}"
        dialog.findViewById<TextView>(R.id.tvReceiptNotes).text =
            "Notes: ${receipt.optString("notes", "N/A")}"

        dialog.findViewById<Button>(R.id.btnCloseReceipt).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
