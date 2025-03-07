package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CheckoutAdapter(
    private val context: Context,
    private val cartItems: List<HashMap<String, String>>
) : RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.checkout_item, parent, false)
        return CheckoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckoutViewHolder, position: Int) {
        val item = cartItems[position]
        holder.productNameTextView.text = item["name"]
        holder.quantityTextView.text = "Quantity: ${item["quantity"]}"
        holder.priceTextView.text = "₱ ${item["price"]}"

        // Load product image using Glide.
        Glide.with(context).load(item["image"]).into(holder.productImageView)
    }

    override fun getItemCount(): Int = cartItems.size

    class CheckoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImageView: ImageView = view.findViewById(R.id.productImageView)
        val productNameTextView: TextView = view.findViewById(R.id.productNameTextView)
        val quantityTextView: TextView = view.findViewById(R.id.quantityTextView)
        val priceTextView: TextView = view.findViewById(R.id.priceTextView)
    }
}
