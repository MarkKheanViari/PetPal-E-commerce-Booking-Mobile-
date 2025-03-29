package com.example.myapplication

data class Order(
    val id: Int,
    val totalPrice: String,
    val paymentMethod: String,
    var status: String,
    val createdAt: String,
    val items: List<OrderItem>,
    val userName: String,
    val address: String,
    val phoneNumber: String
)
