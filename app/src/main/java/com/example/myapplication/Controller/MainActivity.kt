package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // RecyclerView and Adapter
    private lateinit var productsRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var productAdapter: ProductAdapter

    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView

    // DrawerLayout
    private lateinit var drawerLayout: DrawerLayout

    // OkHttp client
    private val client = OkHttpClient()

    // Shared Preferences and current user
    private var currentUserId: Int = -1

    // Lists holding all products and those currently displayed
    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DEBUG:MainActivity", "onCreate called")
        setContentView(R.layout.activity_main)

        checkUserAndResetIfNeeded()

        // Setup DrawerLayout and menu icon
        drawerLayout = findViewById(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup cart button
        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Initialize RecyclerView
        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Create adapter with displayedProducts list
        productAdapter = ProductAdapter(this, displayedProducts)
        productsRecyclerView.adapter = productAdapter

        setupCategoryButtons()
        setupBottomNavigation()

        // Fetch products from the server
        fetchProducts()
    }

    private fun checkUserAndResetIfNeeded() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            currentUserId = userId
        }
    }

    private fun setupCategoryButtons() {
        val allButton: Button = findViewById(R.id.allButton)
        val catButton: Button = findViewById(R.id.catButton)
        val dogButton: Button = findViewById(R.id.dogButton)

        allButton.setOnClickListener {
            fetchProductsByCategory("all")
            updateButtonStyles(allButton, catButton, dogButton)
        }
        catButton.setOnClickListener {
            fetchProductsByCategory("cat")
            updateButtonStyles(catButton, allButton, dogButton)
        }
        dogButton.setOnClickListener {
            fetchProductsByCategory("dog")
            updateButtonStyles(dogButton, allButton, catButton)
        }
    }

    private fun updateButtonStyles(selectedButton: Button, vararg otherButtons: Button) {
        selectedButton.setBackgroundTintList(
            ContextCompat.getColorStateList(this, R.color.orange)
        )
        selectedButton.setTextColor(Color.WHITE)
        for (button in otherButtons) {
            button.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.light_smth)
            )
            button.setTextColor(Color.BLACK)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products -> {
                    productsRecyclerView.visibility = View.VISIBLE
                    true
                }
                R.id.menu_service -> {

                    startActivity(Intent(this, ServiceAvailActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Fetch all products from the server.
     */
    private fun fetchProducts() {
        val request = Request.Builder()
            .url("http://192.168.1.12/backend/fetch_product.php")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Empty response from server", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        val productsArray = json.optJSONArray("products") ?: JSONArray()
                        val fetchedProducts = mutableListOf<Product>()
                        val baseImageUrl = "http://192.168.1.12/backend/images/"
                        for (i in 0 until productsArray.length()) {
                            val productJson = productsArray.getJSONObject(i)
                            val rawImage = productJson.optString("image", "")
                            val fullImageUrl = if (rawImage.startsWith("http")) rawImage else baseImageUrl + rawImage

                            fetchedProducts.add(
                                Product(
                                    id = productJson.getInt("id"),
                                    name = productJson.getString("name"),
                                    price = productJson.getString("price"),
                                    description = productJson.getString("description"),
                                    quantity = productJson.getInt("quantity"),
                                    imageUrl = fullImageUrl
                                )
                            )
                        }
                        runOnUiThread {
                            Log.d("DEBUG:MainActivity", "Fetched ${fetchedProducts.size} products")
                            allProducts.clear()
                            allProducts.addAll(fetchedProducts)
                            displayedProducts.clear()
                            displayedProducts.addAll(fetchedProducts)
                            // Pass a new copy of the list to the adapter
                            productAdapter.updateProducts(ArrayList(displayedProducts))
                        }
                    } else {
                        Log.e("DEBUG:MainActivity", "Server returned success=false")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Fetch products by category from the server.
     */
    private fun fetchProductsByCategory(category: String) {
        if (category == "all") {
            displayedProducts.clear()
            displayedProducts.addAll(allProducts)
            productAdapter.updateProducts(ArrayList(displayedProducts))
            return
        }

        val url = "http://192.168.1.12/backend/fetch_product.php?category=$category"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No products found for $category", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val jsonResponse = JSONObject(responseData)
                    if (!jsonResponse.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch products for $category", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                    val productsArray = jsonResponse.optJSONArray("products") ?: JSONArray()
                    val baseImageUrl = "http://192.168.1.12/backend/images/"
                    val categoryProducts = mutableListOf<Product>()
                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        val rawImage = productJson.optString("image", "")
                        val fullImageUrl = if (rawImage.startsWith("http")) rawImage else baseImageUrl + rawImage

                        categoryProducts.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = fullImageUrl
                            )
                        )
                    }
                    runOnUiThread {
                        displayedProducts.clear()
                        displayedProducts.addAll(categoryProducts)
                        productAdapter.updateProducts(ArrayList(displayedProducts))
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
