package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.transition.TransitionManager

class FaqActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        // Set up the Toolbar as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "FAQ"

        // Get the container holding the FAQ items
        val faqContainer = findViewById<ViewGroup>(R.id.faq_container)

        // Setup toggles for each FAQ item
        setupToggle(faqContainer, R.id.faq_question1, R.id.faq_answer1)
        setupToggle(faqContainer, R.id.faq_question2, R.id.faq_answer2)
        setupToggle(faqContainer, R.id.faq_question3, R.id.faq_answer3)
    }

    /**
     * Helper function to add a smooth expand/collapse transition to a FAQ item.
     */
    private fun setupToggle(container: ViewGroup, questionId: Int, answerId: Int) {
        val question: TextView = findViewById(questionId)
        val answer: TextView = findViewById(answerId)

        question.setOnClickListener {
            // Animate the transition within the container
            TransitionManager.beginDelayedTransition(container)
            answer.visibility = if (answer.visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    // Handle the Up button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()  // Close the activity and return
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
