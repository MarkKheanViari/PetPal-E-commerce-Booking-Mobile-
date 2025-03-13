package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment_profile layout which now contains help and about buttons
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the username and email from SharedPreferences
        val sharedPrefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedUserName = sharedPrefs.getString("username", "Default Name")
        val savedUserEmail = sharedPrefs.getString("user_email", "user@example.com")

        // Set the retrieved data to the TextViews
        val userNameTextView = view.findViewById<TextView>(R.id.user_name)
        val userEmailTextView = view.findViewById<TextView>(R.id.user_email)
        userNameTextView.text = savedUserName
        userEmailTextView.text = savedUserEmail

        // Existing click listeners for other functions
        val scheduledServices = view.findViewById<TextView>(R.id.scheduled_services)
        scheduledServices.setOnClickListener {
            startActivity(Intent(requireContext(), ScheduledServicesActivity::class.java))
        }

        val orderDetails = view.findViewById<TextView>(R.id.order_details)
        orderDetails.setOnClickListener {
            startActivity(Intent(requireContext(), OrderDetailsActivity::class.java))
        }

        // New click listener for Help
        val helpButton = view.findViewById<TextView>(R.id.help)
        helpButton.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        // New click listener for About Us
        val aboutButton = view.findViewById<TextView>(R.id.about)
        aboutButton.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }
}
