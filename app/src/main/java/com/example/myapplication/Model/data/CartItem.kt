package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val productId: Int,
    val productName: String,
    val imageUrl: String, // ✅ Product Image
    val description: String, // ✅ Product Description
    val quantity: Int,
    val price: Double
) : Parcelable
