package com.example.myapplication.Controller

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.OrderHistoryActivity
import com.example.myapplication.ProfileActivity
import com.example.myapplication.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // Back button: navigate back
        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener { finish() }

        // Profile Section: clicking header launches ProfileActivity
        val headerView = findViewById<TextView>(R.id.user_name) // or the appropriate header element
        headerView.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        // Order History click: navigate to OrderHistoryActivity
        val orderHistory = findViewById<TextView>(R.id.order_history)
        orderHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        // Shipping History click: navigate to ShippingHistoryActivity
        //val shippingHistory = findViewById<TextView>(R.id.shipping_history)
        //shippingHistory.setOnClickListener {
            //startActivity(Intent(this, ShippingHistoryActivity::class.java))
        //}

        // Other settings items (e.g., Help, About) can have their own click listeners here
    }
}
