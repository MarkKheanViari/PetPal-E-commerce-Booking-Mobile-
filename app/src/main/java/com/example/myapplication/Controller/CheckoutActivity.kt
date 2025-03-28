package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class CheckoutActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalTextView: TextView
    private lateinit var userInfoText: TextView
    private lateinit var addressName: TextView
    private lateinit var addressPhone: TextView
    private lateinit var addressDetails: TextView

    // Legacy button from previous logic (hidden in XML)
    private lateinit var selectPaymentButton: Button

    // Payment UI: we use checkboxes now to select a payment method.
    private lateinit var paymentMethodText: TextView
    private lateinit var paymentMethodIcon: ImageView
    private lateinit var placeOrderButton: Button
    private lateinit var addressSection: LinearLayout

    // Payment checkboxes
    private lateinit var gcashCheckbox: CheckBox
    private lateinit var codCheckbox: CheckBox

    // Cart variables
    private lateinit var cartItems: ArrayList<HashMap<String, String>>
    private lateinit var cartList: ArrayList<CartItem>
    private var cartTotal: Double = 0.0

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Create the notification channel for Android O and above.
        createNotificationChannel()

        // Handle any deep link
        handleDeepLink(intent)

        // Toolbar / back button
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        // Find UI references
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        totalTextView = findViewById(R.id.totalTextView)
        userInfoText = findViewById(R.id.userInfoText)
        addressName = findViewById(R.id.addressName)
        addressPhone = findViewById(R.id.addressPhone)
        addressDetails = findViewById(R.id.addressDetails)
        selectPaymentButton = findViewById(R.id.selectPaymentButton) // legacy; will be hidden
        paymentMethodText = findViewById(R.id.paymentMethodText)
        paymentMethodIcon = findViewById(R.id.paymentMethodIcon)
        placeOrderButton = findViewById(R.id.checkoutBtn)
        addressSection = findViewById(R.id.addressSection)

        gcashCheckbox = findViewById(R.id.gcashCheckbox)
        codCheckbox = findViewById(R.id.codCheckbox)

        // Hide the legacy select payment button
        selectPaymentButton.visibility = View.GONE

        // Payment checkbox logic: only allow one to be selected at a time.
        gcashCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                codCheckbox.isChecked = false
                paymentMethodText.text = "GCASH"
                paymentMethodIcon.visibility = View.VISIBLE
            } else {
                if (!codCheckbox.isChecked) {
                    paymentMethodText.text = "Select Payment Method"
                    paymentMethodIcon.visibility = View.GONE
                }
            }
        }
        codCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                gcashCheckbox.isChecked = false
                paymentMethodText.text = "COD (Cash on Delivery)"
                paymentMethodIcon.visibility = View.GONE
            } else {
                if (!gcashCheckbox.isChecked) {
                    paymentMethodText.text = "Select Payment Method"
                }
            }
        }

        // Optionally, allow payment method selection via the legacy select button.
        selectPaymentButton.setOnClickListener {
            val paymentOptions = arrayOf("COD (Cash on Delivery)", "GCASH")
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Select Payment Method")
            builder.setItems(paymentOptions) { _, which ->
                val selectedMethod = paymentOptions[which]
                paymentMethodText.text = selectedMethod
                paymentMethodIcon.visibility = if (selectedMethod == "GCASH") View.VISIBLE else View.GONE
                // Update checkboxes accordingly.
                gcashCheckbox.isChecked = selectedMethod == "GCASH"
                codCheckbox.isChecked = selectedMethod.contains("COD")
            }
            builder.show()
        }

        // Make address section clickable.
        addressSection.setOnClickListener { navigateToAddressSelection(it) }

        // Get product details and cart items from Intent extras.
        val productId = intent.getIntExtra("productId", -1)
        val productName = intent.getStringExtra("productName") ?: "Unknown"
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val productImage = intent.getStringExtra("productImage") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)

        cartItems = intent.getSerializableExtra("cartItems") as? ArrayList<HashMap<String, String>>
            ?: arrayListOf()

        // If no cart items exist but a product was passed, add it as a single-item cart.
        if (cartItems.isEmpty() && productId != -1) {
            val productMap = HashMap<String, String>().apply {
                put("product_id", productId.toString())
                put("name", productName)
                put("price", productPrice.toString())
                put("image", productImage)
                put("description", "No description available")
                put("quantity", quantity.toString())
            }
            cartItems.add(productMap)
        }

        // Convert cartItems (HashMap list) to cartList (CartItem data class list).
        cartList = ArrayList()
        cartList.addAll(cartItems.map {
            CartItem(
                productId = it["product_id"]?.toInt() ?: 0,
                productName = it["name"] ?: "Unknown",
                imageUrl = it["image"] ?: "",
                description = it["description"] ?: "No description available",
                quantity = it["quantity"]?.toInt() ?: 1,
                price = it["price"]?.toDoubleOrNull() ?: 0.0
            )
        })

        // Set up RecyclerView.
        recyclerView.layoutManager = LinearLayoutManager(this)
        val checkoutAdapter = CheckoutAdapter(this, cartItems)
        recyclerView.adapter = checkoutAdapter

        // Set click listener for placing the order.
        placeOrderButton.setOnClickListener { submitOrder() }

        // Fetch user info (including address).
        fetchUserInfo()

        // Calculate totals and order summary.
        calculateTotal()
        calculateOrderSummary()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            when (uri.path) {
                "/payment/success" -> {
                    val orderId = uri.getQueryParameter("order_id")
                    Toast.makeText(this, "✅ Payment successful for Order #$orderId", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
                "/payment/cancel" -> {
                    Toast.makeText(this, "⚠ Payment canceled", Toast.LENGTH_LONG).show()
                    // Remain in CheckoutActivity to allow a retry.
                }
            }
        }
    }

    private fun fetchUserInfo() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)
        Log.d("CheckoutActivity", "mobileUserId from SharedPreferences: $mobileUserId")

        if (mobileUserId == -1) {
            Log.e("CheckoutActivity", "No user logged in!")
            runOnUiThread {
                Toast.makeText(this@CheckoutActivity, "❌ Please login to continue", Toast.LENGTH_SHORT).show()
                finish()
            }
            return
        }

        val url = "http://192.168.1.12/backend/fetch_user_info.php?mobile_user_id=$mobileUserId"
        Log.d("CheckoutActivity", "Request URL: $url")
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckoutActivity", "Network failure: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "❌ Network failure: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CheckoutActivity", "Response: $responseBody")
                if (!responseBody.isNullOrEmpty()) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            val username = jsonResponse.optString("username", "Unknown User")
                            val contactNumber = jsonResponse.optString("contact_number", "No contact")
                            val location = jsonResponse.optString("location", "No address available")
                            runOnUiThread {
                                addressName.text = username
                                addressPhone.text = contactNumber
                                addressDetails.text = location
                                Toast.makeText(this@CheckoutActivity, "✅ User info fetched", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMessage = jsonResponse.optString("message", "Unknown error")
                            Log.e("CheckoutActivity", "Backend error: $errorMessage")
                            runOnUiThread {
                                Toast.makeText(this@CheckoutActivity, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CheckoutActivity", "Error parsing JSON: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this@CheckoutActivity, "❌ Error parsing response: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("CheckoutActivity", "Empty response from server")
                    runOnUiThread {
                        Toast.makeText(this@CheckoutActivity, "❌ Empty response from server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun submitOrder() {
        Log.d("CheckoutActivity", "🚀 submitOrder() function triggered!")
        // Decide payment method using checkboxes.
        val paymentMethod = paymentMethodText.text.toString().trim()
        if (paymentMethod.equals("Select Payment Method", ignoreCase = true) || paymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return
        }

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
            put("payment_method", paymentMethod)
            put("cart_items", JSONArray().apply {
                for (item in cartList) {
                    val itemJson = JSONObject().apply {
                        put("product_id", item.productId)
                        put("quantity", item.quantity)
                        put("price", item.price)
                        put("name", item.productName)
                        put("image", item.imageUrl)
                        put("description", item.description)
                    }
                    put(itemJson)
                }
            })
        }
        Log.d("CheckoutActivity", "📦 Request JSON: $jsonObject")
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())

        if (paymentMethod.equals("GCASH", ignoreCase = true)) {
            Log.d("CheckoutActivity", "⚡ Using PayMongo GCASH Payment")
            val request = Request.Builder()
                .url("http://192.168.1.12/backend/paymongo_checkout.php")
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
                        try {
                            val jsonResponse = JSONObject(responseBody ?: "{}")
                            if (jsonResponse.optBoolean("success", false)) {
                                val checkoutUrl = jsonResponse.optString("checkout_url", "")
                                if (checkoutUrl.isNotEmpty()) {
                                    Log.d("CheckoutActivity", "🌐 Redirecting to PayMongo: $checkoutUrl")
                                    val intent = Intent(this@CheckoutActivity, WebViewActivity::class.java)
                                    intent.putExtra("url", checkoutUrl)
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@CheckoutActivity, "❌ Error: Missing checkout URL", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val errorMessage = jsonResponse.optString("message", "Unknown error")
                                Toast.makeText(this@CheckoutActivity, "❌ $errorMessage", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: JSONException) {
                            Log.e("CheckoutActivity", "❌ JSON Parsing Error: ${e.message}")
                            Toast.makeText(this@CheckoutActivity, "❌ JSON Parsing Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } else {
            // COD order submission.
            val request = Request.Builder()
                .url("http://192.168.1.12/backend/submit_order.php")
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
                            // Add a local notification for the order summary.
                            val productNames = cartList.joinToString { it.productName }
                            addLocalNotification("Order Placed: $productNames")
                            // Also send a system notification to the user.
                            sendOrderNotification("Order Placed", "Your order for $productNames has been placed successfully!")
                            // Optionally clear cart items on the server.
                            clearCartItems(userId)
                            finish()
                        } else {
                            Toast.makeText(this@CheckoutActivity, "❌ Failed to place order.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun addLocalNotification(message: String) {
        val sp = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val existingString = sp.getString("notifications", "[]")
        val array = JSONArray(existingString)
        array.put(message)
        sp.edit().putString("notifications", array.toString()).apply()
    }

    private fun clearCartItems(userId: Int) {
        val url = "http://192.168.1.12/backend/clear_cart.php"
        val json = JSONObject().apply {
            put("mobile_user_id", userId)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CheckoutActivity", "❌ Failed to clear cart: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@CheckoutActivity, "❌ Failed to clear cart", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("CheckoutActivity", "✅ Cart cleared successfully")
            }
        })
    }

    private fun calculateTotal() {
        cartTotal = cartList.sumOf { it.price * it.quantity }
        totalTextView.text = "Total: ₱%.2f".format(cartTotal)
        Log.d("CheckoutActivity", "Calculated cartTotal: $cartTotal")
    }

    private fun calculateOrderSummary() {
        var subtotal = 0.0
        var totalItems = 0

        for (item in cartList) {
            subtotal += item.price * item.quantity
            totalItems += item.quantity
        }

        val shippingFee = 50.0
        val total = subtotal + shippingFee

        val subtotalTextView = findViewById<TextView>(R.id.subtotalTextView)
        val orderTotalPriceSummary = findViewById<TextView>(R.id.orderTotalPriceSummary)

        subtotalTextView.text = "₱%.2f".format(subtotal)
        orderTotalPriceSummary.text = "₱%.2f".format(total)

        Log.d("CheckoutActivity", "Subtotal: $subtotal, Total: $total, Items: $totalItems")
    }

    fun navigateToAddressSelection(view: View) {
        val intent = Intent(this, AddressSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_ADDRESS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADDRESS && resultCode == RESULT_OK) {
            val selectedAddress = data?.getStringExtra("selectedAddress")
            addressDetails.text = selectedAddress ?: "No address selected"
        }
    }

    private fun showOrderPlacedToast() {
        val inflater: LayoutInflater = layoutInflater
        val layout = inflater.inflate(R.layout.order_placed, null)
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.setGravity(Gravity.TOP or Gravity.END, 16, 16)
        toast.show()
    }

    // Create a notification channel (for Android O and above)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "order_channel"
            val channelName = "Order Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendOrderNotification(title: String, message: String) {
        val channelId = "order_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Replace with a valid icon resource. For testing, using a built-in icon:
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a unique notification ID. Here we use current time.
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }


    companion object {
        const val REQUEST_CODE_ADDRESS = 1001
    }
}
