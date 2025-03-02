package com.example.petpal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.GroomingActivity
import com.example.myapplication.VeterinaryActivity

class ServiceFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.services_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groomingCard = view.findViewById<CardView>(R.id.cardGrooming)
        val vetCard = view.findViewById<CardView>(R.id.cardVet)

        groomingCard.setOnClickListener {
            val intent = Intent(requireContext(), GroomingActivity::class.java)
            startActivity(intent)
        }

        vetCard.setOnClickListener {
            val intent = Intent(requireContext(), VeterinaryActivity::class.java)
            startActivity(intent)
        }
    }
}
