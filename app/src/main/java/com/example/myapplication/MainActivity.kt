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
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var productListView: ListView
    private lateinit var serviceListView: ListView
    private lateinit var bottomTabLayout: TabLayout
    private val client = OkHttpClient()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewCartButton: Button = findViewById(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        productListView = findViewById(R.id.productListView)
        serviceListView = findViewById(R.id.serviceListView)
        bottomTabLayout = findViewById(R.id.bottomTabLayout)

        setupCategoryButtons() // Ensure all UI elements are set up
        setupBottomTabs()       // before calling any method that depends on them

        // Now it's safe to call
        checkUserAndResetIfNeeded()
    }


    override fun onResume() {
        super.onResume()
        checkUserAndResetIfNeeded()
        fetchProducts()
        fetchServices()
    }

    private fun checkUserAndResetIfNeeded() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId != currentUserId) {
            currentUserId = userId
            resetServices()
        }
    }

    private fun resetServices() {
        (serviceListView.adapter as? ServiceAdapter)?.clear()
        fetchServices()
        fetchProducts()
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

    private fun setupBottomTabs() {
        bottomTabLayout.removeAllTabs() // Reset to avoid duplicate tabs

        bottomTabLayout.addTab(bottomTabLayout.newTab().setText("Products"))
        bottomTabLayout.addTab(bottomTabLayout.newTab().setText("Grooming"))
        bottomTabLayout.addTab(bottomTabLayout.newTab().setText("Veterinary"))

        bottomTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Products Tab
                        productListView.visibility = View.VISIBLE
                        serviceListView.visibility = View.GONE
                    }
                    1 -> { // Grooming Tab (Launch Grooming Activity)
                        startActivity(Intent(this@MainActivity, GroomingActivity::class.java))
                    }
                    2 -> { // Veterinary Tab (Launch Veterinary Activity)
                        startActivity(Intent(this@MainActivity, VeterinaryActivity::class.java))
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }



    private fun fetchServicesByType(type: String) {
        val url = "http://192.168.1.65/backend/fetch_services.php?type=$type"
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
                        Toast.makeText(this@MainActivity, "❌ No $type services found.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val servicesArray = json.getJSONArray("services")
                    val serviceList = mutableListOf<Service>()

                    for (i in 0 until servicesArray.length()) {
                        val serviceJson = servicesArray.getJSONObject(i)
                        serviceList.add(
                            Service(
                                id = serviceJson.getInt("id"),
                                serviceName = serviceJson.getString("service_name"),
                                description = serviceJson.getString("description"),
                                price = serviceJson.getDouble("price"),
                                status = serviceJson.getString("status")
                            )
                        )
                    }

                    runOnUiThread {
                        serviceListView.adapter = ServiceAdapter(this@MainActivity, serviceList) { service, selectedDate ->
                            availService(service, selectedDate)
                        }
                    }
                } else {
                    Log.e("Service Fetch", "❌ JSON Success = false")
                }
            }
        })
    }




    private fun fetchProducts() {
        val request = Request.Builder().url("http://192.168.1.65/backend/fetch_product.php").build()
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
                    val productList = mutableListOf<Product>()

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        productList.add(
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
                        if (productListView.adapter == null) {
                            productListView.adapter = ProductAdapter(this@MainActivity, productList)
                        } else {
                            (productListView.adapter as? ProductAdapter)?.updateProducts(productList)
                        }
                    }
                } else {
                    Log.e("Product Fetch", "❌ JSON Success = false")
                }
            }
        })
    }

    private fun fetchServices() {
        val request = Request.Builder().url("http://192.168.1.65/backend/fetch_services.php").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("Service Fetch", "📦 API Response: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ No services received.", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val servicesArray = json.getJSONArray("services")
                    val serviceList = mutableListOf<Service>()

                    for (i in 0 until servicesArray.length()) {
                        val serviceJson = servicesArray.getJSONObject(i)
                        serviceList.add(
                            Service(
                                id = serviceJson.getInt("id"),
                                serviceName = serviceJson.getString("service_name"),
                                description = serviceJson.getString("description"),
                                price = serviceJson.getDouble("price"),
                                status = serviceJson.getString("status")
                            )
                        )
                    }

                    runOnUiThread {
                        serviceListView.adapter = ServiceAdapter(this@MainActivity, serviceList) { service, selectedDate ->
                            availService(service, selectedDate)
                        }
                    }
                } else {
                    Log.e("Service Fetch", "❌ JSON Success = false")
                }
            }
        })
    }

    private fun fetchProductsByCategory(category: String) {
        val url = "http://192.168.1.65/backend/fetch_product.php?category=$category"
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
                    val productList = mutableListOf<Product>()

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        productList.add(
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
                        if (productListView.adapter == null) {
                            productListView.adapter = ProductAdapter(this@MainActivity, productList)
                        } else {
                            (productListView.adapter as ProductAdapter).updateProducts(productList)
                        }
                    }
                } else {
                    Log.e("FetchProducts", "❌ JSON success = false")
                }
            }
        })
    }

    private fun availService(service: Service, selectedDate: String) {
        Toast.makeText(this, "Availing ${service.serviceName} on $selectedDate", Toast.LENGTH_SHORT).show()
        // Add network request logic for availing service
    }
}
