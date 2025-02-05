import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.myapplication.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ProductAdapter(private val context: Context, private var products: MutableList<Product>) :
    android.widget.BaseAdapter() {

    override fun getCount(): Int = products.size
    override fun getItem(position: Int): Product = products[position]
    override fun getItemId(position: Int): Long = products[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
        val product = getItem(position)

        val itemName: TextView = view.findViewById(R.id.productName)
        val itemPrice: TextView = view.findViewById(R.id.productPrice)
        val itemStock: TextView = view.findViewById(R.id.productStock)
        val itemImage: ImageView = view.findViewById(R.id.productImage)
        val addToCartButton: Button = view.findViewById(R.id.addToCartButton)

        itemName.text = product.name
        itemPrice.text = "₱${product.price}"
        itemStock.text = "Stock: ${product.quantity}"
        Glide.with(context).load(product.imageUrl).into(itemImage)

        if (product.quantity > 0) {
            addToCartButton.isEnabled = true
            addToCartButton.text = "Add to Cart"
            addToCartButton.setBackgroundColor(Color.parseColor("#FF9800")) // Orange color

            addToCartButton.setOnClickListener {
                addToCart(product)
            }
        } else {
            addToCartButton.isEnabled = false
            addToCartButton.text = "Out of Stock"
            addToCartButton.setBackgroundColor(Color.GRAY)
        }

        return view
    }

    private fun addToCart(product: Product) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val mobileUserId = sharedPreferences.getInt("user_id", -1)

        if (mobileUserId == -1) {
            Toast.makeText(context, "❌ Please login to add to cart", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", product.id)
            put("quantity", 1) // Default to 1
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/add_to_cart.php")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AddToCart", "❌ Network request failed: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                Handler(Looper.getMainLooper()).post {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val success = jsonResponse.optBoolean("success", false)

                        if (success) {
                            Toast.makeText(context, "✅ Added to Cart!", Toast.LENGTH_SHORT).show()
                            product.quantity -= 1
                            notifyDataSetChanged() // Refresh UI dynamically
                        } else {
                            Toast.makeText(context, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    // ✅ Single `updateProducts()` function (No more duplicate conflicts)
    fun updateProducts(newProducts: List<Product>) {
        products.clear() // ✅ Clear old data
        products.addAll(newProducts) // ✅ Add new data
        notifyDataSetChanged() // ✅ Refresh UI
    }



    // ✅ Allow `MainActivity.kt` to get the list of products
    fun getProducts(): List<Product> {
        return products
    }
}
