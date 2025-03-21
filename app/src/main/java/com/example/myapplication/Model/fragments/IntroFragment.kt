package com.example.myapplication.Model.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.Controller.WelcomeActivity
import com.example.myapplication.R

class IntroFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_RES = "layoutRes"
        private const val ARG_IS_LAST = "isLast"

        fun newInstance(layoutRes: Int, isLast: Boolean = false): IntroFragment {
            val fragment = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_LAYOUT_RES, layoutRes)
            args.putBoolean(ARG_IS_LAST, isLast)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutRes = arguments?.getInt(ARG_LAYOUT_RES) ?: R.layout.fragment_intro
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set skip functionality on the "Press to Skip" text
        val skipText = view.findViewById<TextView>(R.id.skipText)
        skipText?.setOnClickListener {
            val sharedPrefs = requireActivity().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("hasSeenIntro", true).apply()
            startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            requireActivity().finish()
        }

        // (Optional) If on the last page, set the hidden button's click action too.
        val isLast = arguments?.getBoolean(ARG_IS_LAST, false) ?: false
        if (isLast) {
            val getStartedButton = view.findViewById<Button>(R.id.getStartedButton)
            getStartedButton?.setOnClickListener {
                val sharedPrefs = requireActivity().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("hasSeenIntro", true).apply()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
                requireActivity().finish()
            }
        }
    }
}
