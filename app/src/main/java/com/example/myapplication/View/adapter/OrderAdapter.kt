package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager

class OrderAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderDate: TextView = view.findViewById(R.id.order_date)
        val orderStatus: TextView = view.findViewById(R.id.order_status)
        val totalPrice: TextView = view.findViewById(R.id.total_price)
        val viewDetailsButton: Button = view.findViewById(R.id.view_details_button)
        val cancelOrderButton: Button = view.findViewById(R.id.cancel_order_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderDate.text = order.createdAt
        holder.totalPrice.text = "₱${order.totalPrice}"

        // Set Order Status Color and Show/Hide Cancel Button
        when (order.status) {
            "Pending" -> {
                holder.orderStatus.text = "Pending"
                holder.orderStatus.setTextColor(Color.parseColor("#FFC107")) // Yellow
                holder.cancelOrderButton.visibility = View.VISIBLE // Show Cancel Button
            }
            "To Ship" -> {
                holder.orderStatus.text = "To Ship"
                holder.orderStatus.setTextColor(Color.parseColor("#007BFF")) // Blue
                holder.cancelOrderButton.visibility = View.GONE
            }
            "Shipped" -> {
                holder.orderStatus.text = "Shipped"
                holder.orderStatus.setTextColor(Color.parseColor("#00BFFF")) // Light Blue
                holder.cancelOrderButton.visibility = View.GONE
            }
            "Delivered" -> {
                holder.orderStatus.text = "Delivered"
                holder.orderStatus.setTextColor(Color.parseColor("#28A745")) // Green
                holder.cancelOrderButton.visibility = View.GONE
            }
            else -> {
                holder.orderStatus.text = order.status
                holder.orderStatus.setTextColor(Color.BLACK) // Default Black
                holder.cancelOrderButton.visibility = View.GONE
            }
        }

        // Handle "View Details" Button Click
        holder.viewDetailsButton.setOnClickListener {
            showOrderDetailsPopup(holder.itemView.context, order)
        }

        // Handle "Cancel Order" Button Click
        holder.cancelOrderButton.setOnClickListener {
            confirmCancellation(holder.itemView.context, order)
        }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    // Show Popup with Order Details
    private fun showOrderDetailsPopup(context: Context, order: Order) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_order_details, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.orderItemsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = OrderItemsAdapter(order.items)

        builder.setView(view)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    // Confirm Cancellation with Dialog
    private fun confirmCancellation(context: Context, order: Order) {
        AlertDialog.Builder(context)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel Order #${order.id}?")
            .setPositiveButton("Yes") { _, _ ->
                cancelOrder(context, order)
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }


    // Send Cancel Request to Backend
    private fun cancelOrder(context: Context, order: Order) {
        val url = "http://192.168.1.12/backend/cancel_order.php?order_id=${order.id}"
        val requestQueue = Volley.newRequestQueue(context)

        val request = JsonObjectRequest(Request.Method.POST, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    Toast.makeText(context, "Order #${order.id} canceled successfully!", Toast.LENGTH_SHORT).show()
                    // Remove the order from the list
                    val mutableOrders = orders.toMutableList()
                    mutableOrders.remove(order)
                    orders = mutableOrders
                    notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Failed to cancel order: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            })

        requestQueue.add(request)
    }
}