package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import android.app.AlertDialog
import android.view.LayoutInflater

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var requestQueue: RequestQueue
    private var allOrders: MutableList<Order> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.order_details_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(allOrders)
        recyclerView.adapter = orderAdapter

        // Initialize TabLayout
        tabLayout = findViewById(R.id.order_status_tabs)

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this)

        // Create Notification Channel (for Android O and above)
        createNotificationChannel()

        // Fetch orders from backend
        fetchOrders()

        // Listen for tab selection changes
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { filterOrders(it.position) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "order_updates_channel"
            val channelName = "Order Updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendOrderStatusNotification(order: Order) {
        val channelId = "order_updates_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notif) // Replace with your own icon resource
            .setContentTitle("Order Update")
            .setContentText("Your order #${order.id} is now ${order.status}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Notify using order.id as a unique identifier
        notificationManager.notify(order.id, notificationBuilder.build())

        // Optionally, save the notification message for history
        saveNotification("Order #${order.id} is now ${order.status}")
    }

    private fun saveNotification(message: String) {
        val sp = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val notificationsJson = sp.getString("notifications", "[]")
        val jsonArray = JSONArray(notificationsJson)
        jsonArray.put(message)
        sp.edit().putString("notifications", jsonArray.toString()).apply()
    }

    private fun fetchOrders() {
        val userId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://192.168.1.15/backend/fetch_orders_mobile.php?mobile_user_id=$userId"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("OrderDetailsActivity", "✅ Response: $response")
                if (response.getBoolean("success")) {
                    parseOrders(response.getJSONArray("orders"))
                } else {
                    Toast.makeText(this, "No orders found!", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e("OrderDetailsActivity", "❌ Error: ${error.message}")
                Toast.makeText(this, "Error fetching orders!", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(request)
    }

    private fun parseOrders(jsonArray: JSONArray) {
        allOrders.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            // Extract items if they exist
            val itemsList = mutableListOf<OrderItem>()
            if (obj.has("items")) {
                val itemsArray = obj.getJSONArray("items")
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val item = OrderItem(
                        productId = itemObj.getInt("product_id"),
                        name = itemObj.getString("product_name"),
                        imageUrl = itemObj.getString("image"),
                        description = itemObj.getString("description"),
                        quantity = itemObj.getInt("quantity"),
                        price = itemObj.getString("price")
                    )
                    itemsList.add(item)
                }
            }

            val order = Order(
                id = obj.getInt("id"),
                totalPrice = obj.getString("total_price"),
                paymentMethod = obj.getString("payment_method"),
                status = obj.getString("status"),
                createdAt = obj.getString("created_at"),
                items = itemsList
            )
            allOrders.add(order)

            // Notify user if order status is "Shipped" or "Delivered"
            if (order.status == "Shipped" || order.status == "Delivered") {
                sendOrderStatusNotification(order)
            }
        }
        filterOrders(tabLayout.selectedTabPosition)
    }

    private fun filterOrders(selectedTab: Int) {
        val filteredOrders = when (selectedTab) {
            0 -> allOrders // "All"
            1 -> allOrders.filter { it.status == "Pending" }
            2 -> allOrders.filter { it.status == "To Ship" }
            3 -> allOrders.filter { it.status == "Shipped" }
            4 -> allOrders.filter { it.status == "Delivered" }
            5 -> allOrders.filter { it.status == "Cancelled" }
            else -> allOrders
        }
        orderAdapter.updateOrders(filteredOrders)
    }
}
