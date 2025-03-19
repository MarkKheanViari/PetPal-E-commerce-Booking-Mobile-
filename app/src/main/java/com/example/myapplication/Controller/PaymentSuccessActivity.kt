package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PaymentSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the deep link URI
        val data: Uri? = intent?.data
        Log.d("DeepLink", "Received URI: $data")

        // Extract the order_id from the deep link
        val orderId = data?.getQueryParameter("order_id") ?: "Unknown"

        if (orderId != "Unknown") {
            Toast.makeText(this, "✅ Payment Successful! Order ID: $orderId", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "❌ Payment Success: Missing Order ID!", Toast.LENGTH_LONG).show()
        }

        // Redirect to MainActivity after handling the deep link
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
