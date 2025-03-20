package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class LikedProductsActivity : AppCompatActivity() {

    private lateinit var likedProductsRecyclerView: RecyclerView
    private lateinit var likedProductAdapter: ProductAdapter
    private val likedProductsList = mutableListOf<Product>()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_products)

        // Setup RecyclerView
        likedProductsRecyclerView = findViewById(R.id.likedProductsRecyclerView)
        likedProductsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        likedProductAdapter = ProductAdapter(this, likedProductsList) { product ->
            // Handle product click if needed
        }
        likedProductsRecyclerView.adapter = likedProductAdapter

        // Instead of using the userId parameter, we now fetch all liked products.
        fetchLikedProducts()
    }

    private fun fetchLikedProducts() {
        // Updated URL: no userId query parameter now.
        val url = "http://192.168.1.12/backend/get_liked_products.php"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LikedProductsActivity, "Error fetching liked products: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                Log.e("LikedProductsActivity", "Network error: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@LikedProductsActivity, "Error fetching liked products.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success")) {
                        // Expecting the backend returns "likedProducts" as an array.
                        val productsArray: JSONArray = json.optJSONArray("likedProducts") ?: JSONArray()
                        val fetchedProducts = mutableListOf<Product>()
                        for (i in 0 until productsArray.length()) {
                            val productJson = productsArray.getJSONObject(i)
                            fetchedProducts.add(
                                Product(
                                    id = productJson.getInt("id"),
                                    name = productJson.getString("name"),
                                    price = productJson.getString("price"),
                                    description = productJson.getString("description"),
                                    quantity = productJson.getInt("quantity"),
                                    imageUrl = productJson.getString("imageUrl")
                                )
                            )
                        }
                        runOnUiThread {
                            likedProductsList.clear()
                            likedProductsList.addAll(fetchedProducts)
                            likedProductAdapter.updateProducts(ArrayList(likedProductsList))
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LikedProductsActivity, "Failed to load liked products.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@LikedProductsActivity, "Parsing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("LikedProductsActivity", "Parsing error: ${e.localizedMessage}")
                }
            }
        })
    }
}
