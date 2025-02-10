package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CheckoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        val productName = intent.getStringExtra("productName")
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val quantity = intent.getIntExtra("quantity", 1)
        val totalPrice = productPrice * quantity

        // UI Elements
        val productNameTextView = findViewById<TextView>(R.id.productNameTextView)
        val quantityTextView = findViewById<TextView>(R.id.quantityTextView)
        val totalTextView = findViewById<TextView>(R.id.totalTextView)
        val placeOrderButton = findViewById<Button>(R.id.placeOrderButton)

        val cartItems = intent.getSerializableExtra("cartItems") as ArrayList<HashMap<String, String>>
        for (item in cartItems) {
            // Access item details like item["name"], item["price"], item["quantity"]
        }


        // Set UI values
        productNameTextView.text = productName
        quantityTextView.text = "Quantity: $quantity"
        totalTextView.text = "Total: ₱$totalPrice"

        // Place Order button click
        placeOrderButton.setOnClickListener {
            Toast.makeText(this, "✅ Order placed successfully!", Toast.LENGTH_SHORT).show()
            finish() // Close the activity after order is placed
        }
    }
}
