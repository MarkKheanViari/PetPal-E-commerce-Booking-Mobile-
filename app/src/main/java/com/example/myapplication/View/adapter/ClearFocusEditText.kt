package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

class ClearFocusEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        // If the user pressed the BACK key and it's an ACTION_UP event
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            // Hide the keyboard
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(windowToken, 0)

            // Clear the EditText focus
            clearFocus()

            // Show the bottom nav again if we're in MainActivity
            // We can cast 'context' to MainActivity if the EditText is used there
            if (context is MainActivity) {
                (context as MainActivity).bottomNavigation
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
