package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CartActivity : AppCompatActivity(), CartActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView
    private val client = OkHttpClient()
    private val cartItems = mutableListOf<HashMap<String, String>>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.cartRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        checkoutButton = findViewById(R.id.checkoutButton)
        totalPriceTextView = findViewById(R.id.totalPriceTextView) // Total price text view

        // Initialize RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(this, cartItems, this)
        recyclerView.adapter = cartAdapter

        fetchCartItems()

        checkoutButton.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("cartItems", ArrayList(cartItems)) // Pass cart items to CheckoutActivity
                startActivity(intent)
            } else {
                Toast.makeText(this, "❌ Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun updateCartQuantity(cartId: Int, newQuantity: Int) {
        val url = "http://192.168.150.55/backend/update_cart.php"
        val json = JSONObject().apply {
            put("cart_id", cartId)
            put("quantity", newQuantity)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CartActivity, "❌ Failed to update quantity", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@CartActivity, "✅ Quantity updated", Toast.LENGTH_SHORT).show()
                    fetchCartItems() // Refresh cart
                }
            }
        })
    }

    override fun removeItemFromCart(cartId: Int) {
        val url = "http://192.168.150.55/backend/remove_from_cart.php"
        val json = JSONObject().apply { put("cart_id", cartId) }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CartActivity, "❌ Failed to remove item", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    Toast.makeText(this@CartActivity, "✅ Item removed", Toast.LENGTH_SHORT).show()
                    fetchCartItems() // Refresh cart
                }
            }
        })
    }

    private fun fetchCartItems() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to view cart", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.150.55/backend/fetch_cart.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CartActivity, "❌ Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val cartArray = jsonResponse.optJSONArray("cart") ?: JSONArray()

                    cartItems.clear()
                    var totalPrice = 0.0

                    for (i in 0 until cartArray.length()) {
                        val item = cartArray.getJSONObject(i)
                        val price = item.getDouble("price")
                        val quantity = item.getInt("quantity")
                        totalPrice += price * quantity

                        val cartItem = hashMapOf(
                            "cart_id" to item.getString("cart_id"),
                            "product_id" to item.getString("product_id"),
                            "name" to item.getString("name"),
                            "price" to price.toString(),
                            "quantity" to quantity.toString(),
                            "image" to item.getString("image")
                        )
                        cartItems.add(cartItem)
                    }

                    runOnUiThread {
                        cartAdapter.notifyDataSetChanged()
                        emptyText.visibility = if (cartItems.isEmpty()) TextView.VISIBLE else TextView.GONE
                        totalPriceTextView.text = "₱ %.2f".format(totalPrice) // Display total price
                    }
                }
            }
        })
    }
}
