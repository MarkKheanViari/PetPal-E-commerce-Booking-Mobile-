package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProductDetailsActivity : AppCompatActivity() {

    // Using a default quantity of 1 (since the XML does not include quantity controls)
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Retrieve product data passed via Intent
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productDescription = intent.getStringExtra("productDescription")
        val productPrice = intent.getStringExtra("productPrice")?.toDoubleOrNull() ?: 0.0

        // Bind UI elements (IDs updated to match XML)
        val productNameTextView = findViewById<TextView>(R.id.productName)
        val productImageView = findViewById<ImageView>(R.id.productImageView)
        val productDescriptionTextView = findViewById<TextView>(R.id.productDescription)
        val productPriceTextView = findViewById<TextView>(R.id.prduct_price)

        // Set product details into views
        productNameTextView.text = productName
        productDescriptionTextView.text = productDescription
        productPriceTextView.text = "₱$productPrice"
        Picasso.get().load(productImage).into(productImageView)

        // Set up click listener for the Add to Cart container
        val addToCartContainer = findViewById<LinearLayout>(R.id.addtocart_container)
        addToCartContainer.setOnClickListener {
            if (productId != -1) {
                addToCart(productId, quantity)
            } else {
                Toast.makeText(this, "❌ Failed to add product to cart", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up click listener for the Buy Now container
        val buyNowContainer = findViewById<LinearLayout>(R.id.buynow_container)
        buyNowContainer.setOnClickListener {
            if (productId != -1) {
                goToCheckout(productId, productName, productPrice, quantity)
            } else {
                Toast.makeText(this, "❌ Failed to proceed to checkout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to add product to cart
    private fun addToCart(productId: Int, quantity: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to add to cart", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", productId)
            put("quantity", quantity)
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.12/backend/add_to_cart.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProductDetailsActivity,
                        "❌ Failed to connect to server",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val success = jsonResponse.optBoolean("success", false)
                        if (success) {
                            Toast.makeText(
                                this@ProductDetailsActivity,
                                "✅ Added $quantity item(s) to Cart!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ProductDetailsActivity,
                                "❌ Error: ${jsonResponse.optString("message")}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    // Function to navigate to checkout
    private fun goToCheckout(
        productId: Int,
        productName: String?,
        productPrice: Double,
        quantity: Int
    ) {
        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra("productId", productId)
            putExtra("productName", productName)
            putExtra("productPrice", productPrice)
            putExtra("quantity", quantity)
        }
        startActivity(intent)
    }
}
