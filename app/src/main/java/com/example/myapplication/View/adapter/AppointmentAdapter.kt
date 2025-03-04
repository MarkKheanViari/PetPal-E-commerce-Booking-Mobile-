package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(private val appointments: List<Appointment>) :
    RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serviceName: TextView = view.findViewById(R.id.serviceName)
        val appointmentDate: TextView = view.findViewById(R.id.appointmentDate) // ✅ Fix reference
        val price: TextView = view.findViewById(R.id.price)
        val status: TextView = view.findViewById(R.id.status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.appointment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.serviceName.text = appointment.serviceName
        holder.appointmentDate.text = appointment.appointmentDate // ✅ Fix reference
        holder.price.text = "₱${appointment.price}"
        holder.status.text = appointment.status
    }

    override fun getItemCount(): Int = appointments.size
}
