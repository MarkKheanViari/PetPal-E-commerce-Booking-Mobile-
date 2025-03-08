package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the layout for Privacy Policy
        setContentView(R.layout.privacy_and_policy)
    }
}
