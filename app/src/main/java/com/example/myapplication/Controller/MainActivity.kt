package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.ServiceFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var productsRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var productAdapter: ProductAdapter
    lateinit var bottomNavigation: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rootLayout: ConstraintLayout
    private var currentUserId: Int = -1
    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure the window resizes when keyboard appears
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_main)

        // Get a reference to the root layout for tapping outside the search bar
        rootLayout = findViewById(R.id.rootLayout)

        checkUserAndResetIfNeeded()

        drawerLayout = findViewById(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // NavigationView setup using the menu XML you provided
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val headerName = headerView.findViewById<TextView>(R.id.headerName)
        val headerEmail = headerView.findViewById<TextView>(R.id.headerEmail)
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        headerName.text = sharedPrefs.getString("username", "User Name")
        headerEmail.text = sharedPrefs.getString("user_email", "user@example.com")

        // Set click listener on the header if you want the whole header to launch profile
        headerView.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    doLogout()
                    true
                }
                R.id.menu_service -> {
                    loadFragment(ServiceFragment())
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            true
        }

        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(this, displayedProducts) { product ->
            addToWishlist(product)
        }
        productsRecyclerView.adapter = productAdapter

        setupCategoryButtons()
        setupBottomNavigation()

        // Handle search bar focus and text changes
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            bottomNavigation.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(searchBar)
                true
            } else {
                false
            }
        }

        // Add TextWatcher to automatically filter products as the user types
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Filter products using the current text in the search bar
                filterProducts(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed here
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No action needed here
            }
        })

        // Optionally, tap outside to clear focus
        rootLayout.setOnClickListener {
            if (searchBar.hasFocus()) {
                hideKeyboardAndClearFocus(searchBar)
            }
        }

        fetchProducts()
    }

    // Force bottom navigation visible when the window regains focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            bottomNavigation.visibility = View.VISIBLE
        }
    }

    // Clear focus and force bottom nav visible on back press
    override fun onBackPressed() {
        val searchBar = findViewById<EditText>(R.id.search_bar)
        if (searchBar.hasFocus()) {
            hideKeyboardAndClearFocus(searchBar)
            // Post a delay to ensure keyboard is hidden and then force bottom nav visible
            searchBar.postDelayed({
                bottomNavigation.visibility = View.VISIBLE
            }, 300)
        } else {
            super.onBackPressed()
        }
    }

    private fun hideKeyboardAndClearFocus(searchBar: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
        searchBar.clearFocus()
        bottomNavigation.visibility = View.VISIBLE
    }

    private fun addToWishlist(product: Product) {
        Toast.makeText(this, "Added ${product.name} to Wishlist!", Toast.LENGTH_SHORT).show()
        // TODO: Implement logic to store the wishlist item (e.g., save to database or SharedPreferences)
    }
    // This function filters allProducts by the query and updates the adapter
    private fun filterProducts(query: String) {
        val filteredList = allProducts.filter { product ->
            product.name.contains(query, ignoreCase = true)
        }
        displayedProducts.clear()
        displayedProducts.addAll(filteredList)
        productAdapter.updateProducts(ArrayList(displayedProducts))
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkUserAndResetIfNeeded() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            currentUserId = userId
        }
    }

    private fun setupCategoryButtons() {
        val allButton: Button = findViewById(R.id.allButton)
        val catButton: Button = findViewById(R.id.catButton)
        val dogButton: Button = findViewById(R.id.dogButton)

        allButton.setOnClickListener {
            fetchProductsByCategory("all")
            updateButtonStyles(allButton, catButton, dogButton)
        }
        catButton.setOnClickListener {
            fetchProductsByCategory("cat")
            updateButtonStyles(catButton, allButton, dogButton)
        }
        dogButton.setOnClickListener {
            fetchProductsByCategory("dog")
            updateButtonStyles(dogButton, allButton, catButton)
        }
    }

    private fun updateButtonStyles(selectedButton: Button, vararg otherButtons: Button) {
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange))
        selectedButton.setTextColor(Color.WHITE)
        for (button in otherButtons) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_smth))
            button.setTextColor(Color.BLACK)
        }
    }
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products -> {
                    showMainUI(true)
                    supportFragmentManager.popBackStack()
                    // Change toolbar title back to "Catalog" when products are shown
                    findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"
                    true
                }
                R.id.menu_service -> {
                    showMainUI(false)
                    loadFragment(ServiceFragment())
                    // Update toolbar title to "Service" when service fragment is clicked
                    findViewById<TextView>(R.id.toolbarTitle).text = "Service"
                    true
                }
                else -> false
            }
        }
    }


    private fun showMainUI(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.browseText).visibility = visibility
        findViewById<View>(R.id.categoryScrollView).visibility = visibility
        findViewById<TextView>(R.id.recommendTxt).visibility = visibility
        findViewById<View>(R.id.productsRecyclerView).visibility = visibility
    }

    private fun fetchProducts() {
        val request = Request.Builder()
            .url("http://192.168.1.65/backend/fetch_product.php")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "❌ Empty response from server", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val json = JSONObject(responseBody)
                    if (json.optBoolean("success", false)) {
                        val productsArray = json.optJSONArray("products") ?: JSONArray()
                        val fetchedProducts = mutableListOf<Product>()
                        val baseImageUrl = "http://192.168.1.65/backend/images/"
                        for (i in 0 until productsArray.length()) {
                            val productJson = productsArray.getJSONObject(i)
                            val rawImage = productJson.optString("image", "")
                            val fullImageUrl = if (rawImage.startsWith("http")) rawImage else baseImageUrl + rawImage
                            fetchedProducts.add(
                                Product(
                                    id = productJson.getInt("id"),
                                    name = productJson.getString("name"),
                                    price = productJson.getString("price"),
                                    description = productJson.getString("description"),
                                    quantity = productJson.getInt("quantity"),
                                    imageUrl = fullImageUrl
                                )
                            )
                        }
                        runOnUiThread {
                            Log.d("DEBUG:MainActivity", "Fetched ${fetchedProducts.size} products")
                            allProducts.clear()
                            allProducts.addAll(fetchedProducts)
                            displayedProducts.clear()
                            displayedProducts.addAll(fetchedProducts)
                            productAdapter.updateProducts(ArrayList(displayedProducts))
                        }
                    } else {
                        Log.e("DEBUG:MainActivity", "Server returned success=false")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchProductsByCategory(category: String) {
        if (category == "all") {
            displayedProducts.clear()
            displayedProducts.addAll(allProducts)
            productAdapter.updateProducts(ArrayList(displayedProducts))
            return
        }

        val url = "http://192.168.1.65/backend/fetch_product.php?category=$category"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No products found for $category", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val jsonResponse = JSONObject(responseData)
                    if (!jsonResponse.optBoolean("success", false)) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch products for $category", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }
                    val productsArray = jsonResponse.optJSONArray("products") ?: JSONArray()
                    val baseImageUrl = "http://192.168.1.65/backend/images/"
                    val categoryProducts = mutableListOf<Product>()
                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        val rawImage = productJson.optString("image", "")
                        val fullImageUrl = if (rawImage.startsWith("http")) rawImage else baseImageUrl + rawImage
                        categoryProducts.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = fullImageUrl
                            )
                        )
                    }
                    runOnUiThread {
                        displayedProducts.clear()
                        displayedProducts.addAll(categoryProducts)
                        productAdapter.updateProducts(ArrayList(displayedProducts))
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun doLogout() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
