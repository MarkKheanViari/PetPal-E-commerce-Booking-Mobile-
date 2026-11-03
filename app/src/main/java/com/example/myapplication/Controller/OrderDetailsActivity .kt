package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import android.view.LayoutInflater

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var requestQueue: RequestQueue
    private lateinit var emptyMessageTextView: TextView
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

        // Handle "View Details" clicks
        orderAdapter.onViewDetailsClicked = { order ->
            showOrderDetailsPopup(order)
        }

        // Handle order cancellation: update master list, reapply filter, and switch tab
        orderAdapter.onOrderCancelled = { cancelledOrder ->
            val index = allOrders.indexOfFirst { it.id == cancelledOrder.id }
            if (index != -1) {
                allOrders[index] = cancelledOrder
            }
            filterOrders(tabLayout.selectedTabPosition)
            tabLayout.getTabAt(5)?.select()
        }

        // Empty state TextView
        emptyMessageTextView = findViewById(R.id.emptyMessageTextView)

        // TabLayout
        tabLayout = findViewById(R.id.order_status_tabs)

        // Volley request queue
        requestQueue = Volley.newRequestQueue(this)

        // Fetch orders
        fetchOrders()

        // Handle tab selection changes
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { filterOrders(it.position) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showOrderDetailsPopup(order: Order) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_order_details, null)

        // Bind user info
        val userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        val addressTextView = view.findViewById<TextView>(R.id.addressTextView)
        val phoneNumberTextView = view.findViewById<TextView>(R.id.phoneNumberTextView)

        userNameTextView.text = "Name: ${order.userName}"
        addressTextView.text = "Address: ${order.address}"
        phoneNumberTextView.text = "Phone: ${order.phoneNumber}"

        // Set up the RecyclerView for items
        val itemsRecyclerView = view.findViewById<RecyclerView>(R.id.orderItemsRecyclerView)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = OrderItemsAdapter(order.items)

        builder.setView(view)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun fetchOrders() {
        val userId = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "http://192.168.80.63/backend/fetch_orders_mobile.php?mobile_user_id=$userId"
        Log.d("OrderDetailsActivity", "Fetching orders from: $url")

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d("OrderDetailsActivity", "Response: $response")
                if (response.getBoolean("success")) {
                    parseOrders(response.getJSONArray("orders"))
                } else {
                    showEmptyState()
                }
            },
            Response.ErrorListener { error ->
                Log.e("OrderDetailsActivity", "Error: ${error.message}")
                Toast.makeText(this, "Error fetching orders!", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(request)
    }

    private fun parseOrders(jsonArray: JSONArray) {
        // Use fallback values from SharedPreferences if backend values are missing
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val storedUserName = sharedPrefs.getString("username", "No Username Provided") ?: "No Username Provided"
        val storedAddress = sharedPrefs.getString("location", "No Address Provided") ?: "No Address Provided"
        val storedPhone = sharedPrefs.getString("contact_number", "No Phone Number Provided") ?: "No Phone Number Provided"

        allOrders.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            // Build the list of order items
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

            // Create the Order object
            val order = Order(
                id = obj.getInt("id"),
                totalPrice = obj.getString("total_price"),
                paymentMethod = obj.getString("payment_method"),
                status = obj.getString("status"),
                createdAt = obj.getString("created_at"),
                items = itemsList,
                userName = obj.optString("user_name", storedUserName),
                address = obj.optString("address", storedAddress),
                phoneNumber = obj.optString("phone_number", storedPhone)
            )
            allOrders.add(order)
        }
        // Filter orders based on the currently selected tab
        filterOrders(tabLayout.selectedTabPosition)
    }

    private fun filterOrders(selectedTab: Int) {
        val filteredOrders = when (selectedTab) {
            0 -> allOrders // All
            1 -> allOrders.filter { it.status.equals("Pending", ignoreCase = true) }
            2 -> allOrders.filter { it.status.equals("To Ship", ignoreCase = true) }
            3 -> allOrders.filter { it.status.equals("Shipped", ignoreCase = true) }
            4 -> allOrders.filter { it.status.equals("Delivered", ignoreCase = true) }
            5 -> allOrders.filter { it.status.equals("Cancelled", ignoreCase = true) }
            else -> allOrders
        }

        if (filteredOrders.isEmpty()) {
            emptyMessageTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyMessageTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        orderAdapter.updateOrders(filteredOrders)
    }

    private fun showEmptyState() {
        emptyMessageTextView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
}
