package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LikedProductsActivity : AppCompatActivity() {

    private lateinit var likedProductsRecyclerView: RecyclerView
    private lateinit var likedProductAdapter: ProductAdapter
    private val likedProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_products)

        setupToolbar()
        setupRecyclerView()
        fetchLikedProducts()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        likedProductsRecyclerView = findViewById(R.id.likedProductsRecyclerView)

        // Use a GridLayoutManager with 2 columns
        likedProductsRecyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initialize the adapter with both wishlist and report listeners
        likedProductAdapter = ProductAdapter(
            this,
            likedProducts,
            { product ->
                // Handle adding to wishlist (optional in this context)
                Toast.makeText(this, "${product.name} is already in your wishlist!", Toast.LENGTH_SHORT).show()
            },
            { product ->
                // Handle reporting a product
                reportProduct(product)
            }
        )

        likedProductsRecyclerView.adapter = likedProductAdapter
    }

    /**
     * Fetch the user's liked products from the backend.
     */
    private fun fetchLikedProducts() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to view liked products", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.1.12/backend/fetch_liked_products.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LikedProductsActivity, "❌ Failed to fetch liked products: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return
                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@LikedProductsActivity, "❌ Error: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val likedProductsArray = json.optJSONArray("liked_products") ?: return
                    likedProducts.clear()
                    for (i in 0 until likedProductsArray.length()) {
                        val productJson = likedProductsArray.getJSONObject(i)
                        val imageUrl = productJson.optString("image", "")
                        // Log the image URL to debug
                        android.util.Log.d("LikedProductsActivity", "Product ${productJson.optString("name")}: Image URL = $imageUrl")
                        val product = Product(
                            id = productJson.optInt("id", -1),
                            name = productJson.optString("name", ""),
                            price = productJson.optString("price", "0.0"),
                            description = productJson.optString("description", ""),
                            quantity = productJson.optInt("quantity", 1),
                            imageUrl = imageUrl
                        )
                        likedProducts.add(product)
                    }

                    runOnUiThread {
                        likedProductAdapter.updateProducts(ArrayList(likedProducts))
                        if (likedProducts.isEmpty()) {
                            Toast.makeText(this@LikedProductsActivity, "No liked products found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LikedProductsActivity, "❌ Error parsing liked products: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // New method to handle product reporting
    private fun reportProduct(product: Product) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

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
                .url("http://192.168.1.12/backend/report_product.php")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@LikedProductsActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    runOnUiThread {
                        if (responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.optBoolean("success", false)) {
                                Toast.makeText(this@LikedProductsActivity, "✅ Product reported successfully!", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(this@LikedProductsActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        }

        // Show the dialog
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}