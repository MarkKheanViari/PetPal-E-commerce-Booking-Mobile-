package com.example.myapplication

import android.content.Context
import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * A single CartFragment that merges all functionality
 * from CartActivity and CartFragment into one.
 */
class CartFragment : Fragment(), CartActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var checkoutButton: Button
    private lateinit var totalPriceTextView: TextView

    private val client = OkHttpClient()
    private val cartItems = mutableListOf<HashMap<String, String>>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate your cart fragment layout (rename if needed)
        return inflater.inflate(R.layout.activity_cart, container, false)
        // e.g., use R.layout.fragment_cart if you have renamed your layout file
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.cartRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        checkoutButton = view.findViewById(R.id.checkoutButton)
        totalPriceTextView = view.findViewById(R.id.totalPriceTextView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(requireContext(), cartItems, this)
        recyclerView.adapter = cartAdapter

        // Checkout button functionality
        checkoutButton.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                // Navigate to a CheckoutActivity or CheckoutFragment
                val intent = Intent(requireContext(), CheckoutActivity::class.java)
                intent.putExtra("cartItems", ArrayList(cartItems))
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "❌ Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch items from the server
        fetchCartItems()
    }

    /**
     * Called when quantity changes in the cart. Updates the server.
     */
    override fun updateCartQuantity(cartId: Int, newQuantity: Int) {
        val url = "http://192.168.1.65/backend/update_cart.php"
        val json = JSONObject().apply {
            put("cart_id", cartId)
            put("quantity", newQuantity)
        }
        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "❌ Failed to update quantity",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrEmpty()) {
                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)
                    val message = jsonResponse.optString("message", "")

                    activity?.runOnUiThread {
                        if (success) {
                            fetchCartItems() // Refresh the cart if the update was successful
                        } else {
                            Toast.makeText(requireContext(), "❌ $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Called when an item is removed from the cart. Updates the server.
     */
    override fun removeItemFromCart(cartId: Int) {
        val url = "http://192.168.1.65/backend/remove_from_cart.php"
        val json = JSONObject().apply { put("cart_id", cartId) }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "❌ Failed to remove item", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "✅ Item removed", Toast.LENGTH_SHORT).show()
                    fetchCartItems() // Refresh cart
                }
            }
        })
    }
    /**
     * When a product in the cart is clicked.
     * Opens ProductDetailsActivity (or a fragment).
     */
    override fun onProductClick(cartItem: HashMap<String, String>) {
        val intent = Intent(requireContext(), ProductDetailsActivity::class.java)
        val productId = cartItem["product_id"]?.toIntOrNull() ?: -1
        intent.putExtra("productId", productId)
        intent.putExtra("productName", cartItem["name"])
        intent.putExtra("productImage", cartItem["image"])
        intent.putExtra(
            "productDescription",
            cartItem["description"] ?: "No description available."
        )
        val productPrice = cartItem["price"]?.toDoubleOrNull() ?: 0.0
        intent.putExtra("productPrice", productPrice)
        startActivity(intent)
    }

    /**
     * Fetch the cart items from the server for the logged-in user.
     */
    private fun fetchCartItems() {
        val sharedPreferences =
            requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(requireContext(), "❌ Please login to view cart", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val url = "http://192.168.1.65/backend/fetch_cart.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "❌ Network Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
                            "description" to item.optString(
                                "description",
                                "No description available"
                            )
                        )
                        cartItems.add(cartItem)
                    }

                    activity?.runOnUiThread {
                        // After data is updated, notify the adapter and update visibility
                        cartAdapter.notifyDataSetChanged()

                        emptyText.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
                        recyclerView.visibility =
                            if (cartItems.isNotEmpty()) View.VISIBLE else View.GONE

                        totalPriceTextView.text = "₱ %.2f".format(totalPrice)
                    }
                }
            }
        })
    }
}