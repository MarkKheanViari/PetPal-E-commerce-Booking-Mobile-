package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import okhttp3.*
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

        // Initialize the adapter
        likedProductAdapter = ProductAdapter(
            this,
            likedProducts
        ) { product ->
            // Handle product clicks to navigate to ProductDetailsActivity
            val intent = Intent(this, ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productPrice", product.price)
                putExtra("productDescription", product.description)
                putExtra("productImage", product.imageUrl)
            }
            startActivity(intent)
        }

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
}
