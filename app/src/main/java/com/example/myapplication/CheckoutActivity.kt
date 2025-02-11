package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class CheckoutActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalTextView: TextView
    private lateinit var userInfoText: TextView
    private lateinit var selectPaymentButton: Button
    private lateinit var paymentMethodText: TextView
    private lateinit var cartItems: ArrayList<HashMap<String, String>>
    private var totalPrice = 0.0
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Initialize UI elements
        recyclerView = findViewById(R.id.checkoutRecyclerView)
        totalTextView = findViewById(R.id.totalTextView)
        userInfoText = findViewById(R.id.userInfoText)
        selectPaymentButton = findViewById(R.id.selectPaymentButton)
        paymentMethodText = findViewById(R.id.paymentMethodText)

        // Get cart items from intent
        cartItems = intent.getSerializableExtra("cartItems") as ArrayList<HashMap<String, String>>

        // Fetch user info and display it
        fetchUserInfo()

        // Calculate total price
        calculateTotal()

        // Initialize RecyclerView with cart items
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

    private fun calculateTotal() {
        totalPrice = 0.0
        for (item in cartItems) {
            val price = item["price"]?.toDouble() ?: 0.0
            val quantity = item["quantity"]?.toInt() ?: 1
            totalPrice += price * quantity
        }
        totalTextView.text = "Total: ₱ %.2f".format(totalPrice)
    }

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
