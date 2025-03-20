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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProductDetailsActivity : AppCompatActivity() {

    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the provided XML layout without changes to IDs
        setContentView(R.layout.activity_product_details)

        // Retrieve product data from the Intent extras
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName") ?: "N/A"
        val productPriceString = intent.getStringExtra("productPrice") ?: "0.0"
        val productPrice = try {
            productPriceString.toDouble()
        } catch (e: NumberFormatException) {
            Log.e("ProductDetailsActivity", "Failed to parse productPrice: $productPriceString", e)
            0.0
        }
        val productDescription = intent.getStringExtra("productDescription") ?: ""
        val productImage = intent.getStringExtra("productImage") ?: ""

        // Find UI elements by their XML IDs
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toptoolbar)
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        val likedBtn = findViewById<ImageView>(R.id.likedBtn)  // in the toolbar
        val likeBtn = findViewById<ImageView>(R.id.likeBtn)    // separate like icon in layout
        val productImageView = findViewById<ImageView>(R.id.productImageView)
        val productNameTextView = findViewById<TextView>(R.id.productName)
        val productPriceTextView = findViewById<TextView>(R.id.prduct_price)
        val productDescriptionTextView = findViewById<TextView>(R.id.productDescription)
        val addToCartButton = findViewById<MaterialButton>(R.id.addtocart_container)
        val buyNowButton = findViewById<MaterialButton>(R.id.buynow_container)

        // Set product details on UI
        productNameTextView.text = productName
        productPriceTextView.text = "₱$productPrice"
        productDescriptionTextView.text = productDescription

        // Load product image using Glide
        if (productImage.isNotEmpty()) {
            Glide.with(this)
                .load(productImage)
                .placeholder(R.drawable.cat) // your placeholder drawable
                .error(R.drawable.oranage_header) // your error drawable
                .into(productImageView)
        } else {
            productImageView.setImageResource(R.drawable.cat)
        }

        // Back button: simply finish the activity
        backBtn.setOnClickListener { finish() }

        // Create a common click listener for both heart icons (likedBtn and likeBtn)
        val likeClickListener = {
            // Check if the user is logged in by retrieving "user_id" from SharedPreferences
            val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val userId = sharedPrefs.getInt("user_id", -1)
            if (userId == -1) {
                Toast.makeText(this, "Please log in to like products", Toast.LENGTH_SHORT).show()
                // Navigate to LoginActivity if not logged in
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                if (productId != -1) {
                    // Create a Product instance using the received data
                    val product = Product(
                        id = productId,
                        name = productName,
                        price = productPrice.toString(),
                        description = productDescription,
                        quantity = 1, // default quantity for liked products
                        imageUrl = productImage
                    )
                    // Call the like method with a callback that navigates to LikedProductsActivity
                    LikedProductsStore.addProduct(product) {
                        runOnUiThread {
                            startActivity(Intent(this, LikedProductsActivity::class.java))
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to add product to likes", Toast.LENGTH_SHORT).show()
                }
            }
        }
        likedBtn.setOnClickListener { likeClickListener() }
        likeBtn.setOnClickListener { likeClickListener() }

        // "Add to Cart" functionality
        addToCartButton.setOnClickListener {
            if (productId != -1) {
                addToCart(productId, quantity)
            } else {
                Toast.makeText(this, "Failed to add product to cart", Toast.LENGTH_SHORT).show()
            }
        }

        // "Buy Now" functionality
        buyNowButton.setOnClickListener {
            if (productId != -1) {
                goToCheckout(
                    productId,
                    productName,
                    productDescription,
                    productPrice,
                    quantity,
                    productImage
                )
            } else {
                Toast.makeText(this, "Failed to proceed to checkout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Adds the product to the cart by sending a POST request.
     */
    private fun addToCart(productId: Int, quantity: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Please login to add to cart", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        val jsonObject = JSONObject().apply {
            put("mobile_user_id", userId)
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
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@ProductDetailsActivity, "Added to cart", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Creates a single-item cart and starts CheckoutActivity.
     */
    private fun goToCheckout(
        productId: Int,
        productName: String,
        productDescription: String,
        productPrice: Double,
        quantity: Int,
        productImage: String
    ) {
        val productMap = HashMap<String, String>().apply {
            put("product_id", productId.toString())
            put("name", productName)
            put("price", productPrice.toString())
            put("image", productImage)
            put("quantity", quantity.toString())
        }
        val cartItems = arrayListOf(productMap)
        val intent = Intent(this, CheckoutActivity::class.java).apply {
            putExtra("cartItems", cartItems)
        }
        startActivity(intent)
    }
}
