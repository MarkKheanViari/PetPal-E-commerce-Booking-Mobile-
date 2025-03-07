package com.example.myapplication

object LikedProductsStore {
    val likedProducts: MutableList<Product> = mutableListOf()

    fun addProduct(product: Product) {
        // Optionally, check for duplicates before adding.
        if (!likedProducts.contains(product)) {
            likedProducts.add(product)
        }
    }
}
