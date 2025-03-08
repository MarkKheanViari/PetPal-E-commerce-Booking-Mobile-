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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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
    private lateinit var sharedPreferences: SharedPreferences
    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout once
        setContentView(R.layout.activity_main)

        // Initialize bottomNavigation as soon as possible
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Now you can safely access bottomNavigation in your listeners
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            bottomNavigation.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        // Ensure the window resizes when the keyboard appears
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Do not call setContentView again!

        // Get a reference to the root layout
        rootLayout = findViewById(R.id.rootLayout)
        checkUserAndResetIfNeeded()

        // Drawer layout and menu icon
        drawerLayout = findViewById(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // NavigationView header
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val headerName = headerView.findViewById<TextView>(R.id.headerName)
        val headerEmail = headerView.findViewById<TextView>(R.id.headerEmail)
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        headerName.text = sharedPrefs.getString("username", "User Name")
        headerEmail.text = sharedPrefs.getString("user_email", "user@example.com")
        headerView.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // NavigationView item selections
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_likedProducts -> {
                    startActivity(Intent(this, LikedProductsActivity::class.java))
                    true
                }
                R.id.nav_notif -> {
                    // startActivity(Intent(this, NotificationActivity::class.java))
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

        // Using the dedicated ImageView for the cart button in the toolbar
        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Setup products RecyclerView and adapter
        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(this, displayedProducts) { product ->
            // Add product to wishlist when user likes a product.
            addToWishlist(product)
        }
        productsRecyclerView.adapter = productAdapter

        setupCategoryButtons()
        setupBottomNavigation()

        // Search bar behavior
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(searchBar)
                true
            } else {
                false
            }
        }
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterProducts(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        // Clear focus when tapping outside the search bar
        rootLayout.setOnClickListener {
            if (searchBar.hasFocus()) {
                hideKeyboardAndClearFocus(searchBar)
            }
        }

        fetchProducts()

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (currentFragment) {
                is ServiceFragment -> bottomNavigation.selectedItemId = R.id.menu_service
                is CartFragment -> bottomNavigation.selectedItemId = R.id.nav_cart
                else -> bottomNavigation.selectedItemId = R.id.nav_products
            }
        }


        val mobileUserId = sharedPreferences.getInt("user_id", -1)
        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        // Fetch notifications for approved appointments
        checkForApprovedAppointments(mobileUserId)
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            bottomNavigation.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        val searchBar = findViewById<EditText>(R.id.search_bar)
        if (searchBar.hasFocus()) {
            hideKeyboardAndClearFocus(searchBar)
            searchBar.postDelayed({
                bottomNavigation.visibility = View.VISIBLE
            }, 300)
        } else {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun hideKeyboardAndClearFocus(searchBar: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
        searchBar.clearFocus()
        bottomNavigation.visibility = View.VISIBLE
    }

    private fun addToWishlist(product: Product) {
        LikedProductsStore.addProduct(product)
        Toast.makeText(this, "Added ${product.name} to Wishlist!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LikedProductsActivity::class.java))
    }

    private fun filterProducts(query: String) {
        val filteredList = allProducts.filter { product ->
            product.name.contains(query, ignoreCase = true)
        }
        displayedProducts.clear()
        displayedProducts.addAll(filteredList)
        productAdapter.updateProducts(ArrayList(displayedProducts))
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val fragmentTag = fragment.javaClass.simpleName
        val existingFragment = fragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment != null) {
            fragmentTransaction.replace(R.id.fragment_container, existingFragment, fragmentTag)
        } else {
            fragmentTransaction.replace(R.id.fragment_container, fragment, fragmentTag)
            fragmentTransaction.addToBackStack(fragmentTag)
        }
        fragmentTransaction.commit()
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

    private fun checkForApprovedAppointments(userId: Int) {
        val url = "http://192.168.1.12/backend/fetch_approved_appointments.php?mobile_user_id=$userId"
        val request = JsonObjectRequest(
            com.android.volley.Request.Method.GET, url, null,
            { response ->
                if (response.getBoolean("success")) {
                    val appointmentsArray = response.getJSONArray("appointments")
                    if (appointmentsArray.length() > 0) {
                        showNotificationDialog(appointmentsArray)
                    }
                }
            },
            { error ->
                Toast.makeText(this, "❌ Error fetching notifications: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun showNotificationDialog(appointments: JSONArray) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Appointment Update 📅")
        var message = ""
        for (i in 0 until appointments.length()) {
            val appointment = appointments.getJSONObject(i)
            val serviceName = appointment.getString("service_name")
            val appointmentDate = appointment.getString("appointment_date")
            val status = appointment.getString("status")
            if (status == "Approved") {
                message += "✅ Your appointment for $serviceName on $appointmentDate has been **APPROVED**.\n\n"
            } else if (status == "Declined") {
                message += "❌ Your appointment for $serviceName on $appointmentDate has been **DECLINED**.\n\n"
            }
        }
        if (message.isEmpty()) {
            message = "No new appointment updates."
        }
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
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
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_products -> {
                    showMainUI(true)
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"
                    true
                }
                R.id.menu_service -> {
                    showMainUI(false)
                    loadFragment(ServiceFragment())
                    findViewById<TextView>(R.id.toolbarTitle).text = "Service"
                    true
                }
                R.id.nav_cart -> {
                    // Hide the main UI
                    showMainUI(false)
                    // Then load the cart
                    loadFragment(CartFragment())
                    findViewById<TextView>(R.id.toolbarTitle).text = "Shopping Cart"
                    true
                }
                else -> false
            }
        }

    }


    private fun showMainUI(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE

        // Hide the search bar
        findViewById<EditText>(R.id.search_bar).visibility = visibility

        // Hide “Browse Through” text
        findViewById<TextView>(R.id.browseText).visibility = visibility

        // Hide the category buttons
        findViewById<View>(R.id.categoryScrollView).visibility = visibility

        // Hide “Recommended Items” text
        findViewById<TextView>(R.id.recommendTxt).visibility = visibility

        // Hide the product RecyclerView
        findViewById<View>(R.id.productsRecyclerView).visibility = visibility
    }


    private fun fetchProducts() {
        val request = Request.Builder()
            .url("http://192.168.1.12/backend/fetch_product.php")
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
                        val baseImageUrl = "http://192.168.1.12/backend/images/"
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
        val url = "http://192.168.1.12/backend/fetch_product.php?category=$category"
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
                    val baseImageUrl = "http://192.168.1.12/backend/images/"
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
