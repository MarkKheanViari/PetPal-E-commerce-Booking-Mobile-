package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var backBtn: ImageView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView
    private val client = OkHttpClient()
    private val cartItems = mutableListOf<HashMap<String, String>>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure the layout file name matches your resource (e.g. activity_cart.xml)
        setContentView(R.layout.activity_cart)

        // Find views from the layout
        recyclerView = findViewById(R.id.cartRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        //backBtn = findViewById(R.id.backBtn)
        checkoutButton = findViewById(R.id.checkoutButton)
        totalPriceTextView = findViewById(R.id.totalPriceTextView)

        // Setup RecyclerView and adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        cartAdapter = CartAdapter(this, cartItems, this)
        recyclerView.adapter = cartAdapter

        // Custom back button functionality
        backBtn.setOnClickListener {
            finish()
        }

        // Checkout button functionality: pass all cart items to CheckoutActivity
        checkoutButton.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("cartItems", ArrayList(cartItems))
                startActivity(intent)
            } else {
                Toast.makeText(this, "❌ Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        fetchCartItems()
    }

    override fun updateCartQuantity(cartId: Int, newQuantity: Int) {
        val url = "http://192.168.168.55/backend/update_cart.php"
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
        val url = "http://192.168.168.55/backend/remove_from_cart.php"
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

    // When a product is clicked, open the ProductDetailsActivity with the product's details.
    override fun onProductClick(cartItem: HashMap<String, String>) {
        val intent = Intent(this, ProductDetailsActivity::class.java)
        // Assuming the cartItem contains these keys
        val productId = cartItem["product_id"]?.toIntOrNull() ?: -1
        intent.putExtra("productId", productId)
        intent.putExtra("productName", cartItem["name"])
        intent.putExtra("productImage", cartItem["image"])
        // If your API doesn't send a description, you can use a default value
        intent.putExtra("productDescription", cartItem["description"] ?: "No description available.")
        val productPrice = cartItem["price"]?.toDoubleOrNull() ?: 0.0
        intent.putExtra("productPrice", productPrice)
        startActivity(intent)
    }

    private fun fetchCartItems() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to view cart", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.168.55/backend/fetch_cart.php?mobile_user_id=$mobileUserId"
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
                            "image" to item.getString("image"),
                            "description" to item.optString("description", "No description available")
                        )
                        cartItems.add(cartItem)
                    }

                    runOnUiThread {
                        cartAdapter.notifyDataSetChanged()
                        emptyText.visibility = if (cartItems.isEmpty()) TextView.VISIBLE else TextView.GONE
                        totalPriceTextView.text = "₱ %.2f".format(totalPrice)
                    }
                }
            }
        })
    }
}
