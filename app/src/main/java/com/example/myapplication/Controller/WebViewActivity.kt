package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    Log.d("WebViewActivity", "Loading URL: $url")
                    // Handle cart payment deep links
                    if (url.startsWith("petpal://payment/success")) {
                        val intent = Intent(this@WebViewActivity, CheckoutActivity::class.java).apply {
                            data = android.net.Uri.parse(url)
                        }
                        startActivity(intent)
                        finish()
                        return true
                    } else if (url.startsWith("petpal://payment/cancel")) {
                        val intent = Intent(this@WebViewActivity, CheckoutActivity::class.java).apply {
                            data = android.net.Uri.parse(url)
                        }
                        startActivity(intent)
                        finish()
                        return true
                    }
                    // Handle appointment deep links
                    if (url.startsWith("petpal://appointment/success")) {
                        val source = intent.getStringExtra("source")
                        val targetActivity = if (source == "appointment") {
                            if (intent.getStringExtra("service_type") == "Grooming") {
                                GroomingAppointmentActivity::class.java
                            } else {
                                VeterinaryAppointmentActivity::class.java
                            }
                        } else {
                            CheckoutActivity::class.java // Fallback
                        }
                        val intent = Intent(this@WebViewActivity, targetActivity).apply {
                            data = android.net.Uri.parse(url)
                        }
                        startActivity(intent)
                        finish()
                        return true
                    } else if (url.startsWith("petpal://appointment/cancel")) {
                        val source = intent.getStringExtra("source")
                        val targetActivity = if (source == "appointment") {
                            if (intent.getStringExtra("service_type") == "Grooming") {
                                GroomingAppointmentActivity::class.java
                            } else {
                                VeterinaryAppointmentActivity::class.java
                            }
                        } else {
                            CheckoutActivity::class.java // Fallback
                        }
                        val intent = Intent(this@WebViewActivity, targetActivity).apply {
                            data = android.net.Uri.parse(url)
                        }
                        startActivity(intent)
                        finish()
                        return true
                    }
                }
                return false
            }
        }
        webView.settings.javaScriptEnabled = true

        val url = intent.getStringExtra("url")
        if (url != null) {
            Log.d("WebViewActivity", "Loading initial URL: $url")
            webView.loadUrl(url)
        } else {
            Log.e("WebViewActivity", "No URL provided, closing activity")
            finish()
        }
    }
}