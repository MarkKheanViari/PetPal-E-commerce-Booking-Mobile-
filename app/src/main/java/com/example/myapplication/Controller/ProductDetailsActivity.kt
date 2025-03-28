package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
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
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        // Retrieve product data from the Intent
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName")
        val productImage = intent.getStringExtra("productImage")
        val productDescription = intent.getStringExtra("productDescription")
        val navigateToReport = intent.getBooleanExtra("navigateToReport", false)

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
        val reportButton = findViewById<ImageView>(R.id.reportButton) // Add report button reference
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

        // Cart button
        cartBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to", "cart")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Report button functionality
        reportButton.setOnClickListener {
            if (productId != -1) {
                val product = Product(
                    id = productId,
                    name = productName ?: "",
                    price = productPrice.toString(),
                    description = productDescription ?: "",
                    quantity = 0, // Quantity isn't needed for reporting
                    imageUrl = productImage ?: ""
                )
                reportProduct(product)
            } else {
                Toast.makeText(this, "❌ Invalid product ID", Toast.LENGTH_SHORT).show()
            }
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

        // If navigated here to report, show the report dialog
        if (navigateToReport && productId != -1) {
            val product = Product(
                id = productId,
                name = productName ?: "",
                price = productPrice.toString(),
                description = productDescription ?: "",
                quantity = 0, // Quantity isn't needed for reporting
                imageUrl = productImage ?: ""
            )
            reportProduct(product)
        }
    }

    private fun reportProduct(product: Product) {
        val isGuest = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getBoolean("isGuest", false)
        if (isGuest) {
            Toast.makeText(this, "Guest users cannot report products. Please log in.", Toast.LENGTH_SHORT).show()
            return
        }

        val mobileUserId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getInt("user_id", -1)
        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to report a product", Toast.LENGTH_SHORT).show()
            return
        }

        // Updated list with 5 report reasons
        val reportReasons = listOf(
            "Harmful or Toxic Ingredients – The product contains substances that can be dangerous to pets.",
            "Expired or Spoiled Product – Pet food, treats, or medications that are past their expiration date or appear contaminated.",
            "Counterfeit or Fake Product – The product is not from the claimed brand or is a replica of an original product.",
            "Inappropriate or Dangerous Design – Toys, leashes, or accessories that pose choking hazards or safety risks.",
            "Defective or Poor-Quality Material – Items that break easily, causing potential harm to pets."
        )

        // Create custom dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_product, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set up dialog title
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        dialogTitle.text = "Report Product: ${product.name}"

        // Set up RecyclerView for reasons
        val reasonsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.reasonsRecyclerView)
        reasonsRecyclerView.layoutManager = LinearLayoutManager(this)
        reasonsRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        var selectedReason: String? = null
        val reasonAdapter = ReportReasonAdapter(reportReasons) { reason ->
            selectedReason = reason  // Can be null if deselected
        }
        reasonsRecyclerView.adapter = reasonAdapter

        // Set up buttons
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        submitButton.setOnClickListener {
            if (selectedReason == null) {
                Toast.makeText(this, "Please select a reason for reporting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonObject = JSONObject().apply {
                put("mobile_user_id", mobileUserId)
                put("product_id", product.id)
                put("reason", selectedReason)
            }

            val requestBody = jsonObject.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("http://192.168.1.65/backend/report_product.php")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@ProductDetailsActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d("API_RESPONSE", "Response: $responseBody")
                    runOnUiThread {
                        if (responseBody != null) {
                            try {
                                val jsonResponse = JSONObject(responseBody)
                                if (jsonResponse.optBoolean("success", false)) {
                                    Toast.makeText(this@ProductDetailsActivity, "✅ Product reported successfully!", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: JSONException) {
                                Toast.makeText(this@ProductDetailsActivity, "❌ Invalid response format.", Toast.LENGTH_SHORT).show()
                                Log.e("JSON_PARSE", "Response is not valid JSON: $responseBody")
                            }
                        }
                    }
                }
            })
        }

        // Show the dialog
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun checkIfProductIsLiked(productId: Int, mobileUserId: Int) {
        val url = "http://192.168.1.65/backend/check_if_liked.php?mobile_user_id=$mobileUserId&product_id=$productId"
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

    private fun updateLikeButton() {
        if (isLiked) {
            likedBtn.setImageResource(R.drawable.fill_heart)
        } else {
            likedBtn.setImageResource(R.drawable.empty_heart)
        }
    }

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
            .url("http://192.168.1.65/backend/add_to_liked_products.php")
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
            .url("http://192.168.1.65/backend/remove_from_liked_products.php")
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

    private fun fetchUserRating(productId: Int, mobileUserId: Int) {
        val url = "http://192.168.1.65/backend/fetch_user_rating.php?mobile_user_id=$mobileUserId&product_id=$productId"
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

    private fun fetchRatingStats(productId: Int) {
        val url = "http://192.168.1.65/backend/fetch_product_rating_stats.php?product_id=$productId"
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
                        averageRatingBar.rating = averageRating.toFloat()
                        val df = DecimalFormat("#.#")
                        averageRatingText.text = "${df.format(averageRating)} out of 5"
                        totalRatingsText.text = "$totalRatings customer ratings"
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

    private fun showAddedToCartToast() {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.added_to_cart, null)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

    private fun showRatingToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

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
            .url("http://192.168.1.65/backend/add_to_cart.php")
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

    private fun submitReview(productId: Int, rating: Int) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to submit a rating", Toast.LENGTH_SHORT).show()
            ratingBar.rating = 0f
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
            .url("http://192.168.1.65/backend/add_product_review.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProductDetailsActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                    ratingBar.rating = 0f
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            showRatingToast(jsonResponse.optString("message"))
                            fetchRatingStats(productId)
                        } else {
                            Toast.makeText(this@ProductDetailsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                            ratingBar.rating = 0f
                        }
                    }
                }
            }
        })
    }

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
