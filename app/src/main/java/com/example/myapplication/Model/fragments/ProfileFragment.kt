package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

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

        // Example: click listener for "Scheduled Services"
        val scheduledServices = view.findViewById<TextView>(R.id.scheduled_services)
        scheduledServices?.setOnClickListener {
            // If you need to open an activity from here:
            val intent = Intent(requireContext(), ScheduledServicesActivity::class.java)
            startActivity(intent)
        }
    }
}
