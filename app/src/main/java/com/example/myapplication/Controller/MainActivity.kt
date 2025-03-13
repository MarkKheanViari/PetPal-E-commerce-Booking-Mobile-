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
import android.view.animation.AnimationUtils
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
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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
        setContentView(R.layout.activity_main)

        // Initialize bottomNavigation
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_products

        // Set initial UI
        showMainUI(true)
        findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"

        // Search bar logic
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            bottomNavigation.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        rootLayout = findViewById(R.id.rootLayout)
        checkUserAndResetIfNeeded()

        // Drawer layout & menu icon
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

        // Click on header -> load ProfileFragment
        headerView.setOnClickListener {
            showMainUI(false)
            loadFragment(ProfileFragment())
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // NavigationView item selections
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    showMainUI(false)
                    loadFragment(ProfileFragment())
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
                    showMainUI(false)
                    loadFragment(ServiceFragment())
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            true
        }

        // Cart button in the toolbar
        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Setup products RecyclerView and adapter
        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(this, displayedProducts) { product ->
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Clear focus when tapping outside the search bar
        rootLayout.setOnClickListener {
            if (searchBar.hasFocus()) {
                hideKeyboardAndClearFocus(searchBar)
            }
        }

        // Fetch initial products
        fetchProducts()

        // Listen for back stack changes
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (currentFragment) {
                is ServiceFragment -> bottomNavigation.selectedItemId = R.id.menu_service
                is CartFragment -> bottomNavigation.selectedItemId = R.id.nav_cart
                is ProfileFragment -> bottomNavigation.selectedItemId = R.id.nav_profile
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
            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            } else {
                // When coming back from a fragment, reset to "Catalog"
                bottomNavigation.selectedItemId = R.id.nav_products
                showMainUI(true)
                findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"
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

    /**
     * Replaces the fragment_container with the given fragment.
     */
    private fun loadFragment(fragment: Fragment) {
        val fragmentTag = fragment.javaClass.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment != null && existingFragment.isAdded) {
            // Already added
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, fragmentTag)
            .addToBackStack(fragmentTag)
            .commit()
    }

    private fun checkUserAndResetIfNeeded() {
        val userId = sharedPreferences.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            currentUserId = userId
        }
    }

    private fun setupCategoryButtons() {
        val categories = listOf(
            Pair(R.id.allButton, "all"),
            Pair(R.id.foodButton, "Food"),
            Pair(R.id.treatsButton, "Treats"),
            Pair(R.id.essentialsButton, "Essentials"),
            Pair(R.id.suppliesButton, "Supplies"),
            Pair(R.id.accessoriesButton, "Accessories"),
            Pair(R.id.groomingButton, "Grooming"),
            Pair(R.id.hygieneButton, "Hygiene"),
            Pair(R.id.toysButton, "Toys"),
            Pair(R.id.enrichmentButton, "Enrichment"),
            Pair(R.id.healthcareButton, "Healthcare"),
            Pair(R.id.trainingButton, "Training")
        )

        categories.forEach { (buttonId, category) ->
            val button = findViewById<Button>(buttonId)
            button.setOnClickListener {
                // 1) Pop any active fragments from the back stack
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                // 2) Show the main UI again
                showMainUI(true)

                // 3) Reset bottom nav to 'Products' tab
                bottomNavigation.selectedItemId = R.id.nav_products

                // 4) Fetch products by this category
                fetchProductsByCategory(category)

                // 5) Update button styles
                updateButtonStyles(
                    button,
                    *categories.map { findViewById<Button>(it.first) }.toTypedArray()
                )
            }
        }
    }

    private fun checkForApprovedAppointments(userId: Int) {
        val url = "http://192.168.1.15/backend/fetch_approved_appointments.php?mobile_user_id=$userId"
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
            if (button != selectedButton) {
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.light_smth))
                button.setTextColor(Color.BLACK)
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            // Attempt to find the view corresponding to the selected item
            // (This may vary depending on your BottomNavigationView implementation)
            val viewToAnimate = bottomNavigation.findViewById<View>(item.itemId)
            viewToAnimate?.let { view ->
                val bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce)
                view.startAnimation(bounceAnimation)
            }

            when (item.itemId) {
                R.id.nav_products -> {
                    // Pop any active fragments and show the catalog
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    showMainUI(true)
                    findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"
                    fetchProducts()
                    true
                }
                R.id.menu_service -> {
                    showMainUI(false)
                    loadFragment(ServiceFragment())
                    true
                }
                R.id.nav_cart -> {
                    showMainUI(false)
                    loadFragment(CartFragment())
                    true
                }
                R.id.nav_profile -> {
                    showMainUI(false)
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }


    /**
     * Toggles the visibility of the search bar, browse text,
     * category buttons, recommended text, and products RecyclerView.
     */
    private fun showMainUI(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE

        findViewById<EditText>(R.id.search_bar).visibility = visibility
        findViewById<TextView>(R.id.browseText).visibility = visibility
        findViewById<View>(R.id.categoryScrollView).visibility = visibility
        findViewById<TextView>(R.id.recommendTxt).visibility = visibility
        findViewById<View>(R.id.productsRecyclerView).visibility = visibility
    }

    private fun fetchProducts() {
        val url = "http://192.168.1.15/backend/fetch_product.php"
        val request = Request.Builder().url(url).build()

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
                    if (!json.optBoolean("success", false)) return

                    val productsArray = json.optJSONArray("products") ?: JSONArray()
                    val fetchedProducts = mutableListOf<Product>()
                    val baseImageUrl = "http://192.168.1.15/backend/uploads/"

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        val fetchedPrice = productJson.optDouble("price", 0.0)
                        val rawImage = productJson.optString("image", "").trim()
                        val fullImageUrl = if (rawImage.isNotEmpty() && !rawImage.startsWith("http")) {
                            baseImageUrl + rawImage
                        } else {
                            rawImage
                        }
                        val finalImageUrl = if (fullImageUrl.isNotEmpty()) fullImageUrl else "${baseImageUrl}default.jpg"

                        fetchedProducts.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = finalImageUrl
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

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchProductsByCategory(category: String) {
        // Show loading spinner
        val progressBar: ProgressBar = findViewById(R.id.progressBar) // Reference to your progress bar
        progressBar.visibility = View.VISIBLE

        // Clear any previous products
        displayedProducts.clear()
        productAdapter.notifyDataSetChanged()

        // Fetch products by category
        val url = "http://192.168.1.15/backend/fetch_product.php?category=$category"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()

                // Check if no products are available
                if (responseData.isNullOrEmpty() || responseData.contains("\"products\":[]")) {
                    runOnUiThread {
                        // Show the "No products available" message and hide the RecyclerView
                        findViewById<RecyclerView>(R.id.productsRecyclerView).visibility = View.GONE
                        findViewById<TextView>(R.id.noProductsTextView).visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                    return
                }

                try {
                    val jsonResponse = JSONObject(responseData)
                    if (!jsonResponse.optBoolean("success", false)) return

                    val categoryProducts = mutableListOf<Product>()
                    val productsArray = jsonResponse.optJSONArray("products") ?: JSONArray()

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        val rawImage = productJson.optString("image", "").trim()
                        val fullImageUrl = if (rawImage.isNotEmpty() && !rawImage.startsWith("http")) {
                            "http://192.168.1.15/backend/uploads/$rawImage"
                        } else {
                            rawImage
                        }
                        val finalImageUrl = if (fullImageUrl.isNotEmpty()) fullImageUrl else "http://192.168.1.15/backend/uploads/default.jpg"

                        categoryProducts.add(
                            Product(
                                id = productJson.getInt("id"),
                                name = productJson.getString("name"),
                                price = productJson.getString("price"),
                                description = productJson.getString("description"),
                                quantity = productJson.getInt("quantity"),
                                imageUrl = finalImageUrl
                            )
                        )
                    }

                    runOnUiThread {
                        // Hide progress bar once data is loaded
                        progressBar.visibility = View.GONE

                        if (categoryProducts.isNotEmpty()) {
                            displayedProducts.clear()
                            displayedProducts.addAll(categoryProducts)
                            productAdapter.updateProducts(ArrayList(displayedProducts))

                            // Show the RecyclerView with products
                            findViewById<RecyclerView>(R.id.productsRecyclerView).visibility = View.VISIBLE
                            findViewById<TextView>(R.id.noProductsTextView).visibility = View.GONE
                        } else {
                            // Show no products message
                            Toast.makeText(this@MainActivity, "No products available for this category", Toast.LENGTH_SHORT).show()
                            findViewById<RecyclerView>(R.id.productsRecyclerView).visibility = View.GONE
                            findViewById<TextView>(R.id.noProductsTextView).visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Error parsing data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Function to disable category buttons
    private fun disableCategoryButtons(disable: Boolean) {
        val buttons = listOf(
            R.id.allButton, R.id.foodButton, R.id.treatsButton, R.id.essentialsButton,
            R.id.suppliesButton, R.id.accessoriesButton, R.id.groomingButton, R.id.hygieneButton,
            R.id.toysButton, R.id.enrichmentButton, R.id.healthcareButton, R.id.trainingButton
        )
        buttons.forEach { buttonId ->
            val button = findViewById<Button>(buttonId)
            button.isEnabled = !disable
        }
    }

    // Function to enable category buttons again
    private fun enableCategoryButtons() {
        disableCategoryButtons(false)
    }


    private fun doLogout() {
        val sharedPrefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
