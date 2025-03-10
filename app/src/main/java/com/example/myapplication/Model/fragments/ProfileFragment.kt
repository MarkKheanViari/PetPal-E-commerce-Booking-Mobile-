package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.ProfileActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment_profile layout (create fragment_profile.xml accordingly)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Click listener for "Scheduled Services"
        val scheduledServices = view.findViewById<TextView>(R.id.scheduled_services)
        scheduledServices?.setOnClickListener {
            val intent = Intent(requireContext(), ScheduledServicesActivity::class.java)
            startActivity(intent)
        }

        // Click listener for "Order Details"
        val orderDetails = view.findViewById<TextView>(R.id.order_details)
        orderDetails?.setOnClickListener {
            val intent = Intent(requireContext(), OrderDetailsActivity::class.java)
            startActivity(intent)
        }

        // **New**: Click listener for "Profile Details" (or any button you want to lead to the profile)
        val profileDetails = view.findViewById<TextView>(R.id.profile_details)
        profileDetails?.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
