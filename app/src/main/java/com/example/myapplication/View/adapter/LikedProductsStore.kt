package com.example.myapplication

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object LikedProductsStore {
    val likedProducts: MutableList<Product> = mutableListOf()
    private val client = OkHttpClient()
    fun addProduct(product: Product, onSuccess: (() -> Unit)? = null) {
        if (!likedProducts.contains(product)) {
            likedProducts.add(product)

            // Build the JSON payload with product details
            val json = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("price", product.price)
                put("description", product.description)
                put("quantity", product.quantity)
                put("imageUrl", product.imageUrl)
            }

            // Create a RequestBody with the JSON
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = json.toString().toRequestBody(mediaType)

            // Replace the URL with your endpoint for handling likes
            val request = Request.Builder()
                .url("http://192.168.1.12/backend/like_product.php")
                .post(body)
                .build()

            // Execute the request asynchronously
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("LikedProductsStore", "Error posting liked product: ${e.localizedMessage}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d("LikedProductsStore", "Product liked successfully.")
                        onSuccess?.invoke()
                    } else {
                        Log.e("LikedProductsStore", "Failed to like product. Code: ${response.code}")
                    }
                }
            })
        } else {
            // If already liked, immediately invoke the callback.
            onSuccess?.invoke()
        }
    }
}
