package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.Model.fragments.IntroFragment
import com.example.myapplication.R

class OnboardingAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    // Define the list of pages (6 in this example).
    private val pages = listOf(
        // Page 1: "Welcome to Petpal!" (intro1.xml)
        IntroFragment.newInstance(R.layout.fragment_intro, isLast = false),
        // Page 2: "Shop for your Pets" (intro2.xml)
        IntroFragment.newInstance(R.layout.fragment_intro2, isLast = false),
        // Page 3: "Book Pet Services" (intro3.xml)
        IntroFragment.newInstance(R.layout.fragment_intro3, isLast = false),
        // Page 4: "Tailored Just for You" (intro4.xml)
        IntroFragment.newInstance(R.layout.fragment_intro4, isLast = false),
        // Page 5: "Everything your Pet Needs in One App" (intro5.xml)
        IntroFragment.newInstance(R.layout.fragment_intro5, isLast = false),
        // Page 6: "Adopt. Care. Love." with Get Started button and progress bar (fragment_intro.xml)
        IntroFragment.newInstance(R.layout.fragment_intro6, isLast = true)
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment = pages[position]
}
