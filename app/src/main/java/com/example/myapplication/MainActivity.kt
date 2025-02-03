package com.example.myapplication

import Product
import ProductAdapter
import Service
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchProducts() {
        val request = Request.Builder()
            .url("http://192.168.1.65/backend/fetch_product.php") // ✅ Fetch all products with images
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to fetch products", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val json = JSONArray(response.body!!.string())
                    val productList = mutableListOf<Product>()

                    for (i in 0 until json.length()) {
                        val productJson = json.getJSONObject(i)
                        productList.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = productJson.getString("image") // ✅ Ensure image URL is loaded
                            )
                        )
                    }

                    runOnUiThread {
                        val adapter = productListView.adapter as? ProductAdapter
                        if (adapter == null) {
                            productListView.adapter = ProductAdapter(this@MainActivity, productList)
                        } else {
                            adapter.updateProducts(productList) // ✅ Update product list
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }





    private fun fetchServices() {
        val userId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getInt("user_id", -1)
        val request = Request.Builder()
            .url("http://192.168.1.65/backend/fetch_services.php?user_id=$userId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    if (!response.isSuccessful || responseBody == null) {
                        throw IOException("Unexpected response ${response.code}")
                    }

                    val jsonArray = JSONArray(responseBody)
                    val services = mutableListOf<Service>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        services.add(
                            Service(
                                id = jsonObject.getInt("id"),
                                serviceName = jsonObject.getString("service_name"),
                                description = jsonObject.getString("description"),
                                status = jsonObject.optString("status", ""),
                                userId = jsonObject.optInt("user_id", -1).takeIf { it != -1 }
                            )
                        )
                    }

                    runOnUiThread {
                        val adapter = serviceListView.adapter as? ServiceAdapter
                        if (adapter == null) {
                            serviceListView.adapter = ServiceAdapter(this@MainActivity, services.toMutableList()) { service, selectedDate ->
                                availService(service, selectedDate)
                            }
                        } else {
                            adapter.updateServices(services)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error processing data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun availService(service: Service, selectedDate: String) {
        val userId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Please login to avail services", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("service_id", service.id)
            put("user_id", userId)
            put("status", "pending")
            put("selected_date", selectedDate)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/request_service.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to request service: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Service request submitted successfully", Toast.LENGTH_SHORT).show()
                    fetchServices()
                }
            }
        })
    }
}
