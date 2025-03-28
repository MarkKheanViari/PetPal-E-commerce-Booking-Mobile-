package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ProductAdapter(
    private val context: Context,
    private var products: MutableList<Product>,
    private val wishlistListener: (Product) -> Unit,
    private val reportListener: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var lastQuery: String? = null

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.productName)
        val itemPrice: TextView = view.findViewById(R.id.productPrice)
        val itemStock: TextView = view.findViewById(R.id.productStock)
        val itemImage: ImageView = view.findViewById(R.id.productImage)
        val buyNowButton: Button = view.findViewById(R.id.buyNowButton)
        val reportButton: ImageButton = view.findViewById(R.id.reportButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.itemName.text = if (!lastQuery.isNullOrBlank()) {
            SpannableString(product.name).apply {
                val query = lastQuery!!.lowercase()
                val start = product.name.lowercase().indexOf(query)
                if (start != -1) {
                    setSpan(
                        BackgroundColorSpan(ContextCompat.getColor(context, android.R.color.holo_orange_light)),
                        start,
                        start + query.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        } else {
            product.name
        }

        holder.itemPrice.text = try {
            "₱${String.format("%,.2f", product.price.toFloat())}"
        } catch (e: NumberFormatException) {
            "₱${product.price}"
        }

        holder.itemStock.text = when {
            product.quantity > 0 -> "Stock: ${product.quantity}"
            product.quantity == 0 -> "Out of Stock"
            else -> "Stock: N/A"
        }

        Glide.with(context)
            .load(product.imageUrl)
            .placeholder(R.drawable.cat)
            .error(R.drawable.oranage_header)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .into(holder.itemImage)

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

        holder.buyNowButton.setOnClickListener {
            val productMap = HashMap<String, String>().apply {
                put("product_id", product.id.toString())
                put("name", product.name)
                put("price", product.price)
                put("image", product.imageUrl)
                put("quantity", "1")
            }
            val cartItems = arrayListOf(productMap)
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra("cartItems", cartItems)
            }
            context.startActivity(intent)
        }

        holder.buyNowButton.isEnabled = product.quantity > 0
        holder.buyNowButton.alpha = if (product.quantity > 0) 1.0f else 0.5f

        // Ensure report button is visible and log binding
        android.util.Log.d("ProductAdapter", "Binding report button for ${product.name}")
        holder.reportButton.visibility = View.VISIBLE
        holder.reportButton.setOnClickListener {
            reportListener(product)
        }
    }

    fun updateProducts(newProducts: List<Product>, query: String? = null) {
        lastQuery = query
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun getProducts(): List<Product> = products.toList()
}