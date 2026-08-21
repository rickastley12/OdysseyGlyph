package com.example.odysseyglyph

import android.content.Context

object MatrixConfig {
    const val MATRIX_SIZE_PHONE_3 = 25
    const val MATRIX_SIZE_PHONE_4A_PRO = 13

    // Get the current simulated matrix size from preferences
    fun getMatrixSize(context: Context): Int {
        val prefs = context.getSharedPreferences("OdysseyPrefs", Context.MODE_PRIVATE)
        val simulate4aPro = prefs.getBoolean("simulate_4a_pro", false)
        return if (simulate4aPro) MATRIX_SIZE_PHONE_4A_PRO else MATRIX_SIZE_PHONE_3
    }
    
    // Real hardware matrix size, ignoring simulator mode
    fun getHardwareMatrixSize(): Int {
        // If we want dynamic detection in the future based on Build.MODEL:
        // if (android.os.Build.MODEL.contains("4a Pro")) return MATRIX_SIZE_PHONE_4A_PRO
        return MATRIX_SIZE_PHONE_3 // Assume Phone 3 hardware for this specific build/device
    }

    // Safely format a byte array for the physical hardware, upscaling if needed
    fun formatForHardware(frame: ByteArray): IntArray {
        val hardwareSize = getHardwareMatrixSize()
        val expectedLength = hardwareSize * hardwareSize
        
        if (frame.size == expectedLength) {
            val result = IntArray(expectedLength)
            for (i in frame.indices) result[i] = (frame[i].toInt() and 0xFF) * 16 // 0-255 -> 0-4095
            return result
        }
        
        // If we are simulating 13x13 (169) on 25x25 hardware (625)
        if (frame.size == 169 && expectedLength == 625) {
            val result = IntArray(625)
            val scale = 25f / 13f
            for (y in 0 until 25) {
                for (x in 0 until 25) {
                    val srcX = (x / scale).toInt().coerceIn(0, 12)
                    val srcY = (y / scale).toInt().coerceIn(0, 12)
                    val value = frame[srcY * 13 + srcX].toInt() and 0xFF
                    result[y * 25 + x] = value * 16
                }
            }
            return result
        }
        
        // Fallback: return empty array to prevent crashes
        return IntArray(expectedLength)
    }
    
    // Total cells in the matrix (e.g. 625 for 25x25, 169 for 13x13)
    fun getCellCount(context: Context): Int {
        val size = getMatrixSize(context)
        return size * size
    }
}
