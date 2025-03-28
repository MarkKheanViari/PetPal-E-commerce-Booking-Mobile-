package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.DecimalFormat

class ProductDetailsActivity : AppCompatActivity() {

    private var quantity = 1
    private lateinit var ratingBar: RatingBar
    private lateinit var averageRatingBar: RatingBar
    private lateinit var averageRatingText: TextView
    private lateinit var totalRatingsText: TextView
    private lateinit var progress5Star: ProgressBar
    private lateinit var progress4Star: ProgressBar
    private lateinit var progress3Star: ProgressBar
    private lateinit var progress2Star: ProgressBar
    private lateinit var progress1Star: ProgressBar
    private lateinit var percent5Star: TextView
    private lateinit var percent4Star: TextView
    private lateinit var percent3Star: TextView
    private lateinit var percent2Star: TextView
    private lateinit var percent1Star: TextView
    private lateinit var likedBtn: ImageView
    private var isLiked = false

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
        val productPriceTextView = findViewById<TextView>(R.id.prduct_price)
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        val cartBtn = findViewById<ImageView>(R.id.cartBtn)
        likedBtn = findViewById(R.id.likedBtn)
        val addToCartButton = findViewById<MaterialButton>(R.id.addtocart_container)
        val buyNowButton = findViewById<MaterialButton>(R.id.buynow_container)
        ratingBar = findViewById(R.id.ratingBar)
        averageRatingBar = findViewById(R.id.average_rating_bar)
        averageRatingText = findViewById(R.id.average_rating_text)
        totalRatingsText = findViewById(R.id.total_ratings_text)
        progress5Star = findViewById(R.id.progress_5_star)
        progress4Star = findViewById(R.id.progress_4_star)
        progress3Star = findViewById(R.id.progress_3_star)
        progress2Star = findViewById(R.id.progress_2_star)
        progress1Star = findViewById(R.id.progress_1_star)
        percent5Star = findViewById(R.id.percent_5_star)
        percent4Star = findViewById(R.id.percent_4_star)
        percent3Star = findViewById(R.id.percent_3_star)
        percent2Star = findViewById(R.id.percent_2_star)
        percent1Star = findViewById(R.id.percent_1_star)

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

        cartBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "cart")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Like button functionality
        likedBtn.setOnClickListener {
            if (productId != -1) {
                if (isLiked) {
                    removeFromLikedProducts(productId)
                } else {
                    addToLikedProducts(productId)
                }
            } else {
                Toast.makeText(this, "❌ Failed to update liked products", Toast.LENGTH_SHORT).show()
            }
        }

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
                    productPrice,
                    quantity,
                    productImage
                )
            } else {
                Toast.makeText(this, "❌ Failed to proceed to checkout", Toast.LENGTH_SHORT).show()
            }
        }

        // RatingBar listener to submit or update rating
        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (fromUser && productId != -1) {
                submitReview(productId, rating.toInt())
            }
        }

        // Fetch and display rating statistics
        if (productId != -1) {
            fetchRatingStats(productId)
            // Fetch the user's current rating
            val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val mobileUserId = sharedPreferences.getInt("user_id", -1)
            if (mobileUserId != -1) {
                fetchUserRating(productId, mobileUserId)
                // Check if the product is liked
                checkIfProductIsLiked(productId, mobileUserId)
            }
        }
    }

    /**
     * Check if the product is already liked by the user.
     */
    private fun checkIfProductIsLiked(productId: Int, mobileUserId: Int) {
        val url = "http://192.168.1.15/backend/check_if_liked.php?mobile_user_id=$mobileUserId&product_id=$productId"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to check like status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return
                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    isLiked = json.optBoolean("is_liked", false)
                    runOnUiThread {
                        updateLikeButton()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ProductDetailsActivity, "❌ Error parsing like status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Update the like button icon based on the like status.
     */
    private fun updateLikeButton() {
        if (isLiked) {
            likedBtn.setImageResource(R.drawable.fill_heart)
        } else {
            likedBtn.setImageResource(R.drawable.empty_heart)
        }
    }

    /**
     * Add the product to the user's liked products via API.
     */
    private fun addToLikedProducts(productId: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to like a product", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", productId)
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.15/backend/add_to_liked_products.php")
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
                            isLiked = true
                            updateLikeButton()
                            Toast.makeText(this@ProductDetailsActivity, "✅ Added to liked products!", Toast.LENGTH_SHORT).show()
                            // Navigate to LikedProductsActivity
                            val intent = Intent(this@ProductDetailsActivity, LikedProductsActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Remove the product from the user's liked products via API.
     */
    private fun removeFromLikedProducts(productId: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to unlike a product", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", productId)
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.15/backend/remove_from_liked_products.php")
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
                            isLiked = false
                            updateLikeButton()
                            Toast.makeText(this@ProductDetailsActivity, "✅ Removed from liked products!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Fetch the user's current rating for the product.
     */
    private fun fetchUserRating(productId: Int, mobileUserId: Int) {
        val url = "http://192.168.1.15/backend/fetch_user_rating.php?mobile_user_id=$mobileUserId&product_id=$productId"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to fetch user rating: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return
                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val userRating = json.optInt("rating", 0)
                    runOnUiThread {
                        if (userRating > 0) {
                            ratingBar.rating = userRating.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ProductDetailsActivity, "❌ Error parsing user rating: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Fetch rating statistics for the product.
     */
    private fun fetchRatingStats(productId: Int) {
        val url = "http://192.168.1.15/backend/fetch_product_rating_stats.php?product_id=$productId"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to fetch rating stats: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return
                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val totalRatings = json.optInt("total_ratings", 0)
                    val averageRating = json.optDouble("average_rating", 0.0)
                    val ratingDistribution = json.optJSONObject("rating_distribution") ?: JSONObject()

                    runOnUiThread {
                        // Update average rating
                        averageRatingBar.rating = averageRating.toFloat()
                        val df = DecimalFormat("#.#")
                        averageRatingText.text = "${df.format(averageRating)} out of 5"

                        // Update total ratings
                        totalRatingsText.text = "$totalRatings customer ratings"

                        // Update rating distribution
                        percent5Star.text = "${ratingDistribution.optInt("5", 0)}%"
                        percent4Star.text = "${ratingDistribution.optInt("4", 0)}%"
                        percent3Star.text = "${ratingDistribution.optInt("3", 0)}%"
                        percent2Star.text = "${ratingDistribution.optInt("2", 0)}%"
                        percent1Star.text = "${ratingDistribution.optInt("1", 0)}%"

                        progress5Star.progress = ratingDistribution.optInt("5", 0)
                        progress4Star.progress = ratingDistribution.optInt("4", 0)
                        progress3Star.progress = ratingDistribution.optInt("3", 0)
                        progress2Star.progress = ratingDistribution.optInt("2", 0)
                        progress1Star.progress = ratingDistribution.optInt("1", 0)

                        // Optional: Hide rating distribution if there are no ratings
                        if (totalRatings == 0) {
                            findViewById<LinearLayout>(R.id.rating_distribution_container).visibility = View.GONE
                        } else {
                            findViewById<LinearLayout>(R.id.rating_distribution_container).visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@ProductDetailsActivity, "❌ Error parsing rating stats: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
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
     * Display a custom toast for rating submission or update.
     */
    private fun showRatingToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
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
     * Submits or updates a rating for the product by sending a POST request.
     */
    private fun submitReview(productId: Int, rating: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to submit a rating", Toast.LENGTH_SHORT).show()
            ratingBar.rating = 0f // Reset the rating if user is not logged in
            return
        }

        if (rating == 0) {
            Toast.makeText(this, "❌ Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", productId)
            put("rating", rating)
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.15/backend/add_product_review.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                    ratingBar.rating = 0f // Reset on failure
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            showRatingToast(jsonResponse.optString("message"))
                            fetchRatingStats(productId) // Refresh rating stats after submission
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                            ratingBar.rating = 0f // Reset on error
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
            put("price", productPrice.toString())
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