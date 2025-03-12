package com.example.myapplication

data class OrderItem(
    val productId: Int,
    val name: String,
    val imageUrl: String,
    val description: String,
    val quantity: Int,
    val price: String
)
