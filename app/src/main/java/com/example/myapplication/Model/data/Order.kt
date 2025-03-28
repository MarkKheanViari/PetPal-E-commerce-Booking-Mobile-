package com.example.myapplication

data class Order(
    val id: Int,
    val totalPrice: String,
    val paymentMethod: String,
    val status: String,
    val createdAt: String,
    val items: List<OrderItem> = emptyList(), // Default to an empty list
    val userName: String = "",    // New field for the user's name
    val address: String = "",     // New field for the user's address
    val phoneNumber: String = ""  // New field for the user's phone number
)
