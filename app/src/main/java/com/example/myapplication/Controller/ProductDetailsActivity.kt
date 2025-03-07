package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class ProductDetailsActivity : AppCompatActivity() {

    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Retrieve product data from the Intent
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productDescription = intent.getStringExtra("productDescription")
        val productPrice = intent.getStringExtra("productPrice")?.toDoubleOrNull() ?: 0.0

        // UI Elements
        val productNameTextView = findViewById<TextView>(R.id.productName)
        val productImageView = findViewById<ImageView>(R.id.productImageView)
        val productDescriptionTextView = findViewById<TextView>(R.id.productDescription)
        val productPriceTextView = findViewById<TextView>(R.id.prduct_price)
        val backBtn = findViewById<ImageView>(R.id.backBtn)

        // Display product details
        productNameTextView.text = productName ?: "N/A"
        productDescriptionTextView.text = productDescription ?: "No description available."
        productPriceTextView.text = "₱$productPrice"

        // Load product image using Glide if available
        if (!productImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(productImage)
                .into(productImageView)
        }

        // Back button functionality
        backBtn.setOnClickListener { finish() }

        // Buy Now functionality passes details to CheckoutActivity
        val buyNowButton = findViewById<MaterialButton>(R.id.buynow_container)
        buyNowButton.setOnClickListener {
            if (productId != -1) {
                goToCheckout(productId, productName, productDescription, productPrice, quantity)
            } else {
                Toast.makeText(this, "❌ Failed to proceed to checkout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToCheckout(
        productId: Int,
        productName: String?,
        productDescription: String?,
        productPrice: Double,
        quantity: Int
    ) {
        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra("productId", productId)
            putExtra("productName", productName)
            putExtra("productDescription", productDescription)
            putExtra("productPrice", productPrice)
            putExtra("quantity", quantity)
        }
        startActivity(intent)
    }
}
