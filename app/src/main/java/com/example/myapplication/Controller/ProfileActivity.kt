package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etGender: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etSocial: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_details)

        // Initialize back button and set click listener
        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Close the activity and go back
        }

        // Initialize profile detail views
        imgProfile = findViewById(R.id.imgProfile)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etGender = findViewById(R.id.etGender)
        etBirthday = findViewById(R.id.etBirthday)
        etSocial = findViewById(R.id.etSocial)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // Save button click listener to update the profile on the database
        btnSaveProfile.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val gender = etGender.text.toString().trim()
            val birthday = etBirthday.text.toString().trim()
            val social = etSocial.text.toString().trim()

            updateUserProfile(username, email, phone, gender, birthday, social)
        }
    }

    private fun updateUserProfile(
        username: String,
        email: String,
        phone: String,
        gender: String,
        birthday: String,
        social: String
    ) {
        // Replace the URL with your actual server URL
        val url = "https://192.168.1.15/backend/update_user_info.php"

        val requestQueue = Volley.newRequestQueue(this)
        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                // Handle your server response here.
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                // Add the fields that you want to update.
                params["username"] = username
                params["email"] = email
                params["phone"] = phone
                params["gender"] = gender
                params["birthday"] = birthday
                params["social"] = social

                return params
            }
        }
        requestQueue.add(stringRequest)
    }
}
