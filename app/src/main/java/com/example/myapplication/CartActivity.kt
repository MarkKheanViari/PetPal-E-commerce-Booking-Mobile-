package com.example.myapplication


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.example.myapplication.CartAdapter
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CartActivity : AppCompatActivity() {
    private lateinit var cartListView: ListView
    private lateinit var emptyText: TextView
    private lateinit var checkoutButton: Button
    private val client = OkHttpClient()
    private val cartItems = mutableListOf<HashMap<String, String>>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        cartListView = findViewById(R.id.cartListView)
        emptyText = findViewById(R.id.emptyText)
        checkoutButton = findViewById(R.id.checkoutButton)

        fetchCartItems()
    }

    private fun fetchCartItems() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to view cart", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.1.65/backend/fetch_cart.php?mobile_user_id=$mobileUserId"
        Log.d("CartActivity", "🔄 Fetching cart from: $url") // ✅ Debugging log

        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("CartActivity", "❌ Network Error: ${e.message}")
                    Toast.makeText(this@CartActivity, "❌ Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CartActivity", "📦 API Response: $responseBody") // ✅ Debugging log

                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@CartActivity, "❌ No cart items found.", Toast.LENGTH_SHORT).show()
                        emptyText.visibility = TextView.VISIBLE
                    }
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@CartActivity, "❌ Error loading cart.", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val cartArray = json.optJSONArray("cart") ?: JSONArray()
                    cartItems.clear()

                    for (i in 0 until cartArray.length()) {
                        val item = cartArray.getJSONObject(i)
                        val cartItem = hashMapOf(
                            "cart_id" to item.getString("cart_id"),
                            "product_id" to item.getString("product_id"),
                            "name" to item.getString("name"),
                            "price" to item.getString("price"),
                            "quantity" to item.getString("quantity"),
                            "image" to item.getString("image")
                        )
                        cartItems.add(cartItem)
                    }

                    runOnUiThread {
                        cartAdapter = CartAdapter(this@CartActivity, cartItems)
                        cartListView.adapter = cartAdapter
                        emptyText.visibility = if (cartItems.isEmpty()) TextView.VISIBLE else TextView.GONE
                    }

                } catch (e: Exception) {
                    Log.e("CartActivity", "❌ JSON Parsing Error: ${e.message}")
                }
            }
        })
    }


    // ✅ Remove item from cart
    fun removeItemFromCart(cartId: Int) {
        val url = "http://192.168.1.65/backend/remove_from_cart.php"

        val json = JSONObject()
        json.put("cart_id", cartId)

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

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
}
