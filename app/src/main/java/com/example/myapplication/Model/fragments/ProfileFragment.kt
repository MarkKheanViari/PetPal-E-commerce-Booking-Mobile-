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
        // Inflate the fragment_profile layout which contains your UI elements
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

        // Set up click listeners for additional functions
        view.findViewById<TextView>(R.id.scheduled_services).setOnClickListener {
            startActivity(Intent(requireContext(), ScheduledServicesActivity::class.java))
        }
        view.findViewById<TextView>(R.id.order_details).setOnClickListener {
            startActivity(Intent(requireContext(), OrderDetailsActivity::class.java))
        }
        view.findViewById<TextView>(R.id.help).setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        view.findViewById<TextView>(R.id.about).setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        view.findViewById<TextView>(R.id.liked_products).setOnClickListener {
            startActivity(Intent(requireContext(),LikedProductsActivity::class.java))
        }
        view.findViewById<TextView>(R.id.profile_details).setOnClickListener {
            startActivity(Intent(requireContext(),ProfileActivity::class.java))
        }


    }
}
