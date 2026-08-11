package com.example.odysseyglyph

import android.graphics.Color
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar

fun Snackbar.applyNothingStyle(): Snackbar {
    val view = this.view
    val context = view.context
    
    // Set brutalist background
    view.setBackgroundColor(Color.parseColor("#000000"))
    view.setPadding(0, 0, 0, 0)
    
    // Customize text
    val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    textView.setTextColor(Color.WHITE)
    try {
        textView.typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    } catch (e: Exception) {
        // Fallback if font fails to load
    }
    textView.textSize = 13f
    textView.isAllCaps = true
    
    // Customize action text if there's any action
    val actionView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
    actionView.setTextColor(Color.RED)
    try {
        actionView.typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    } catch (e: Exception) {
        // Fallback
    }
    actionView.textSize = 13f
    actionView.isAllCaps = true
    
    return this
}
