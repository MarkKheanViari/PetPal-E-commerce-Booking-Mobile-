package com.example.myapplication

import android.os.Bundle
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar  // if using the activity-level progress bar
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        progressBar = findViewById(R.id.introProgressBarActivity)
        progressBar.max = 6 // total number of pages

        adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter

        // Listen for page changes to update the progress bar.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Update progress; positions are 0-based, so add 1.
                progressBar.progress = position + 1
            }
        })
    }
}
