package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderId: TextView = view.findViewById(R.id.order_id)
        val orderDate: TextView = view.findViewById(R.id.order_date)
        val orderStatus: TextView = view.findViewById(R.id.order_status)
        val totalPrice: TextView = view.findViewById(R.id.total_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderId.text = "Order #${order.id}"
        holder.orderDate.text = order.createdAt
        holder.totalPrice.text = "₱${order.totalPrice}"

        // ✅ Set Order Status Color
        when (order.status) {
            "Pending" -> {
                holder.orderStatus.text = "Pending"
                holder.orderStatus.setTextColor(Color.parseColor("#FFC107")) // Yellow
            }
            "To Ship" -> {
                holder.orderStatus.text = "To Ship"
                holder.orderStatus.setTextColor(Color.parseColor("#007BFF")) // Blue
            }
            "Shipped" -> {
                holder.orderStatus.text = "Shipped"
                holder.orderStatus.setTextColor(Color.parseColor("#00BFFF")) // Light Blue
            }
            "Delivered" -> {
                holder.orderStatus.text = "Delivered"
                holder.orderStatus.setTextColor(Color.parseColor("#28A745")) // Green
            }
            else -> {
                holder.orderStatus.text = order.status
                holder.orderStatus.setTextColor(Color.BLACK) // Default Black
            }
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
