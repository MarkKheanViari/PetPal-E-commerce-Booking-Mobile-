package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private lateinit var placeOrderButton: Button

    // This holds the raw cart items passed from the previous activity
    private lateinit var cartItems: ArrayList<HashMap<String, String>>
    // This list will be converted into a list of CartItem objects
    private lateinit var cartList: ArrayList<CartItem>
    private var cartTotal: Double = 0.0

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Hook up the back button to finish the activity
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            // Close the current activity and go back
            finish()
        }

        // Initialize UI elements
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        totalTextView = findViewById(R.id.totalTextView)
        userInfoText = findViewById(R.id.userInfoText)
        selectPaymentButton = findViewById(R.id.selectPaymentButton)
        paymentMethodText = findViewById(R.id.paymentMethodText)
        placeOrderButton = findViewById(R.id.placeOrderButton)

        // Retrieve cart items from intent extras (if none, use an empty list)
        cartItems = intent.getSerializableExtra("cartItems") as? ArrayList<HashMap<String, String>> ?: arrayListOf()
        cartList = arrayListOf()

        // Set up place order button click listener
        placeOrderButton.setOnClickListener {
            Log.d("CheckoutActivity", "✅ Place Order Button Clicked!")
            submitOrder()
        }

        // Add click listener for the checkout ImageView in bottom container
        val checkoutBtn = findViewById<ImageView>(R.id.checkoutBtn)
        checkoutBtn.setOnClickListener {
            submitOrder()
        }

        // Convert raw cart items to CartItem objects
        cartList.addAll(cartItems.map {
            CartItem(
                productId = it["product_id"]?.toInt() ?: 0,
                productName = it["product_name"] ?: "Unknown",
                quantity = it["quantity"]?.toInt() ?: 1,
                price = it["price"]?.toDouble() ?: 0.0
                // If you also store image in CartItem, you'd add it here, e.g.:
                // imageUrl = it["product_image"] ?: ""
            )
        })

        // Fetch user info from your backend
        fetchUserInfo()

        // Calculate total price and update totalTextView
        calculateTotal()

        // Initialize RecyclerView with a CheckoutAdapter
        // Make sure your CheckoutAdapter loads the image from "product_image" key
        recyclerView.layoutManager = LinearLayoutManager(this)
        val checkoutAdapter = CheckoutAdapter(this, cartItems)
        recyclerView.adapter = checkoutAdapter

        // Payment Method Selection
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

        val url = "http://192.168.43.215/backend/fetch_user_info.php?mobile_user_id=$mobileUserId"
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
            .url("http://192.168.43.215/backend/submit_order.php")
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
                        Toast.makeText(this@CheckoutActivity, "✅ Order Placed Successfully!", Toast.LENGTH_SHORT).show()
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

    // Method for address selection (if needed)
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

    companion object {
        const val REQUEST_CODE_ADDRESS = 1001
    }
}
