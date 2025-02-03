import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import com.example.myapplication.R

class ProductAdapter(private val context: Context, private var productList: MutableList<Product>) : BaseAdapter() {

    override fun getCount(): Int = productList.size
    override fun getItem(position: Int): Product = productList[position]
    override fun getItemId(position: Int): Long = productList[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        val product = getItem(position)

        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val productDescription: TextView = view.findViewById(R.id.productDescription)
        val productQuantity: TextView = view.findViewById(R.id.productQuantity)
        val productImage: ImageView = view.findViewById(R.id.productImage)

        productName.text = product.name
        productPrice.text = "₱${product.price}"
        productDescription.text = product.description
        productQuantity.text = "Stock: ${product.quantity}"
        Glide.with(context).load(product.imageUrl).into(productImage)

        return view
    }

    // ✅ Add this function to update the product list dynamically
    fun updateProducts(newProductList: List<Product>) {
        productList.clear()
        productList.addAll(newProductList)
        notifyDataSetChanged() // Refresh the UI
    }
}

