package com.example.myapplication

import android.content.Intent
import android.os.Bundle
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
                    android.util.Log.d("WebViewActivity", "Loading URL: $url")
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
                }
                return false
            }
        }
        webView.settings.javaScriptEnabled = true

        val url = intent.getStringExtra("url")
        webView.loadUrl(url ?: "https://paymongo.com")
    }
}