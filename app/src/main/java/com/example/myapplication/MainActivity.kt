package com.example.myapplication

import com.example.myapplication.CartActivity
import Product
import ProductAdapter
import Service
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var productListView: ListView
    private lateinit var serviceListView: ListView
    private lateinit var tabLayout: TabLayout
    private val client = OkHttpClient()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val serviceHistoryButton = findViewById<Button>(R.id.serviceHistoryButton)
        serviceHistoryButton.setOnClickListener {
            startActivity(Intent(this, ServiceHistoryActivity::class.java))
        }

        val viewCartButton: Button = findViewById(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        productListView = findViewById(R.id.productListView)
        serviceListView = findViewById(R.id.serviceListView)
        tabLayout = findViewById(R.id.tabLayout)

        setupTabs()
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

    private fun setupTabs() {
        if (tabLayout.tabCount == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("Products"))
            tabLayout.addTab(tabLayout.newTab().setText("Services"))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        productListView.visibility = View.VISIBLE
                        serviceListView.visibility = View.GONE
                    }
                    1 -> {
                        productListView.visibility = View.GONE
                        serviceListView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
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

                try {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        val servicesArray = jsonResponse.getJSONArray("services") // ✅ Get the "services" array
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
                } catch (e: Exception) {
                    Log.e("Service Fetch", "❌ JSON Parsing Error: ${e.message}")
                }
            }
        })
    }


    private fun sortProductsByPrice() {
        val adapter = productListView.adapter as? ProductAdapter ?: return
        val sortedList = adapter.getProducts().sortedBy { product -> product.price.toDouble() }
        adapter.updateProducts(sortedList)
    }

    private fun availService(service: Service, selectedDate: String) {
        Toast.makeText(this, "Availing ${service.serviceName} on $selectedDate", Toast.LENGTH_SHORT).show()

        // Add network request logic for availing service
    }

}
