package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.transition.TransitionManager

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set up the Toolbar as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"

        // Get the container holding the about content
        val aboutContainer = findViewById<ViewGroup>(R.id.about_container)

        // Set up toggles for the About Us and Our Team sections
        setupToggle(aboutContainer, R.id.about_title, R.id.about_content)
        setupToggle(aboutContainer, R.id.team_title, R.id.team_content)
    }

    /**
     * Helper function to add a smooth expand/collapse transition to a section.
     */
    private fun setupToggle(container: ViewGroup, titleId: Int, contentId: Int) {
        val title: TextView = findViewById(titleId)
        val content: TextView = findViewById(contentId)

        title.setOnClickListener {
            // Animate the transition within the container for a smooth effect
            TransitionManager.beginDelayedTransition(container)
            content.visibility = if (content.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    // Handle the Up button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // Close the activity and return to the previous screen
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
