package com.example.myapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: OnboardingAdapter
    private lateinit var segments: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        segments = arrayOf(
            findViewById(R.id.segment1),
            findViewById(R.id.segment2),
            findViewById(R.id.segment3),
            findViewById(R.id.segment4),
            findViewById(R.id.segment5),
            findViewById(R.id.segment6)
        )

        adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter

        // Initialize the first segment as active
        updateSegments(0)

        // Listen for page changes to update the segments
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateSegments(position)
            }
        })
    }

    private fun updateSegments(currentPosition: Int) {
        for (i in segments.indices) {
            segments[i].backgroundTintList = ContextCompat.getColorStateList(
                this,
                if (i <= currentPosition) R.color.segment_filled else R.color.segment_unfilled
            )
        }
    }
}