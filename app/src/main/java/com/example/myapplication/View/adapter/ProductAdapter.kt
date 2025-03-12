package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
        val buyNowButton: Button = view.findViewById(R.id.buyNowButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        // Display product details
        holder.itemName.text = product.name
        holder.itemPrice.text = "₱${product.price}"
        holder.itemStock.text = "Stock: ${product.quantity}"

        // Load image with Glide
        Glide.with(context)
            .load(product.imageUrl)
            .placeholder(R.drawable.cat)           // your placeholder drawable
            .error(R.drawable.oranage_header)      // your error drawable
            .into(holder.itemImage)

        // Open Product Details on item click
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

        // "Buy Now" -> create cart item & go to CheckoutActivity
        holder.buyNowButton.setOnClickListener {
            val productMap = HashMap<String, String>().apply {
                put("product_id", product.id.toString())
                put("name", product.name)
                put("price", product.price.toString())
                put("image", product.imageUrl)
                put("quantity", "1")
            }
            val cartItems = arrayListOf(productMap)
            val intent = Intent(context, CheckoutActivity::class.java).apply {
                putExtra("cartItems", cartItems)
            }
            context.startActivity(intent)
        }
    }

    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}
