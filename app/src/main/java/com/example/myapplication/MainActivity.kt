package com.example.myapplication

import com.example.myapplication.CartActivity
import Product
import ProductAdapter
import Service
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    // Using ListView for products and services.
    private lateinit var productListView: ListView
    private lateinit var serviceListView: ListView

    // Using BottomNavigationView for bottom navigation.
    private lateinit var bottomNavigation: BottomNavigationView

    private val client = OkHttpClient()
    private var currentUserId: Int = -1

    // Global lists for local filtering or searching
    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) Check if user is logged in (or finish)
        checkUserAndResetIfNeeded()

        // 2) Set up DrawerLayout and menu icon
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 3) Set up cart button
        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // 4) Find ListViews and bottom nav
        productListView = findViewById(R.id.productsRecyclerView)
        serviceListView = findViewById(R.id.serviceRecyclerView)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // 5) Set up item click listener for product details
        productListView.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = productListView.adapter.getItem(position) as Product
            val intent = Intent(this, ProductDetailsActivity::class.java).apply {
                putExtra("id", selectedProduct.id)
                putExtra("name", selectedProduct.name)
                putExtra("price", selectedProduct.price)
                putExtra("description", selectedProduct.description)
                putExtra("quantity", selectedProduct.quantity)
                putExtra("imageUrl", selectedProduct.imageUrl)
            }
            startActivity(intent)
        }

        // 6) Setup category buttons & bottom navigation
        setupCategoryButtons()
        setupBottomNavigation()

        // 7) Fetch products only once here
        fetchProducts()
    }

    override fun onResume() {
        super.onResume()
        // DO NOT fetch again here - otherwise we risk overwriting our local list
        // with an empty or partial list if the server call fails or returns nothing.

        // If you *do* want to re-fetch from server each time,
        // you can do so conditionally, e.g. if(allProducts.isEmpty()) fetchProducts()
    }

    private fun checkUserAndResetIfNeeded() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        Log.d("MainActivity", "User ID Retrieved: $userId")
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            currentUserId = userId
        }
    }

    private fun resetServices() {
        (serviceListView.adapter as? ServiceAdapter)?.clear()
        // fetchProducts() // only if you want to forcibly refresh
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
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange))
        selectedButton.setTextColor(Color.WHITE)
        for (button in otherButtons) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_gray))
            button.setTextColor(Color.BLACK)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products -> {
                    productListView.visibility = View.VISIBLE
                    serviceListView.visibility = View.GONE
                    true
                }
                R.id.menu_service -> {
                    startActivity(Intent(this, GroomingActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchProducts() {
        val request = Request.Builder().url("http://192.168.1.13/backend/fetch_product.php").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Product Fetch", "📦 Response: $responseBody")
                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ No products found.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val productsArray = json.optJSONArray("products") ?: JSONArray()
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
                                imageUrl = productJson.getString("image")
                            )
                        )
                    }
                    runOnUiThread {
                        // Clear the old lists and update with new data
                        allProducts.clear()
                        allProducts.addAll(fetchedProducts)

                        displayedProducts.clear()
                        displayedProducts.addAll(fetchedProducts)

                        if (productListView.adapter == null) {
                            productListView.adapter = ProductAdapter(this@MainActivity, displayedProducts)
                        } else {
                            (productListView.adapter as? ProductAdapter)?.updateProducts(displayedProducts)
                        }
                    }
                } else {
                    Log.e("Product Fetch", "❌ JSON Success = false")
                }
            }
        })
    }

    private fun fetchProductsByCategory(category: String) {
        val url = "http://192.168.1.13/backend/fetch_product.php?category=$category"
        Log.d("FetchProducts", "Fetching products for category: $category")
        val request = Request.Builder().url(url).build()
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
                        Toast.makeText(this@MainActivity, "❌ No products found for $category.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val productsArray = json.optJSONArray("products") ?: JSONArray()
                    val filteredProducts = mutableListOf<Product>()
                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        filteredProducts.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = productJson.getString("image")
                            )
                        )
                    }
                    runOnUiThread {
                        displayedProducts.clear()
                        displayedProducts.addAll(filteredProducts)
                        (productListView.adapter as? ProductAdapter)?.updateProducts(displayedProducts)
                    }
                } else {
                    Log.e("FetchProducts", "❌ JSON success = false")
                }
            }
        })
    }

    // Optional local search if you want it
    private fun performLocalSearch(query: String) {
        val filtered = allProducts.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
        }
        displayedProducts.clear()
        displayedProducts.addAll(filtered)
        (productListView.adapter as? ProductAdapter)?.updateProducts(displayedProducts)
    }

    private fun availService(service: Service, selectedDate: String) {
        Toast.makeText(this, "Availing ${service.serviceName} on $selectedDate", Toast.LENGTH_SHORT).show()
        // Add network request logic for availing service if needed.
    }
}
