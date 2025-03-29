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

        // Display time availability
        holder.serviceAvailability.text = if (service.startTime != null && service.endTime != null) {
            "${service.startTime} - ${service.endTime}" // Updated format to match screenshot
        } else {
            "Not specified"
        }

        // Display day availability
        holder.serviceDayAvailability.text = if (service.startDay != null && service.endDay != null) {
            "${service.startDay} - ${service.endDay}" // Updated format to match screenshot
        } else {
            "Not specified"
        }

        Glide.with(context)
            .load("http://192.168.1.12/backend/${service.image}")
            .placeholder(R.drawable.cat)
            .into(holder.serviceImage)

        holder.bookNowButton.setOnClickListener {
            val intent = if (service.type == "Grooming") {
                Intent(context, GroomingAppointmentActivity::class.java)
            } else {
                Intent(context, VeterinaryAppointmentActivity::class.java)
            }
            intent.putExtra("SERVICE_NAME", service.name)
            intent.putExtra("SERVICE_PRICE", service.price)
            intent.putExtra("SERVICE_IMAGE", service.image)
            intent.putExtra("START_TIME", service.startTime)
            intent.putExtra("END_TIME", service.endTime)
            intent.putExtra("START_DAY", service.startDay)
            intent.putExtra("END_DAY", service.endDay)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = serviceList.size

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceImage: ImageView = itemView.findViewById(R.id.serviceImage)
        val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        val servicePrice: TextView = itemView.findViewById(R.id.servicePrice)
        val serviceDescription: TextView = itemView.findViewById(R.id.serviceDescription)
        val serviceAvailability: TextView = itemView.findViewById(R.id.serviceAvailability)
        val serviceDayAvailability: TextView = itemView.findViewById(R.id.serviceDayAvailability)
        val bookNowButton: Button = itemView.findViewById(R.id.bookNowButton)
    }
}