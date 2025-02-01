import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import com.example.myapplication.R

class ProductAdapter(private val context: Context, private val products: List<Product>) : BaseAdapter() {
    override fun getCount(): Int = products.size
    override fun getItem(position: Int): Product = products[position]
    override fun getItemId(position: Int): Long = products[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)

        val product = getItem(position)
        view.findViewById<TextView>(R.id.productName).text = product.name
        view.findViewById<TextView>(R.id.productPrice).text = "₱${product.price}"
        view.findViewById<TextView>(R.id.productDescription).text = product.description
        view.findViewById<TextView>(R.id.productQuantity).text = "Stock: ${product.quantity}"

        val productImage = view.findViewById<ImageView>(R.id.productImage)
        Glide.with(context).load(product.imageUrl).into(productImage)

        return view
    }
}
