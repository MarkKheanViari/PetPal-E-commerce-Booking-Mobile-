import android.content.Context
import android.content.SharedPreferences
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
        val itemStock: TextView = view.findViewById(R.id.productStock) // ✅ Ensure stock exists
        val itemImage: ImageView = view.findViewById(R.id.productImage)
        val addToCartButton: Button = view.findViewById(R.id.addToCartButton)

        itemName.text = product.name
        itemPrice.text = "₱${product.price}"
        itemStock.text = "Stock: ${product.quantity}"
        Glide.with(context).load(product.imageUrl).into(itemImage)

        addToCartButton.setOnClickListener {
            if (product.quantity > 0) {
                addToCart(product)
            } else {
                Toast.makeText(context, "Out of stock", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun addToCart(product: Product) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val customerId = sharedPreferences.getInt("user_id", -1)

        if (customerId == -1) {
            Toast.makeText(context, "Please login to add to cart", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("customer_id", customerId)
            put("product_id", product.id)
            put("quantity", 1) // Default to 1
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.65/backend/add_to_cart.php") // ✅ Check if this URL is correct
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AddToCart", "❌ Network request failed: ${e.message}")
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()

                    if (responseBody.isNullOrEmpty()) {
                        Log.e("AddToCart", "❌ Empty response from server")
                        (context as? android.app.Activity)?.runOnUiThread {
                            Toast.makeText(context, "Server returned empty response", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    Log.d("AddToCart", "✅ Server Response: $responseBody")

                    val jsonResponse = JSONObject(responseBody)
                    val success = jsonResponse.optBoolean("success", false)
                    val message = jsonResponse.optString("message", "Unknown error")

                    (context as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AddToCart", "❌ JSON Parsing Error: ${e.message}")
                    Log.e("AddToCart", "🔍 Full response: ${response.body?.string()}") // Log full response
                    (context as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(context, "Error processing server response", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }



    // ✅ Add this function to update the product list dynamically
    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}



