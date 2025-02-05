package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.myapplication.CartActivity
import com.example.myapplication.R

class CartAdapter(
    private val context: Context,
    private val cartItems: List<HashMap<String, String>>
) : BaseAdapter() {

    override fun getCount(): Int = cartItems.size
    override fun getItem(position: Int): Any = cartItems[position]
    override fun getItemId(position: Int): Long = cartItems[position]["cart_id"]!!.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.cart_item, parent, false)

        val itemName = view.findViewById<TextView>(R.id.cartItemName) // ✅ Check if ID exists in XML
        val itemPrice = view.findViewById<TextView>(R.id.cartItemPrice)
        val itemQuantity = view.findViewById<TextView>(R.id.cartItemQuantity)
        val itemImage = view.findViewById<ImageView>(R.id.cartItemImage)
        val removeButton = view.findViewById<Button>(R.id.removeItemButton)


        val item = cartItems[position]

        itemName.text = item["name"]
        itemPrice.text = "₱${item["price"]}"
        itemQuantity.text = "Qty: ${item["quantity"]}"

        // ✅ Load image with Glide
        Glide.with(context).load(item["image"]).into(itemImage)

        // ✅ Remove button logic
        removeButton.setOnClickListener {
            (context as CartActivity).removeItemFromCart(item["cart_id"]!!.toInt())
        }

        return view
    }
}
