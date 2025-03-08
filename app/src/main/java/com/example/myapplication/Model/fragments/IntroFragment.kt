package com.example.myapplication.Model.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.RegisterActivity

class IntroFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_RES = "layoutRes"
        private const val ARG_IS_LAST = "isLast"

        /**
         * Create a new instance of IntroFragment, specifying which layout to inflate
         * and whether this is the last intro screen.
         */
        fun newInstance(layoutRes: Int, isLast: Boolean = false): IntroFragment {
            val fragment = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES, layoutRes)
            args.putBoolean(ARG_IS_LAST, isLast)
            fragment.arguments = args
            return fragment
        }
    }

    private var introProgressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout specified by the arguments; default to fragment_intro.xml if none provided.
        val layoutRes = arguments?.getInt(ARG_LAYOUT_RES) ?: R.layout.fragment_intro
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // (Optional) If the layout contains a progress bar, keep a reference.
        introProgressBar = view.findViewById(R.id.introProgressBar)

        val isLast = arguments?.getBoolean(ARG_IS_LAST, false) ?: false
        if (isLast) {
            val getStartedButton = view.findViewById<Button>(R.id.getStartedButton)
            getStartedButton.setOnClickListener {
                // Set that user has finished seeing the intro

                val sharedPrefs = requireActivity().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("hasSeenIntro", true).apply()

                // Navigate to RegisterActivity
                startActivity(Intent(requireContext(), RegisterActivity::class.java))
                requireActivity().finish() // Close onboarding so user can't go back
            }
        }

    }

    /**
     * Optionally update the progress bar.
     * (This works if your layout includes a progress bar, e.g. in fragment_intro.xml.)
     * For a total of 6 pages, currentPage should be in 1..6.
     */
    fun updateProgress(currentPage: Int) {
        introProgressBar?.progress = currentPage
    }
}
