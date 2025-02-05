package com.example.myapplication

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient

class CartActivity : AppCompatActivity() {
    private lateinit var cartListView: ListView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        cartListView = findViewById(R.id.cartListView)
        Toast.makeText(this, "Cart Opened!", Toast.LENGTH_SHORT).show()
    }
}
