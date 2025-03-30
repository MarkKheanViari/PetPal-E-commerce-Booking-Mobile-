package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderReceivedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_received)

        // Find UI elements
        val orderReceivedImage = findViewById<ImageView>(R.id.orderReceivedImage)
        val orderReceivedText = findViewById<TextView>(R.id.orderReceivedText)
        val continueShoppingButton = findViewById<Button>(R.id.continueShoppingButton)

        // Set the simplified confirmation message
        orderReceivedText.text = "Order Received! Thank you for your purchase."

        // Set the confirmation image (make sure this drawable exists in your resources)
        orderReceivedImage.setImageResource(R.drawable.delivery)

        // Set up continue shopping button to return to catalog
        continueShoppingButton.setOnClickListener {
            // Assuming MainActivity handles the catalog display
            val catalogIntent = Intent(this, MainActivity::class.java)
            catalogIntent.putExtra("navigate_to", "products") // Tells MainActivity to show the catalog
            catalogIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(catalogIntent)
            finish() // Close the OrderReceivedActivity
        }
    }
}
