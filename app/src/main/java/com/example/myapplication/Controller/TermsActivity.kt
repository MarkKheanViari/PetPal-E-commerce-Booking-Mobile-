package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load the layout for Terms and Conditions
        setContentView(R.layout.terms_and_condition)
    }
}
