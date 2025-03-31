package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class OrderAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    // Callback for "View Details" clicks (optional)
    var onViewDetailsClicked: ((Order) -> Unit)? = null

    // Callback for when an order is successfully cancelled. Passes the updated order.
    var onOrderCancelled: ((Order) -> Unit)? = null

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

        // Display basic order info
        holder.orderDate.text = order.createdAt
        holder.totalPrice.text = "₱${order.totalPrice}"

        // Set "View Details" button text to black
        holder.viewDetailsButton.setTextColor(Color.BLACK)

        // Set order status display and button behavior based on the order's status
        when (order.status) {
            "Pending" -> {
                holder.orderStatus.text = "Pending"
                holder.orderStatus.setTextColor(Color.parseColor("#FFC107")) // Yellow
                holder.cancelOrderButton.visibility = View.VISIBLE
                holder.cancelOrderButton.text = "Cancel Order"
                holder.cancelOrderButton.setOnClickListener {
                    confirmCancellation(holder.itemView.context, order)
                }
            }
            "Cancelled" -> {
                holder.orderStatus.text = "Cancelled"
                holder.orderStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
                holder.cancelOrderButton.visibility = View.VISIBLE
                holder.cancelOrderButton.text = "Buy Again"
                holder.cancelOrderButton.setOnClickListener {
                    Toast.makeText(
                        holder.itemView.context,
                        "Buy Again for Order #${order.id}",
                        Toast.LENGTH_SHORT
                    ).show()
                    // TODO: Implement your "Buy Again" logic here.
                }
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
                holder.orderStatus.setTextColor(Color.BLACK)
                holder.cancelOrderButton.visibility = View.GONE
            }
        }

        // Handle "View Details" click: use the callback if set or show a popup
        holder.viewDetailsButton.setOnClickListener {
            onViewDetailsClicked?.invoke(order) ?: run {
                showOrderDetailsPopup(holder.itemView.context, order)
            }
        }
    }

    override fun getItemCount(): Int = orders.size

    // Update the list of orders and refresh the adapter
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    private fun showOrderDetailsPopup(context: Context, order: Order) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_order_details, null)

        val userNameTextView = view.findViewById<TextView>(R.id.userNameTextView)
        val addressTextView = view.findViewById<TextView>(R.id.addressTextView)
        val phoneNumberTextView = view.findViewById<TextView>(R.id.phoneNumberTextView)

        userNameTextView.text = "Name: ${order.userName}"
        addressTextView.text = "Address: ${order.address}"
        phoneNumberTextView.text = "Phone: ${order.phoneNumber}"

        val recyclerView = view.findViewById<RecyclerView>(R.id.orderItemsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = OrderItemsAdapter(order.items)

        builder.setView(view)
        builder.setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    // Confirm order cancellation with a dialog
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

    private fun cancelOrder(context: Context, order: Order) {
        val url = "http://192.168.80.63/backend/cancel_order.php?order_id=${order.id}"
        val requestQueue = Volley.newRequestQueue(context)

        val request = JsonObjectRequest(
            Request.Method.POST, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    // Notify the activity with the updated (cancelled) order
                    onOrderCancelled?.invoke(order.copy(status = "Cancelled"))
                } else {
                    Toast.makeText(context, "Failed to cancel order!", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        requestQueue.add(request)
    }
}
