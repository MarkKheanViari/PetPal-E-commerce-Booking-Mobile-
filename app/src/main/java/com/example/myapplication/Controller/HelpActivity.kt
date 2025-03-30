package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.transition.TransitionManager

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        // Set up the Toolbar as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help"

        // Get the container holding the help items
        val helpContainer = findViewById<ViewGroup>(R.id.help_container)

        // Setup toggles for each help item
        setupToggle(helpContainer, R.id.help_question1, R.id.help_answer1)
        setupToggle(helpContainer, R.id.help_question2, R.id.help_answer2)
        setupToggle(helpContainer, R.id.help_question3, R.id.help_answer3)
    }

    /**
     * Helper function to add a smooth expand/collapse transition to a help item.
     */
    private fun setupToggle(container: ViewGroup, questionId: Int, answerId: Int) {
        val question: TextView = findViewById(questionId)
        val answer: TextView = findViewById(answerId)

        question.setOnClickListener {
            // Animate the transition within the container for a smooth effect
            TransitionManager.beginDelayedTransition(container)
            answer.visibility = if (answer.visibility == View.GONE) View.VISIBLE else View.GONE
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
