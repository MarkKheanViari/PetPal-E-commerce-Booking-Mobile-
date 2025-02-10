package com.example.myapplication

interface CartActionListener {
    fun updateCartQuantity(cartId: Int, newQuantity: Int)
    fun removeItemFromCart(cartId: Int)
}

