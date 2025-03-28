package com.example.myapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReportReasonAdapter(
    private val reasons: List<String>,
    private val onReasonSelected: (String?) -> Unit
) : RecyclerView.Adapter<ReportReasonAdapter.ReasonViewHolder>() {

    private var selectedPosition: Int = -1  // Track the selected position, -1 means nothing is selected

    inner class ReasonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.reasonRadioButton)
        val reasonTextView: TextView = itemView.findViewById(R.id.reasonTextView)

        fun bind(reason: String, position: Int) {
            reasonTextView.text = reason.split(" – ")[0]  // Display only the short reason
            radioButton.isChecked = position == selectedPosition
            Log.d("ReportReasonAdapter", "Binding position $position, selectedPosition: $selectedPosition, isChecked: ${radioButton.isChecked}")

            // Click listener for the RadioButton
            radioButton.setOnClickListener {
                Log.d("ReportReasonAdapter", "RadioButton clicked at position: $position, current selectedPosition: $selectedPosition")
                // Apply scale and fade animation
                applyClickAnimation(radioButton, itemView) {
                    handleSelection(position)
                }
            }

            // Fallback: Click listener for the entire itemView
            itemView.setOnClickListener {
                Log.d("ReportReasonAdapter", "ItemView clicked at position: $position, current selectedPosition: $selectedPosition")
                // Apply scale and fade animation
                applyClickAnimation(radioButton, itemView) {
                    handleSelection(position)
                }
            }
        }

        private fun handleSelection(position: Int) {
            val previousPosition = selectedPosition

            // Toggle selection: if the clicked item is already selected, deselect it
            if (selectedPosition == position) {
                selectedPosition = -1  // Deselect
                onReasonSelected(null)  // Notify that no reason is selected
                Log.d("ReportReasonAdapter", "Deselected, new selectedPosition: $selectedPosition")
            } else {
                selectedPosition = position
                onReasonSelected(reasons[position])  // Notify the selected reason
                Log.d("ReportReasonAdapter", "Selected position: $position, new selectedPosition: $selectedPosition")
            }

            // Update the UI
            if (previousPosition != -1 && previousPosition != selectedPosition) {
                notifyItemChanged(previousPosition)  // Update previous item
                Log.d("ReportReasonAdapter", "Notified previous position: $previousPosition")
            }
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition)  // Update current item
                Log.d("ReportReasonAdapter", "Notified current position: $selectedPosition")
            }
        }

        private fun applyClickAnimation(radioButton: RadioButton, itemView: View, onEnd: () -> Unit) {
            // Scale down animation
            radioButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .start()

            itemView.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .alpha(0.7f)  // Subtle fade effect
                .setDuration(100)
                .withEndAction {
                    // Scale back up animation
                    radioButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()

                    itemView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)  // Restore full opacity
                        .setDuration(100)
                        .withEndAction {
                            onEnd()  // Proceed with selection logic after animation
                        }
                        .start()
                }
                .start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReasonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_reason, parent, false)
        return ReasonViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReasonViewHolder, position: Int) {
        holder.bind(reasons[position], position)
    }

    override fun getItemCount(): Int = reasons.size
}