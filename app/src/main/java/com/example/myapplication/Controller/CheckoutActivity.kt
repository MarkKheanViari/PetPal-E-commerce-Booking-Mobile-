package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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

class CheckoutActivity : AppCompatActivity() {

    private val TAG = "CheckoutActivityDebug"
    private val SHIPPING_COST = 50.0
    private val FREE_SHIPPING_THRESHOLD = 500.0

    // UI elements
    private lateinit var recyclerView: RecyclerView
    private lateinit var totalTextView: TextView       // hidden view for logic
    private lateinit var subtotalText: TextView        // visible Subtotal text
    private lateinit var shippingCostText: TextView    // visible Shipping text
    private lateinit var orderTotalPriceSummary: TextView  // visible Total text
    private lateinit var userInfoText: TextView
    private lateinit var selectPaymentButton: Button
    private lateinit var paymentMethodText: TextView
    private lateinit var placeOrderButton: ImageView

    // Data from intent (cart items)
    private lateinit var cartItems: ArrayList<HashMap<String, String>>
    private lateinit var cartList: ArrayList<CartItem>
    private var cartTotal: Double = 0.0

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Find views
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        totalTextView = findViewById(R.id.totalTextView)
        subtotalText = findViewById(R.id.subtotalText)
        shippingCostText = findViewById(R.id.shippingCostText)
        orderTotalPriceSummary = findViewById(R.id.orderTotalPriceSummary)
        userInfoText = findViewById(R.id.userInfoText)
        selectPaymentButton = findViewById(R.id.selectPaymentButton)
        paymentMethodText = findViewById(R.id.paymentMethodText)
        placeOrderButton = findViewById(R.id.checkoutBtn)

        // Back button
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        // Retrieve cart items from intent (must be passed as serializable)
        cartItems = intent.getSerializableExtra("cartItems") as? ArrayList<HashMap<String, String>> ?: arrayListOf()
        cartList = arrayListOf()

        Log.d(TAG, "Received cartItems size: ${cartItems.size}")
        for ((index, item) in cartItems.withIndex()) {
            Log.d(TAG, "Item #$index => $item")
        }

        // Convert raw cart items into CartItem objects
        for (itemMap in cartItems) {
            val productId = itemMap["product_id"]?.toIntOrNull() ?: 0
            val productName = itemMap["product_name"] ?: "Unknown"
            val quantity = itemMap["quantity"]?.toIntOrNull() ?: 0
            val price = itemMap["price"]?.toDoubleOrNull() ?: 0.0

            cartList.add(CartItem(productId, productName, quantity, price))
        }

        // Log cart items for debugging
        for ((index, cartItem) in cartList.withIndex()) {
            Log.d(TAG, "CartItem #$index => ID=${cartItem.productId}, Name=${cartItem.productName}, Qty=${cartItem.quantity}, Price=${cartItem.price}")
        }

        // Calculate totals and update UI
        calculateTotal()

        // Setup RecyclerView for cart items
        recyclerView.layoutManager = LinearLayoutManager(this)
        val checkoutAdapter = CheckoutAdapter(this, cartItems)
        recyclerView.adapter = checkoutAdapter

        // Payment method selection dialog
        selectPaymentButton.setOnClickListener {
            val paymentOptions = arrayOf("COD (Cash on Delivery)", "GCASH")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Select Payment Method")
            builder.setItems(paymentOptions) { _, which ->
                paymentMethodText.text = paymentOptions[which]
            }
            builder.show()
        }

        // Place Order button listener
        placeOrderButton.setOnClickListener {
            Log.d(TAG, "Place Order Button Clicked!")
            submitOrder()
        }

        // Fetch user info
        fetchUserInfo()
    }

    private fun calculateTotal() {
        // Compute subtotal from cart items
        val subtotal = cartList.sumOf { it.price * it.quantity }
        val shippingCost = if (subtotal >= FREE_SHIPPING_THRESHOLD) 0.0 else SHIPPING_COST
        cartTotal = subtotal + shippingCost

        Log.d(TAG, "Subtotal: $subtotal, Shipping: $shippingCost, Total: $cartTotal")

        // Update UI fields
        subtotalText.text = "₱%.2f".format(subtotal)
        shippingCostText.text = if (shippingCost == 0.0) "Free" else "₱%.2f".format(shippingCost)
        orderTotalPriceSummary.text = "₱%.2f".format(cartTotal)
        totalTextView.text = "Total: ₱%.2f".format(cartTotal)
    }

    private fun fetchUserInfo() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)
        if (mobileUserId == -1) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val url = "http://192.168.1.12/backend/fetch_user_info.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "UserInfo response: $responseBody")
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        val username = jsonResponse.optString("username")
                        val contactNumber = jsonResponse.optString("contact_number")
                        val location = jsonResponse.optString("location")
                        runOnUiThread {
                            userInfoText.text = "$username ($contactNumber)\n$location"
                            userInfoText.visibility = View.VISIBLE
                        }
                    }
                }
            }
        })
    }

    private fun submitOrder() {
        Log.d(TAG, "submitOrder() triggered!")
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Build JSON request
        val subtotal = cartList.sumOf { it.price * it.quantity }
        val shippingCost = if (subtotal >= FREE_SHIPPING_THRESHOLD) 0.0 else SHIPPING_COST

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", userId)
            put("subtotal", subtotal)
            put("shipping_cost", shippingCost)
            put("total_price", cartTotal)
            put("payment_method", paymentMethodText.text.toString())
            put("cart_items", JSONArray().apply {
                cartList.forEach { item ->
                    val itemJson = JSONObject().apply {
                        put("product_id", item.productId)
                        put("quantity", item.quantity)
                        put("price", item.price)
                    }
                    put(itemJson)
                }
            })
        }
        Log.d(TAG, "Request JSON: $jsonObject")
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("http://192.168.1.12/backend/submit_order.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "API Response: $responseBody")
                runOnUiThread {
                    if (response.isSuccessful && responseBody?.contains("success") == true) {
                        // Save order to history before finishing checkout
                        savePurchasedProducts()
                        showOrderPlacedToast()
                        finish()
                    } else {
                        Toast.makeText(this@CheckoutActivity, "Failed to place order.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Save purchased products to SharedPreferences as order history (local demo)
    private fun savePurchasedProducts() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val existingJson = sharedPrefs.getString("order_history", "[]")
        val orderHistoryArray = JSONArray(existingJson)
        // Save each purchased item (you can adjust fields as needed)
        cartList.forEach { item ->
            val productJson = JSONObject().apply {
                put("productName", item.productName)
                put("price", item.price)
                put("quantity", item.quantity)
            }
            orderHistoryArray.put(productJson)
        }
        sharedPrefs.edit().putString("order_history", orderHistoryArray.toString()).apply()
        Log.d(TAG, "Saved order history: $orderHistoryArray")
    }

    private fun showOrderPlacedToast() {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.order_placed, null)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

    companion object {
        const val REQUEST_CODE_ADDRESS = 1001
    }
}
