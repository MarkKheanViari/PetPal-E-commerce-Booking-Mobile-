package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AddressSelectionActivity : AppCompatActivity() {

    private lateinit var addressListView: ListView
    private lateinit var addAddressButton: Button
    private val client = OkHttpClient()
    private val addressList = mutableListOf<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_selection)

        addressListView = findViewById(R.id.addressListView)
        addAddressButton = findViewById(R.id.addAddressButton)

        fetchUserAddresses()

        addAddressButton.setOnClickListener {
            Toast.makeText(this, "Add Address Feature Coming Soon!", Toast.LENGTH_SHORT).show()
            // Handle adding a new address here
        }

        addressListView.setOnItemClickListener { _, _, position, _ ->
            val selectedAddress = "${addressList[position]["username"]} (${addressList[position]["contact_number"]})\n${addressList[position]["location"]}"
            val intent = Intent().apply {
                putExtra("selectedAddress", selectedAddress)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun fetchUserAddresses() {
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
                    Toast.makeText(this@AddressSelectionActivity, "❌ Failed to fetch addresses", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        val addressItem = hashMapOf(
                            "username" to jsonResponse.optString("username"),
                            "contact_number" to jsonResponse.optString("contact_number"),
                            "location" to jsonResponse.optString("location")
                        )

                        addressList.clear()
                        addressList.add(addressItem)

                        runOnUiThread {
                            val adapter = SimpleAdapter(
                                this@AddressSelectionActivity,
                                addressList,
                                android.R.layout.simple_list_item_2,
                                arrayOf("username", "location"),
                                intArrayOf(android.R.id.text1, android.R.id.text2)
                            )
                            addressListView.adapter = adapter
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@AddressSelectionActivity, "No address found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

}
