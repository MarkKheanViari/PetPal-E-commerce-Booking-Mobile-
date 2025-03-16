package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class LikedProductsActivity : AppCompatActivity() {

    private lateinit var likedProductsRecyclerView: RecyclerView
    private lateinit var likedProductAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liked_products)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        likedProductsRecyclerView = findViewById(R.id.likedProductsRecyclerView)

        // Use a GridLayoutManager with 2 columns
        likedProductsRecyclerView.layoutManager = GridLayoutManager(this, 2)

        // Initialize your adapter with the liked products
        likedProductAdapter = ProductAdapter(
            this,
            LikedProductsStore.likedProducts
        ) { product ->
            // Handle product clicks if needed
        }

        likedProductsRecyclerView.adapter = likedProductAdapter

        // Refresh the adapter with the liked products
        likedProductAdapter.updateProducts(ArrayList(LikedProductsStore.likedProducts))
    }
}
