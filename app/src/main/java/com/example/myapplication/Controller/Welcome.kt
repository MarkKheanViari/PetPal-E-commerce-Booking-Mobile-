package com.example.myapplication.Controller

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.LoginActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.RegisterActivity

class Welcome : AppCompatActivity() {

    private lateinit var loginBtn : ImageView
    private lateinit var signUpBtn : ImageView
    private lateinit var skipBtn : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginBtn = findViewById(R.id.loginBtn)
        signUpBtn = findViewById(R.id.signUpBtn)
        skipBtn = findViewById(R.id.skipBtn)

        skipBtn.paintFlags = skipBtn.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        skipBtn.setTextColor(getColor(R.color.other_gray))

        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        skipBtn.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}