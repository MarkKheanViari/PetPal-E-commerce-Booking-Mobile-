package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter
    private val orderHistoryList = mutableListOf<OrderHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ordered_products)

        recyclerView = findViewById(R.id.orderHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = OrderAdapter(orderHistoryList)
        recyclerView.adapter = adapter

        loadOrderHistory()
    }

    private fun loadOrderHistory() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val orderHistoryJson = sharedPrefs.getString("order_history", "[]")
        Log.d("OrderHistoryActivity", "order_history => $orderHistoryJson")

        val jsonArray = JSONArray(orderHistoryJson)
        orderHistoryList.clear()

        for (i in 0 until jsonArray.length()) {
            val itemObj = jsonArray.getJSONObject(i)
            val name = itemObj.optString("productName", "Unknown")
            val manufacturer = itemObj.optString("manufacturer", "Unknown")
            val price = itemObj.optDouble("price", 0.0)
            val quantity = itemObj.optInt("quantity", 1)

            orderHistoryList.add(OrderHistoryItem(name, manufacturer, price, quantity))
        }
        adapter.notifyDataSetChanged()
    }
}
