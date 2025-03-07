package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class OrderAdapter(private var orders: List<OrderHistoryItem>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.product_image)
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productManufacturer: TextView = itemView.findViewById(R.id.product_manufacturer)
        val productPrice: TextView = itemView.findViewById(R.id.product_price)
        val productBoughtQuantity: TextView = itemView.findViewById(R.id.product_bought_quantity)
        val orderAgainBtn: ImageView = itemView.findViewById(R.id.order_againtBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.productName.text = order.productName
        holder.productManufacturer.text = order.manufacturer
        holder.productPrice.text = "₱${order.price}"
        holder.productBoughtQuantity.text = "x${order.quantity}"

        // Placeholder image loading:
        holder.productImage.setImageResource(R.drawable.profile)

        holder.orderAgainBtn.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Ordering again: ${order.productName}", Toast.LENGTH_SHORT).show()
            // TODO: Implement actual re-order logic
        }
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<OrderHistoryItem>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
