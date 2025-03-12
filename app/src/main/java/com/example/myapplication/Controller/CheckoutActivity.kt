package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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

class CheckoutActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalTextView: TextView
    private lateinit var userInfoText: TextView
    private lateinit var selectPaymentButton: Button
    private lateinit var paymentMethodText: TextView
    // This ImageView serves as the Place Order/Checkout button.
    private lateinit var placeOrderButton: ImageView

    // Raw cart items passed from the previous activity
    private lateinit var cartItems: ArrayList<HashMap<String, String>>
    // List converted into CartItem objects for calculations
    private lateinit var cartList: ArrayList<CartItem>
    private var cartTotal: Double = 0.0

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Back button listener
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        // Initialize UI elements
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        totalTextView = findViewById(R.id.totalTextView)
        userInfoText = findViewById(R.id.userInfoText)
        selectPaymentButton = findViewById(R.id.selectPaymentButton)
        paymentMethodText = findViewById(R.id.paymentMethodText)
        placeOrderButton = findViewById(R.id.checkoutBtn) // using checkoutBtn as place order button

        // Retrieve individual product extras (if passed)
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName") ?: "Unknown"
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val productImage = intent.getStringExtra("productImage") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)

        // Retrieve the cart items from the previous activity (if any)
        cartItems = intent.getSerializableExtra("cartItems") as? ArrayList<HashMap<String, String>> ?: arrayListOf()

        // If cartItems is empty and individual product extras were passed, create a cart item from them.
        if (cartItems.isEmpty() && productId != -1) {
            val productMap = HashMap<String, String>().apply {
                put("product_id", productId.toString())
                put("name", productName)
                put("price", productPrice.toString())
                put("image", productImage)  // ✅ Ensure Image is here
                put("description", "No description available") // ✅ Default description if missing
                put("quantity", quantity.toString())
            }
            cartItems.add(productMap)
        }

        cartList = arrayListOf()

        // Set up Place Order button listener
        placeOrderButton.setOnClickListener {
            Log.d("CheckoutActivity", "✅ Place Order Button Clicked!")
            submitOrder()
        }

        // Optional: Additional click listener for the same checkout button
        val checkoutBtn = findViewById<ImageView>(R.id.checkoutBtn)
        checkoutBtn.setOnClickListener { submitOrder() }

        // Convert the raw cart data to CartItem objects for calculation
        cartList.addAll(cartItems.map {
            CartItem(
                productId = it["product_id"]?.toInt() ?: 0,
                productName = it["name"] ?: "Unknown",
                imageUrl = it["image"] ?: "", // ✅ Ensure Image URL is passed
                description = it["description"] ?: "No description available", // ✅ Ensure Description is passed
                quantity = it["quantity"]?.toInt() ?: 1,
                price = it["price"]?.toDouble() ?: 0.0
            )
        })

        // Fetch user info from backend
        fetchUserInfo()

        // Calculate total price and update UI
        calculateTotal()

        // Initialize RecyclerView with CheckoutAdapter (which displays product images, names, etc.)
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val checkoutAdapter = CheckoutAdapter(this, cartItems)
        recyclerView.adapter = checkoutAdapter

        // Calculate order summary (subtotal, shipping, total)
        calculateOrderSummary()

        // Payment Method Selection dialog
        selectPaymentButton.setOnClickListener {
            val paymentOptions = arrayOf("COD (Cash on Delivery)", "GCASH")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Select Payment Method")
            builder.setItems(paymentOptions) { _, which ->
                paymentMethodText.text = paymentOptions[which]
            }
            builder.show()
        }
    }

    private fun fetchUserInfo() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to continue", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val url = "http://192.168.1.65/backend/fetch_user_info.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "❌ Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        val username = jsonResponse.optString("username")
                        val contactNumber = jsonResponse.optString("contact_number")
                        val location = jsonResponse.optString("location")
                        runOnUiThread {
                            userInfoText.text = "$username ($contactNumber)\n$location"
                        }
                    }
                }
            }
        })
    }

    private fun submitOrder() {
        Log.d("CheckoutActivity", "🚀 submitOrder() function triggered!")
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        if (userId == -1) {
            Log.e("CheckoutActivity", "❌ User not logged in!")
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        // Build JSON request body
        val jsonObject = JSONObject().apply {
            put("mobile_user_id", userId)
            put("total_price", cartTotal)
            put("payment_method", paymentMethodText.text.toString())
            put("cart_items", JSONArray().apply {
                for (item in cartList) {
                    val itemJson = JSONObject().apply {
                        put("product_id", item.productId)
                        put("quantity", item.quantity)
                        put("price", item.price)
                        put("name", item.productName) // ✅ Include Name
                        put("image", item.imageUrl) // ✅ Include Image URL
                        put("description", item.description) // ✅ Include Description
                    }
                    put(itemJson)
                }
            })
        }

        Log.d("CheckoutActivity", "📦 Request JSON: $jsonObject")
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/submit_order.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckoutActivity", "❌ Network Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "❌ Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CheckoutActivity", "📦 API Response: $responseBody")
                runOnUiThread {
                    if (response.isSuccessful && responseBody?.contains("success") == true) {
                        showOrderPlacedToast()
                        finish()
                    } else {
                        Toast.makeText(this@CheckoutActivity, "❌ Failed to place order.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    private fun calculateTotal() {
        cartTotal = cartList.sumOf { it.price * it.quantity }
        totalTextView.text = "Total: ₱%.2f".format(cartTotal)
    }

    // Calculate Subtotal, Shipping, and Total Price
    private fun calculateOrderSummary() {
        var subtotal = 0.0
        var totalItems = 0

        // Loop through cart items and calculate the subtotal
        for (item in cartList) {
            subtotal += item.price * item.quantity
            totalItems += item.quantity
        }

        // Assume shipping is a fixed value. Modify as needed.
        val shippingFee = 50.0
        val total = subtotal + shippingFee

        // Update the UI with the calculated values
        val orderTotalPriceSummary = findViewById<TextView>(R.id.orderTotalPriceSummary)
        orderTotalPriceSummary.text = "₱%.2f".format(total)

        // Update the "Total Items" text dynamically
        orderTotalPriceSummary.text = "Total ($totalItems Item${if (totalItems > 1) "s" else ""})"
    }

    // Optional: Navigate to address selection if needed.
    fun navigateToAddressSelection(view: View) {
        val intent = Intent(this, AddressSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_ADDRESS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADDRESS && resultCode == RESULT_OK) {
            val selectedAddress = data?.getStringExtra("selectedAddress")
            userInfoText.text = selectedAddress
        }
    }

    private fun showOrderPlacedToast() {
        val inflater: LayoutInflater = layoutInflater
        val layout = inflater.inflate(R.layout.order_placed, null)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        // Set the gravity to top-right with 16-pixel offsets (adjust as needed)
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

    companion object {
        const val REQUEST_CODE_ADDRESS = 1001
    }
}
