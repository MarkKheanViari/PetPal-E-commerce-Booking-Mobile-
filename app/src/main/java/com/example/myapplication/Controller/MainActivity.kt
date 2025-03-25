package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.Controller.WelcomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    lateinit var bottomNavigation: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rootLayout: ConstraintLayout
    private var currentUserId: Int = -1
    private lateinit var sharedPreferences: SharedPreferences
    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()
    private var searchJob: Job? = null
    private val searchScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_products

        showMainUI(true)
        findViewById<TextView>(R.id.toolbarTitle).text = "Catalog"

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        rootLayout = findViewById(R.id.rootLayout)
        checkUserAndResetIfNeeded()

        drawerLayout = findViewById(R.id.drawer_layout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val headerName = headerView.findViewById<TextView>(R.id.headerName)
        val headerEmail = headerView.findViewById<TextView>(R.id.headerEmail)
        headerName.text = sharedPreferences.getString("username", "User Name")
        headerEmail.text = sharedPreferences.getString("user_email", "user@example.com")

        headerView.setOnClickListener {
            showMainUI(false)
            loadFragment(ProfileFragment(), true)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    showMainUI(false)
                    loadFragment(ProfileFragment(), true)
                    true
                }
                R.id.nav_notif -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    true
                }
                R.id.nav_faq -> {
                    startActivity(Intent(this, FaqActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    doLogout()
                    true
                }
                R.id.menu_service -> {
                    showMainUI(false)
                    loadFragment(ServiceFragment(), true)
                    true
                }
                else -> false
            }.also {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            true
        }

        // --- Logout functionality from custom footer ---
        val footerLogout = findViewById<TextView>(R.id.footerLogout)
        footerLogout.setOnClickListener {
            doLogout()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        // --------------------------------------------------

        val viewCartButton = findViewById<ImageView>(R.id.viewCartButton)
        viewCartButton.setOnClickListener {
            val isGuest = sharedPreferences.getBoolean("isGuest", false)
            if (isGuest) {
                Toast.makeText(this, "Guest users cannot order. Please log in to access your cart.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, CartActivity::class.java))
            }
        }

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(this, displayedProducts) { product ->
            addToWishlist(product)
        }
        productsRecyclerView.adapter = productAdapter

        setupCategoryButtons()
        setupBottomNavigation()
        setupSearchBar()

        fetchProducts()

        val mobileUserId = sharedPreferences.getInt("user_id", -1)
        val isGuest = sharedPreferences.getBoolean("isGuest", false)
        if (mobileUserId == -1 && !isGuest) {
            Toast.makeText(this, "❌ User not logged in!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            currentUserId = mobileUserId
        }

        if (mobileUserId != -1) {
            checkForApprovedAppointments(mobileUserId)
        }
    }

    private fun setupSearchBar() {
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            bottomNavigation.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = searchScope.launch {
                    delay(300)
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        filterProducts(query)
                    } else {
                        displayedProducts.clear()
                        displayedProducts.addAll(allProducts)
                        productAdapter.updateProducts(ArrayList(displayedProducts))
                    }
                }
            }
        })

        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus(searchBar)
                true
            } else {
                false
            }
        }

        rootLayout.setOnClickListener {
            if (searchBar.hasFocus()) {
                hideKeyboardAndClearFocus(searchBar)
            }
        }
    }

    private fun filterProducts(query: String) {
        val filteredList = allProducts.filter { product ->
            val searchText = query.lowercase()
            val nameMatch = product.name.lowercase().contains(searchText)
            val descMatch = product.description.lowercase().contains(searchText)
            val priceMatch = product.price.lowercase().contains(searchText)
            // Simple fuzzy check with Levenshtein
            val fuzzyMatch = levenshteinDistance(product.name.lowercase(), searchText) <= 3 && searchText.length > 2
            nameMatch || descMatch || priceMatch || fuzzyMatch
        }.sortedWith(
            compareBy(
                { !it.name.lowercase().startsWith(query.lowercase()) },
                { it.name }
            )
        )

        displayedProducts.clear()
        displayedProducts.addAll(filteredList)
        productAdapter.updateProducts(ArrayList(displayedProducts), query)

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No products found for '$query'", Toast.LENGTH_SHORT).show()
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
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
                updateBottomNavigation()
            } else {
                showMainUI(true)
                bottomNavigation.selectedItemId = R.id.nav_products
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
        val isGuest = sharedPreferences.getBoolean("isGuest", false)
        if (isGuest) {
            Toast.makeText(this, "Guest users cannot order. Please log in to add products to your wishlist.", Toast.LENGTH_SHORT).show()
            return
        }

        val mobileUserId = sharedPreferences.getInt("user_id", -1)
        if (mobileUserId == -1) {
            Toast.makeText(this, "❌ Please login to add to wishlist", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonObject = JSONObject().apply {
            put("mobile_user_id", mobileUserId)
            put("product_id", product.id)
        }

        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://10.40.70.46/backend/add_to_liked_products.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    if (responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            Toast.makeText(this@MainActivity, "Added ${product.name} to Wishlist!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MainActivity, LikedProductsActivity::class.java))
                        } else {
                            Toast.makeText(this@MainActivity, "❌ Error: ${jsonResponse.optString("message")}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun loadFragment(fragment: Fragment, useContainer1: Boolean = false) {
        val containerId = if (useContainer1) R.id.fragment_container1 else R.id.fragment_container
        val fragmentTag = fragment.javaClass.simpleName
        val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (existingFragment != null && existingFragment.isAdded) return
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, fragmentTag)
            .addToBackStack(fragmentTag)
            .commit()
    }

    private fun checkUserAndResetIfNeeded() {
        val userId = sharedPreferences.getInt("user_id", -1)
        val isGuest = sharedPreferences.getBoolean("isGuest", false)
        if (userId == -1 && !isGuest) {
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

        val buttons = categories.map { (buttonId, _) -> findViewById<Button>(buttonId) }
        buttons.forEach { button ->
            if (button.id == R.id.allButton) {
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.darker_orange)
                button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_smth)
                button.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }

        categories.forEach { (buttonId, category) ->
            val button = findViewById<Button>(buttonId)
            button.setOnClickListener {
                // Reset all buttons to default
                buttons.forEach { btn ->
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_smth)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.black))
                }
                // Highlight the selected button
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.darker_orange)
                button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                showMainUI(true)
                bottomNavigation.selectedItemId = R.id.nav_products
                fetchProductsByCategory(category)
            }
        }
    }

    private fun checkForApprovedAppointments(userId: Int) {
        val url = "http://10.40.70.46/backend/fetch_approved_appointments.php?mobile_user_id=$userId"
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
        if (message.isEmpty()) message = "No new appointment updates."
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            findViewById<TextView>(R.id.toolbarTitle).text = item.title
            when (item.itemId) {
                R.id.nav_products -> {
                    showMainUI(true)
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    fetchProducts()
                    findViewById<FrameLayout>(R.id.fragment_container1).visibility = View.GONE
                    findViewById<FrameLayout>(R.id.fragment_container).visibility = View.VISIBLE
                    true
                }
                R.id.menu_service -> {
                    showMainUI(false)
                    loadFragment(ServiceFragment(), true)
                    findViewById<FrameLayout>(R.id.fragment_container1).visibility = View.VISIBLE
                    findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                    true
                }
                R.id.nav_cart -> {
                    showMainUI(false)
                    loadFragment(CartFragment(), true)
                    findViewById<FrameLayout>(R.id.fragment_container1).visibility = View.VISIBLE
                    findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                    true
                }
                R.id.nav_profile -> {
                    showMainUI(false)
                    loadFragment(ProfileFragment(), true)
                    findViewById<FrameLayout>(R.id.fragment_container1).visibility = View.VISIBLE
                    findViewById<FrameLayout>(R.id.fragment_container).visibility = View.GONE
                    true
                }
                else -> false
            }
        }
    }

    private fun updateBottomNavigation() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container1)
            ?: supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (currentFragment) {
            is ServiceFragment -> bottomNavigation.selectedItemId = R.id.menu_service
            is CartFragment -> bottomNavigation.selectedItemId = R.id.nav_cart
            is ProfileFragment -> bottomNavigation.selectedItemId = R.id.nav_profile
            else -> bottomNavigation.selectedItemId = R.id.nav_products
        }
    }

    private fun showMainUI(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.searchBar_layout).visibility = visibility
        findViewById<TextView>(R.id.browseText).visibility = visibility
        findViewById<View>(R.id.categoryScrollView).visibility = visibility
        findViewById<TextView>(R.id.recommendTxt).visibility = visibility
        findViewById<FrameLayout>(R.id.fragment_container).visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun fetchProducts() {
        val url = "http://10.40.70.46/backend/fetch_product.php"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: return
                try {
                    val json = JSONObject(responseBody)
                    if (!json.optBoolean("success", false)) return

                    val productsArray = json.optJSONArray("products") ?: JSONArray()
                    val fetchedProducts = mutableListOf<Product>()
                    val baseImageUrl = "http://10.40.70.46/backend/uploads/"

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
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
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val noProductsContainer: LinearLayout = findViewById(R.id.noProductsContainer)
        val productsRecyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)

        progressBar.visibility = View.VISIBLE
        noProductsContainer.visibility = View.GONE
        productsRecyclerView.visibility = View.GONE

        displayedProducts.clear()
        productAdapter.notifyDataSetChanged()

        val url = "http://10.40.70.46/backend/fetch_product.php?category=$category"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "❌ Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: return
                try {
                    val jsonResponse = JSONObject(responseData)
                    if (!jsonResponse.optBoolean("success", false)) return

                    val categoryProducts = mutableListOf<Product>()
                    val productsArray = jsonResponse.optJSONArray("products") ?: JSONArray()

                    for (i in 0 until productsArray.length()) {
                        val productJson = productsArray.getJSONObject(i)
                        val rawImage = productJson.optString("image", "").trim()
                        val fullImageUrl = if (rawImage.isNotEmpty() && !rawImage.startsWith("http")) {
                            "http://10.40.70.46/backend/uploads/$rawImage"
                        } else {
                            rawImage
                        }
                        val finalImageUrl = if (fullImageUrl.isNotEmpty()) fullImageUrl else "http://10.40.70.46/backend/uploads/default.jpg"

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
                        progressBar.visibility = View.GONE
                        if (categoryProducts.isNotEmpty()) {
                            displayedProducts.clear()
                            displayedProducts.addAll(categoryProducts)
                            productAdapter.updateProducts(ArrayList(displayedProducts))
                            productsRecyclerView.visibility = View.VISIBLE
                            noProductsContainer.visibility = View.GONE
                        } else {
                            productsRecyclerView.visibility = View.GONE
                            noProductsContainer.visibility = View.VISIBLE
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

    private fun doLogout() {
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchScope.cancel()
    }
}