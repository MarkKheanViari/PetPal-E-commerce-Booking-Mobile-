package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProductAdapter(
    private val context: Context,
    private var products: MutableList<Product>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.productName)
        val itemPrice: TextView = view.findViewById(R.id.productPrice)
        val itemStock: TextView = view.findViewById(R.id.productStock)
        val itemImage: ImageView = view.findViewById(R.id.productImage)
        val addToCartButton: Button = view.findViewById(R.id.addToCartButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        Log.d("DEBUG:ProductAdapter", "getItemCount() returns ${products.size}")
        return products.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        Log.d("DEBUG:ProductAdapter", "onBindViewHolder() for position $position: ${product.name}")

        // Set product details
        holder.itemName.text = product.name
        holder.itemPrice.text = "₱${product.price}"
        holder.itemStock.text = "Stock: ${product.quantity}"

        // Load image with Glide
        Glide.with(context)
            .load(product.imageUrl)
            .into(holder.itemImage)

        // Setup add-to-cart button state
        if (product.quantity > 0) {
            holder.addToCartButton.isEnabled = true
            holder.addToCartButton.text = "Add to Cart"
            holder.addToCartButton.setBackgroundColor(Color.parseColor("#FF9800"))
            holder.addToCartButton.setOnClickListener {
                addToCart(product, position)
            }
        } else {
            holder.addToCartButton.isEnabled = false
            holder.addToCartButton.text = "Out of Stock"
            holder.addToCartButton.setBackgroundColor(Color.GRAY)
        }

        // Clicking the image opens details
        holder.itemImage.setOnClickListener {
            val intent = Intent(context, ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productImage", product.imageUrl)
                putExtra("productDescription", product.description)
                putExtra("productPrice", product.price)
            }
            context.startActivity(intent)
        }
    }

    private fun addToCart(product: Product, position: Int) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(context, "❌ Please login to add to cart", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", product.id)
            put("quantity", 1)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.12/backend/add_to_cart.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Handler(Looper.getMainLooper()).post {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val success = jsonResponse.optBoolean("success", false)
                        if (success) {
                            Toast.makeText(context, "✅ Added to Cart!", Toast.LENGTH_SHORT).show()
                            // Decrement stock and update only this item
                            product.quantity--
                            notifyItemChanged(position)
                        } else {
                            Toast.makeText(
                                context,
                                "❌ Error: ${jsonResponse.optString("message")}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    /**
     * Update the adapter's products list and notify changes.
     */
    fun updateProducts(newProducts: List<Product>) {
        Log.d("DEBUG:ProductAdapter", "updateProducts() called with ${newProducts.size} items")
        products.clear()
        products.addAll(newProducts)
        Log.d("DEBUG:ProductAdapter", "After update, getItemCount() returns ${getItemCount()}")
        notifyDataSetChanged()
    }

    fun getProducts(): List<Product> = products
}
