import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapplication.R

class CartAdapter(private val context: Context, private var cartItems: MutableList<CartItem>) : BaseAdapter() {

    override fun getCount(): Int = cartItems.size
    override fun getItem(position: Int): CartItem = cartItems[position]
    override fun getItemId(position: Int): Long = cartItems[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false)
        val cartItem = getItem(position)

        val itemName: TextView = view.findViewById(R.id.cartItemName)
        val itemPrice: TextView = view.findViewById(R.id.cartItemPrice)
        val itemQuantity: TextView = view.findViewById(R.id.cartItemQuantity)
        val itemImage: ImageView = view.findViewById(R.id.cartItemImage)

        itemName.text = cartItem.name
        itemPrice.text = "₱${cartItem.price}"
        itemQuantity.text = "Qty: ${cartItem.quantity}"
        Glide.with(context).load(cartItem.imageUrl).into(itemImage)

        return view
    }
}
