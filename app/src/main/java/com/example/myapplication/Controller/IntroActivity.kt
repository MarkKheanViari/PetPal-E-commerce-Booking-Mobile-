package com.example.myapplication.Controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.LoginActivity
import com.example.myapplication.R

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var btnNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        btnNext = findViewById(R.id.btnNext)

        val adapter = IntroPagerAdapter(this)
        viewPager.adapter = adapter

        // Setup the custom page indicator
        setupIndicators(adapter.itemCount)
        updateIndicators(0)

        // Handle Page Changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)

                // Set button text based on the page number
                btnNext.text = when (position) {
                    0 -> "Get Started"
                    adapter.itemCount - 1 -> "Take Me There"
                    else -> "Next"
                }
            }
        })


        // Handle Next Button Click
        btnNext.setOnClickListener {
            val nextPage = viewPager.currentItem + 1
            if (nextPage < adapter.itemCount) {
                viewPager.currentItem = nextPage
            } else {
                // Navigate to Login/Register screen
                startActivity(Intent(this, Welcome::class.java))
                finish()
            }
        }
    }

    // Create Custom Page Indicators
    private fun setupIndicators(count: Int) {
        val indicators = Array(count) { View(this) }
        val layoutParams = LinearLayout.LayoutParams(40, 8).apply {
            marginEnd = 10
        }

        for (i in indicators.indices) {
            indicators[i].layoutParams = layoutParams
            indicators[i].setBackgroundResource(R.drawable.indicator_inactive)
            indicatorLayout.addView(indicators[i])
        }
    }

    // Update Indicators Based on Page
    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val indicator = indicatorLayout.getChildAt(i)
            if (i == position) {
                indicator.setBackgroundResource(R.drawable.indicator_active) // Active (darker)
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_inactive) // Inactive (lighter)
            }
        }
    }
}

class IntroPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val layouts = listOf(
        R.layout.fragment_intro,
        R.layout.fragment_intro1,
        R.layout.fragment_intro2,
        R.layout.fragment_intro3,
        R.layout.fragment_intro4,
        R.layout.fragment_intro5
    )

    override fun getItemCount(): Int = layouts.size

    override fun createFragment(position: Int): Fragment {
        return IntroFragment.newInstance(layouts[position])
    }
}
