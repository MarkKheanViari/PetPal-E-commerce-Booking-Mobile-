package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ServiceAdapter(private val context: Context, private val serviceList: List<ServiceModel>) :
    RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = serviceList[position]

        holder.serviceName.text = service.name
        holder.servicePrice.text = "Price: ₱${service.price}"
        holder.serviceDescription.text = service.description

        // Load image using Glide
        Glide.with(context)
            .load("http://192.168.1.65/backend/${service.image}") // Adjust this URL if needed
            .placeholder(R.drawable.cat) // fallback image
            .into(holder.serviceImage)

        // Handle Book Now button
        holder.bookNowButton.setOnClickListener {
            val intent = if (service.type == "Grooming") {
                Intent(context, GroomingAppointmentActivity::class.java)
            } else {
                Intent(context, VeterinaryAppointmentActivity::class.java)
            }
            intent.putExtra("SERVICE_NAME", service.name)
            intent.putExtra("SERVICE_PRICE", service.price)
            intent.putExtra("SERVICE_IMAGE", service.image)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = serviceList.size

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceImage: ImageView = itemView.findViewById(R.id.serviceImage)
        val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        val servicePrice: TextView = itemView.findViewById(R.id.servicePrice)
        val serviceDescription: TextView = itemView.findViewById(R.id.serviceDescription)
        val bookNowButton: Button = itemView.findViewById(R.id.bookNowButton)
    }
}
