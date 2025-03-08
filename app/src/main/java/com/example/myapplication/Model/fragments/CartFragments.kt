package com.example.myapplication

import android.content.Context
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

class CartFragment : Fragment(), CartActionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    // Removed backBtn property since it's no longer used in the layout
    private lateinit var checkoutButton: ImageView
    private lateinit var totalPriceTextView: TextView
    private val client = OkHttpClient()
    private val cartItems = mutableListOf<HashMap<String, String>>()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment (ensure this resource name matches your XML file)
        return inflater.inflate(R.layout.activity_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views in the fragment layout
        recyclerView = view.findViewById(R.id.cartRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        checkoutButton = view.findViewById(R.id.checkoutButton)
        totalPriceTextView = view.findViewById(R.id.totalPriceTextView)

        // Setup RecyclerView and adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(requireContext(), cartItems, this)
        recyclerView.adapter = cartAdapter

        // Removed back button functionality since there's no backBtn in the layout

        // Checkout button functionality
        checkoutButton.setOnClickListener {
            if (cartItems.isNotEmpty()) {
                Toast.makeText(requireContext(), "Proceed to checkout", Toast.LENGTH_SHORT).show()
                // Here you can load a CheckoutFragment or start CheckoutActivity if preferred.
            } else {
                Toast.makeText(requireContext(), "❌ Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        fetchCartItems()
    }

    // Update the cart quantity on the server
    override fun updateCartQuantity(cartId: Int, newQuantity: Int) {
        val url = "http://192.168.1.12/backend/update_cart.php"
        val json = JSONObject().apply {
            put("cart_id", cartId)
            put("quantity", newQuantity)
        }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "❌ Failed to update quantity", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "✅ Quantity updated", Toast.LENGTH_SHORT).show()
                    fetchCartItems() // Refresh cart items
                }
            }
        })
    }

    // Remove an item from the cart on the server
    override fun removeItemFromCart(cartId: Int) {
        val url = "http://192.168.1.12/backend/remove_from_cart.php"
        val json = JSONObject().apply { put("cart_id", cartId) }
        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "❌ Failed to remove item", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "✅ Item removed", Toast.LENGTH_SHORT).show()
                    fetchCartItems() // Refresh cart items
                }
            }
        })
    }

    // Handle click on a product item (e.g. open product details)
    override fun onProductClick(cartItem: HashMap<String, String>) {
        Toast.makeText(requireContext(), "Clicked on ${cartItem["name"]}", Toast.LENGTH_SHORT).show()
        // Implement navigation to a ProductDetailsFragment or start ProductDetailsActivity as needed
    }

    // Fetch cart items from the server
    private fun fetchCartItems() {
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(requireContext(), "❌ Please login to view cart", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        val url = "http://192.168.1.12/backend/fetch_cart.php?mobile_user_id=$mobileUserId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "❌ Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

                    activity?.runOnUiThread {
                        cartAdapter.notifyDataSetChanged()
                        emptyText.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
                        totalPriceTextView.text = "₱ %.2f".format(totalPrice)
                    }
                }
            }
        })
    }
}
