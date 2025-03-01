package com.example.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
) : Parcelable
