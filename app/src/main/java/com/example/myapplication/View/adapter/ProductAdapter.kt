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
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProductAdapter(
    private val context: Context,
    private var products: MutableList<Product>,
    private val wishlistListener: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.productName)
        val itemPrice: TextView = view.findViewById(R.id.productPrice)
        val itemStock: TextView = view.findViewById(R.id.productStock)
        val itemImage: ImageView = view.findViewById(R.id.productImage)
        val buyNowButton: Button = view.findViewById(R.id.buyNowButton) // ✅ Corrected reference
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Set product details
        holder.itemName.text = product.name
        holder.itemPrice.text = "₱${product.price}"
        holder.itemStock.text = "Stock: ${product.quantity}"

        // Load image with Glide
        Glide.with(context)
            .load(product.imageUrl)
            .into(holder.itemImage)

        // Open Product Details when clicking the item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ProductDetailsActivity::class.java).apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productImage", product.imageUrl)
                putExtra("productDescription", product.description)
                putExtra("productPrice", product.price)
            }
            context.startActivity(intent)
        }

        // Handle Buy Now Button Click
        holder.buyNowButton.setOnClickListener {
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra("productId", product.id)
                putExtra("productName", product.name)
                putExtra("productImage", product.imageUrl)
                putExtra("productPrice", product.price)
            }
            context.startActivity(intent)
        }
    }

    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
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
            .url("http://192.168.1.65/backend/add_to_cart.php")
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
}
