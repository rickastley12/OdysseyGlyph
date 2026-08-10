package com.example.odysseyglyph

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Nothing has no UI requirement for a Glyph Toy's launcher app itself —
 * this just gives the user a button that opens the system's "Manage Glyph
 * Toys" screen, where they add "Odyssey" to their active toy carousel.
 * (Best practice per the GDK README.)
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        layout.addView(TextView(this).apply {
            text = "Odyssey Glyph Toy"
            textSize = 20f
        })
        layout.addView(TextView(this).apply {
            text = "Tap below, then add \"Odyssey\" to your Glyph Toy carousel. " +
                "Cycle to it with the Glyph Button on the back of the phone."
            setPadding(0, 24, 0, 48)
        })
        layout.addView(Button(this).apply {
            text = "Open Glyph Toys Manager"
            setOnClickListener {
                val intent = Intent()
                intent.component = ComponentName(
                    "com.nothing.thirdparty",
                    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                )
                startActivity(intent)
            }
        })

        setContentView(layout)
    }
}
