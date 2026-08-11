package com.example.odysseyglyph

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

fun Snackbar.applyNothingStyle(): Snackbar {
    val layout = this.view as Snackbar.SnackbarLayout
    val defaultText = layout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.text?.toString() ?: ""
    
    // Clear default styling and views
    layout.removeAllViews()
    layout.setBackgroundColor(Color.TRANSPARENT)
    layout.setPadding(0, 0, 0, 0)
    layout.elevation = 0f
    
    // Inflate custom Nothing OS layout
    val customView = LayoutInflater.from(layout.context).inflate(R.layout.nothing_snackbar, layout, false)
    val tv = customView.findViewById<TextView>(R.id.tvSnackbarText)
    tv.text = defaultText
    
    // Add to Snackbar layout
    layout.addView(customView)
    
    return this
}
