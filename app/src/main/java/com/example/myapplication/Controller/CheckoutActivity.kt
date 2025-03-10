package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button  // Remove if not needed
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
        placeOrderButton = findViewById(R.id.checkoutBtn)

        // Retrieve cart items from intent extras (or use an empty list)
        cartItems = intent.getSerializableExtra("cartItems") as? ArrayList<HashMap<String, String>> ?: arrayListOf()
        cartList = arrayListOf()

        // Set up Place Order button listener
        placeOrderButton.setOnClickListener {
            Log.d("CheckoutActivity", "✅ Place Order Button Clicked!")
            submitOrder()
        }

        // Optional: Additional click listener for the same checkout button
        val checkoutBtn = findViewById<ImageView>(R.id.checkoutBtn)
        checkoutBtn.setOnClickListener { submitOrder() }

        // Convert raw cart items into CartItem objects for calculation
        cartList.addAll(cartItems.map {
            CartItem(
                productId = it["product_id"]?.toInt() ?: 0,
                productName = it["product_name"] ?: "Unknown",
                quantity = it["quantity"]?.toInt() ?: 1,
                price = it["price"]?.toDouble() ?: 0.0
            )
        })

        // Fetch user info from backend
        fetchUserInfo()

        // Calculate total price and update UI
        calculateTotal()

        // Initialize RecyclerView with CheckoutAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val checkoutAdapter = CheckoutAdapter(this, cartItems)
        recyclerView.adapter = checkoutAdapter

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
                        // Show the custom "Order Placed" toast in the top right corner
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
