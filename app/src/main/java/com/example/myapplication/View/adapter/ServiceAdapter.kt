package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ServiceAdapter(private val serviceList: List<ServiceModel>) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = serviceList[position]
        holder.serviceName.text = service.serviceName
        holder.servicePrice.text = "₱${service.servicePrice}"
        holder.serviceDescription.text = service.serviceDescription
    }

    override fun getItemCount(): Int {
        return serviceList.size
    }

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        val servicePrice: TextView = itemView.findViewById(R.id.servicePrice)
        val serviceDescription: TextView = itemView.findViewById(R.id.serviceDescription)
    }
}
