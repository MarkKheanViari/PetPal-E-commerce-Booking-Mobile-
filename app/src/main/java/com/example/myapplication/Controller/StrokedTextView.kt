// StrokedTextView.kt
package com.yourpackage  // Replace with your actual package name

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.example.myapplication.R

class StrokedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f  // Adjust this for stroke thickness
        color = ContextCompat.getColor(context, R.color.darker_orange)  // Stroke color
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the stroke
        strokePaint.textSize = textSize
        strokePaint.typeface = typeface
        val text = text.toString()
        val x = paddingLeft.toFloat()
        val y = baseline.toFloat()
        canvas.drawText(text, x, y, strokePaint)
        // Draw the filled text on top
        super.onDraw(canvas)
    }
}