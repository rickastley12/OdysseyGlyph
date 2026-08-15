package com.example.odysseyglyph

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class FallbackStyleBottomSheet : BottomSheetDialogFragment() {

    private var selectedStyle = 0
    private var onStyleSelected: ((Int) -> Unit)? = null
    
    // Map of style ID to Title
    private val styles = listOf(
        Pair(0, "Music Note"),
        Pair(4, "Vinyl Record"),
        Pair(1, "Math Wave"),
        Pair(6, "Matrix Rain"),
        Pair(7, "Radar (Math)"),
        Pair(2, "Mic Wave (Mic)"),
        Pair(3, "Speaker Cone (Mic)"),
        Pair(5, "Pulse (Mic)"),
        Pair(8, "Beat Wave (Mic)")
    )
    
    private val bitmaps = mutableMapOf<Int, Bitmap>()

    fun setOnStyleSelectedListener(listener: (Int) -> Unit) {
        onStyleSelected = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fallback_style, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val prefs = requireContext().getSharedPreferences("OdysseyPrefs", android.content.Context.MODE_PRIVATE)
        selectedStyle = prefs.getInt("fallback_style", 0)
        
        // Pre-generate bitmaps for snappy UI
        val engine = GlyphFontEngine
        styles.forEach { (id, _) ->
            bitmaps[id] = engine.generatePreviewBitmap(id)
        }
        
        val rv = view.findViewById<RecyclerView>(R.id.rvFallbackStyles)
        rv.layoutManager = GridLayoutManager(requireContext(), 3) // 3 columns
        val adapter = StyleAdapter()
        rv.adapter = adapter
        
        view.findViewById<MaterialButton>(R.id.btnApplyFallback).setOnClickListener {
            prefs.edit().putInt("fallback_style", selectedStyle).apply()
            onStyleSelected?.invoke(selectedStyle)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cleanup bitmaps to prevent memory leaks
        bitmaps.values.forEach { it.recycle() }
        bitmaps.clear()
    }

    private inner class StyleAdapter : RecyclerView.Adapter<StyleAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardContainer: MaterialCardView = view.findViewById(R.id.cardContainer)
            val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
            val tvName: TextView = view.findViewById(R.id.tvName)
            
            init {
                cardContainer.setOnClickListener {
                    val prevSelected = selectedStyle
                    selectedStyle = styles[adapterPosition].first
                    
                    // Update only changed items for efficiency
                    val prevIndex = styles.indexOfFirst { it.first == prevSelected }
                    notifyItemChanged(prevIndex)
                    notifyItemChanged(adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fallback_style, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (id, title) = styles[position]
            holder.tvName.text = title
            holder.ivPreview.setImageBitmap(bitmaps[id])
            
            if (id == selectedStyle) {
                holder.cardContainer.strokeWidth = 6 // Thicker outline for selected
                holder.cardContainer.strokeColor = Color.WHITE // Or accent color
                holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                holder.cardContainer.strokeWidth = 1
                holder.cardContainer.strokeColor = Color.parseColor("#444444")
                holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        override fun getItemCount() = styles.size
    }
}
