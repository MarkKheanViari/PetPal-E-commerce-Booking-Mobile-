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
            val intent = Intent(this, AddAddressActivity::class.java)
            startActivity(intent) // Go to Add Address Activity
        }

        addressListView.setOnItemClickListener { _, _, position, _ ->
            val selectedAddress = "${addressList[position]["full_name"]} (${addressList[position]["phone_number"]})\n${addressList[position]["street"]}, ${addressList[position]["region"]}"
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

        val url = "http://192.168.137.14/backend/fetch_user_addresses.php?mobile_user_id=$mobileUserId"
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
                        val addresses = jsonResponse.optJSONArray("addresses") ?: JSONArray()
                        addressList.clear()

                        for (i in 0 until addresses.length()) {
                            val address = addresses.getJSONObject(i)
                            val addressItem = hashMapOf(
                                "full_name" to address.getString("full_name"),
                                "phone_number" to address.getString("phone_number"),
                                "street" to address.getString("street"),
                                "region" to address.optString("region", "N/A"),
                                "label" to address.getString("label"),
                                "is_default" to address.getString("is_default")
                            )
                            addressList.add(addressItem)
                        }

                        runOnUiThread {
                            val adapter = SimpleAdapter(
                                this@AddressSelectionActivity,
                                addressList,
                                android.R.layout.simple_list_item_2,
                                arrayOf("full_name", "street"),
                                intArrayOf(android.R.id.text1, android.R.id.text2)
                            )
                            addressListView.adapter = adapter
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@AddressSelectionActivity, "❌ No addresses found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
}
