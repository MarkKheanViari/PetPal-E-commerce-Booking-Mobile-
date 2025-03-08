package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
            // Handle the back action
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        likedProductsRecyclerView = findViewById(R.id.likedProductsRecyclerView)
        likedProductsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Use the global store's list as the data source
        likedProductAdapter = ProductAdapter(this, LikedProductsStore.likedProducts) { product ->
            // Optionally handle product clicks here.
        }
        likedProductsRecyclerView.adapter = likedProductAdapter

        // Refresh the adapter with the liked products
        likedProductAdapter.updateProducts(ArrayList(LikedProductsStore.likedProducts))
    }
}
