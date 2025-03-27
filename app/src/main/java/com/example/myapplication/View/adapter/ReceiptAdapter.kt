package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class ReceiptAdapter(
    private val receipts: JSONArray,
    private val itemClickListener: (JSONObject) -> Unit
) : RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder>() {

    inner class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReceiptSummary: TextView = itemView.findViewById(R.id.tvReceiptSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ReceiptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val receipt = receipts.getJSONObject(position)
        val serviceName = receipt.optString("service_name", "N/A")
        val date = receipt.optString("appointment_date", "N/A")
        val time = receipt.optString("appointment_time", "N/A")
        val summary = "Service: $serviceName | Date: $date | Time: $time"
        holder.tvReceiptSummary.text = summary

        holder.itemView.setOnClickListener { itemClickListener(receipt) }
    }

    override fun getItemCount(): Int = receipts.length()
}
