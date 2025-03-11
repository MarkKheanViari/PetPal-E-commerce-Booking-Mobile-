package com.example.myapplication

data class Order(
    val id: Int,
    val totalPrice: String,
    val paymentMethod: String,
    val status: String,
    val createdAt: String
)
