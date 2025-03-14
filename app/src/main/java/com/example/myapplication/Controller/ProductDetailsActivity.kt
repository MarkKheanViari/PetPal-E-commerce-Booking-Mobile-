package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

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

        // Debug the raw extras to see what type productPrice is
        val extras = intent.extras
        extras?.keySet()?.forEach { key ->
            Log.d("ProductDetailsActivity", "Extra: $key = ${extras[key]} (${extras[key]?.javaClass?.simpleName})")
        }

        // Retrieve productPrice as a String first, then convert to Double
        val productPriceString = intent.getStringExtra("productPrice")
        val productPrice = try {
            productPriceString?.toDouble() ?: intent.getDoubleExtra("productPrice", 0.0)
        } catch (e: NumberFormatException) {
            Log.e("ProductDetailsActivity", "Failed to parse productPrice: $productPriceString", e)
            0.0
        }

        Log.d("ProductDetailsActivity", "Received productPrice: $productPrice")

        // Find UI Elements
        val productNameTextView = findViewById<TextView>(R.id.productName)
        val productImageView = findViewById<ImageView>(R.id.productImageView)
        val productDescriptionTextView = findViewById<TextView>(R.id.productDescription)
        val productPriceTextView = findViewById<TextView>(R.id.prduct_price) // Fix typo if needed
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        val addToCartButton = findViewById<MaterialButton>(R.id.addtocart_container)
        val buyNowButton = findViewById<MaterialButton>(R.id.buynow_container)

        // Display product details
        productNameTextView.text = productName ?: "N/A"
        productDescriptionTextView.text = productDescription ?: "No description available."
        productPriceTextView.text = "₱$productPrice"

        // Load product image with Glide
        if (!productImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(productImage)
                .placeholder(R.drawable.cat)
                .error(R.drawable.oranage_header)
                .into(productImageView)
        } else {
            productImageView.setImageResource(R.drawable.cat)
        }

        // Back button
        backBtn.setOnClickListener { finish() }

        // "Add to Cart" functionality
        addToCartButton.setOnClickListener {
            if (productId != -1) {
                addToCart(productId, quantity)
            } else {
                Toast.makeText(this, "❌ Failed to add product to cart", Toast.LENGTH_SHORT).show()
            }
        }

        // "Buy Now" functionality
        buyNowButton.setOnClickListener {
            if (productId != -1) {
                goToCheckout(
                    productId,
                    productName,
                    productDescription,
                    productPrice, // Pass the corrected price
                    quantity,
                    productImage
                )
            } else {
                Toast.makeText(this, "❌ Failed to proceed to checkout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Display a custom "Added to Cart" toast in the top-right corner.
     */
    private fun showAddedToCartToast() {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.added_to_cart, null)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

    /**
     * Adds the product to the cart by sending a POST request.
     */
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
            .url("http://192.168.1.15/backend/add_to_cart.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            showAddedToCartToast()
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Create a single-item cart and start CheckoutActivity.
     */
    private fun goToCheckout(
        productId: Int,
        productName: String?,
        productDescription: String?,
        productPrice: Double,
        quantity: Int,
        productImage: String?
    ) {
        val productMap = HashMap<String, String>().apply {
            put("product_id", productId.toString())
            put("name", productName ?: "")
            put("price", productPrice.toString()) // Ensure price is passed as a string
            put("image", productImage ?: "")
            put("quantity", quantity.toString())
        }
        val cartItems = arrayListOf(productMap)

        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra("cartItems", cartItems)
        }
        startActivity(intent)
    }
}
