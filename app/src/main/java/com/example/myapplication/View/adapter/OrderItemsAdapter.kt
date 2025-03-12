package com.example.myapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class OrderItemsAdapter(private val items: List<OrderItem>) :
    RecyclerView.Adapter<OrderItemsAdapter.OrderItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return OrderItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderItemViewHolder, position: Int) {
        val item = items[position]
        holder.productName.text = item.name
        holder.productQuantity.text = "Quantity: ${item.quantity}"
        holder.productPrice.text = "₱${item.price}"

        // ✅ Log Image URL to debug if the URL is correct
        Log.d("OrderItemsAdapter", "Loading Image URL: ${item.imageUrl}")

        // ✅ Fix Image Loading with Glide
        Glide.with(holder.itemView.context)
            .load(item.imageUrl.trim()) // Trim spaces
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.cat) // Show placeholder while loading
                    .error(R.drawable.error_image) // Show error image if URL is broken
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache images for better performance
            )
            .into(holder.productImage)
    }

    override fun getItemCount(): Int = items.size

    class OrderItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.productName)
        val productQuantity: TextView = view.findViewById(R.id.productQuantity)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val productImage: ImageView = view.findViewById(R.id.productImage)
    }
}
