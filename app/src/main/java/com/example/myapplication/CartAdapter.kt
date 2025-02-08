package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<HashMap<String, String>>
) : BaseAdapter() {

    override fun getCount(): Int = cartItems.size
    override fun getItem(position: Int): Any = cartItems[position]
    override fun getItemId(position: Int): Long = cartItems[position]["cart_id"]!!.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.cart_item, parent, false)

        val itemName = view.findViewById<TextView>(R.id.cartItemName)
        val itemPrice = view.findViewById<TextView>(R.id.cartItemPrice)
        val itemQuantity = view.findViewById<TextView>(R.id.cartItemQuantity)
        val itemImage = view.findViewById<ImageView>(R.id.cartItemImage)
        val removeButton = view.findViewById<Button>(R.id.removeItemButton)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncrease) // ✅ Match ID with XML
        val btnDecrease = view.findViewById<Button>(R.id.btnDecrease) // ✅ Match ID with XML

        val item = cartItems[position]

        itemName.text = item["name"]
        itemPrice.text = "₱${item["price"]}"
        itemQuantity.text = item["quantity"]

        // ✅ Load image
        Glide.with(context).load(item["image"]).into(itemImage)

        // ✅ Remove button logic
        removeButton.setOnClickListener {
            (context as CartActivity).removeItemFromCart(item["cart_id"]!!.toInt())
        }

        // ✅ Increase quantity
        btnIncrease.setOnClickListener {
            val currentQuantity = item["quantity"]!!.toInt()
            (context as CartActivity).updateCartQuantity(item["cart_id"]!!.toInt(), currentQuantity + 1)
        }

        // ✅ Decrease quantity (prevent going below 1)
        btnDecrease.setOnClickListener {
            val currentQuantity = item["quantity"]!!.toInt()
            if (currentQuantity > 1) {
                (context as CartActivity).updateCartQuantity(item["cart_id"]!!.toInt(), currentQuantity - 1)
            }
        }

        return view
    }

}
