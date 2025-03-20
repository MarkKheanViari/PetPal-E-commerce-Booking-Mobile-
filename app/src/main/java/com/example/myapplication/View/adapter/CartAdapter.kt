package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<HashMap<String, String>>,
    private val listener: CartActionListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        holder.productName.text = item["name"]
        holder.productPrice.text = "₱${item["price"]}"
        holder.quantityText.text = item["quantity"]

        // Load product image using Glide (if you have the image URL)
        Glide.with(context).load(item["image"]).into(holder.productImage)

        holder.minusButton.setOnClickListener {
            val currentQuantity = holder.quantityText.text.toString().toInt()
            val newQuantity = currentQuantity - 1
            if (newQuantity > 0) {
                listener.updateCartQuantity(item["cart_id"]!!.toInt(), newQuantity)
            }
        }

        holder.plusButton.setOnClickListener {
            val currentQuantity = holder.quantityText.text.toString().toInt()
            val newQuantity = currentQuantity + 1
            listener.updateCartQuantity(item["cart_id"]!!.toInt(), newQuantity)
        }

        holder.removeItemButton.setOnClickListener {
            listener.removeItemFromCart(item["cart_id"]!!.toInt())
        }

        // Clicking the entire item -> show product details
        holder.itemView.setOnClickListener {
            listener.onProductClick(item)
        }
    }

    override fun getItemCount(): Int = cartItems.size

    class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImageView)
        val productName: TextView = view.findViewById(R.id.productNameTextView)
        val productPrice: TextView = view.findViewById(R.id.productPriceTextView)
        val quantityText: TextView = view.findViewById(R.id.quantityTextView)
        val minusButton: ImageView = view.findViewById(R.id.minusButton)
        val plusButton: ImageView = view.findViewById(R.id.plusButton)
        val removeItemButton: Button = view.findViewById(R.id.removeItemButton)
    }
}
