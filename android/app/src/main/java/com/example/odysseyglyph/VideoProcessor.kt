package com.example.odysseyglyph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

object VideoProcessor {
    const val MATRIX_SIZE = 25

    fun processMedia(
        context: Context, 
        mediaUri: Uri, 
        mediaType: Int, // 0=video, 1=gif, 2=static
        startTimeMs: Long,
        endTimeMs: Long,
        targetFps: Int,
        playbackMode: Int,
        invertColors: Boolean,
        contrastMulti: Float = 1.0f,
        sharpen: Boolean = true,
        cropCx: Float,
        cropCy: Float,
        cropRadius: Float,
        inverseTransform: Matrix,
        slotIndex: Int = 1,
        onProgress: (Int) -> Unit, 
        onComplete: (Boolean, String?) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        
        thread {
            try {
                // Map screen coordinates to raw video coordinates using the inverse transform matrix
                val pts = floatArrayOf(cropCx, cropCy, cropCx + cropRadius, cropCy)
                inverseTransform.mapPoints(pts)
                val videoCx = pts[0]
                val videoCy = pts[1]
                val videoRadius = Math.hypot((pts[2] - pts[0]).toDouble(), (pts[3] - pts[1]).toDouble()).toFloat()

                val rawFrames = mutableListOf<ByteArray>()

                if (mediaType == 0) {
                    // --- VIDEO ---
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, mediaUri)
                        
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: run {
                            mainHandler.post { onComplete(false, "Could not determine video duration.") }
                            return@thread
                        }
                        
                        val validEndMs = if (endTimeMs <= 0 || endTimeMs > durationMs) durationMs else endTimeMs
                        val validStartMs = if (startTimeMs < 0 || startTimeMs >= validEndMs) 0L else startTimeMs
                        
                        val processDurationMs = validEndMs - validStartMs
                        if (processDurationMs <= 0) {
                            mainHandler.post { onComplete(false, "Trimmed duration is zero.") }
                            return@thread
                        }
                        
                        val intervalMs = 1000L / targetFps
                        val totalFrames = (processDurationMs / intervalMs).toInt()
                        if (totalFrames == 0) {
                            mainHandler.post { onComplete(false, "Video is too short for the selected FPS.") }
                            return@thread
                        }

                        for (i in 0 until totalFrames) {
                            val timeUs = (validStartMs + i * intervalMs) * 1000
                            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                            if (bitmap != null) {
                                val frame = extractAndDownsampleFrame(bitmap, videoCx, videoCy, videoRadius)
                                if (frame != null) rawFrames.add(frame)
                                bitmap.recycle()
                            }
                            val prog = (i.toFloat() / totalFrames * 50).toInt()
                            mainHandler.post { onProgress(prog) }
                        }
                    } finally {
                        retriever.release()
                    }
                } else if (mediaType == 1) {
                    // --- GIF ---
                    val stream = context.contentResolver.openInputStream(mediaUri)
                    val movie = android.graphics.Movie.decodeStream(stream)
                    stream?.close()
                    
                    if (movie == null) {
                        mainHandler.post { onComplete(false, "Could not decode GIF.") }
                        return@thread
                    }
                    
                    val durationMs = movie.duration().toLong().coerceAtLeast(1L)
                    val validEndMs = if (endTimeMs <= 0 || endTimeMs > durationMs) durationMs else endTimeMs
                    val validStartMs = if (startTimeMs < 0 || startTimeMs >= validEndMs) 0L else startTimeMs
                    val processDurationMs = validEndMs - validStartMs
                    
                    val intervalMs = 1000L / targetFps
                    val totalFrames = (processDurationMs / intervalMs).toInt().coerceAtLeast(1)
                    
                    val bitmap = Bitmap.createBitmap(movie.width().coerceAtLeast(1), movie.height().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    
                    for (i in 0 until totalFrames) {
                        val timeMs = validStartMs + i * intervalMs
                        movie.setTime(timeMs.toInt())
                        
                        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                        movie.draw(canvas, 0f, 0f)
                        
                        val frame = extractAndDownsampleFrame(bitmap, videoCx, videoCy, videoRadius)
                        if (frame != null) rawFrames.add(frame)
                        
                        val prog = (i.toFloat() / totalFrames * 50).toInt()
                        mainHandler.post { onProgress(prog) }
                    }
                    bitmap.recycle()
                } else {
                    // --- STATIC IMAGE ---
                    // Use BitmapRegionDecoder to prevent OutOfMemoryError on 50MP photos
                    val stream = context.contentResolver.openInputStream(mediaUri)
                    val decoder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        BitmapRegionDecoder.newInstance(stream!!, false)
                    } else {
                        @Suppress("DEPRECATION")
                        BitmapRegionDecoder.newInstance(stream!!, false)
                    }
                    stream.close()
                    
                    if (decoder == null) {
                        mainHandler.post { onComplete(false, "Could not create image decoder.") }
                        return@thread
                    }
                    
                    val cropSide = (videoRadius * 2).toInt()
                    val left = (videoCx - videoRadius).toInt().coerceIn(0, decoder.width - 1)
                    val top = (videoCy - videoRadius).toInt().coerceIn(0, decoder.height - 1)
                    val width = cropSide.coerceAtMost(decoder.width - left)
                    val height = cropSide.coerceAtMost(decoder.height - top)
                    
                    if (width <= 0 || height <= 0) {
                        decoder.recycle()
                        mainHandler.post { onComplete(false, "Invalid crop region.") }
                        return@thread
                    }
                    
                    val rect = Rect(left, top, left + width, top + height)
                    
                    var sampleSize = 1
                    while (width / sampleSize > 200 && height / sampleSize > 200) {
                        sampleSize *= 2
                    }
                    
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = true
                    }
                    
                    val square = decoder.decodeRegion(rect, options)
                    decoder.recycle()
                    
                    if (square != null) {
                        val frame = downsampleSquareFrame(square)
                        if (frame != null) rawFrames.add(frame)
                        square.recycle()
                    }
                    
                    mainHandler.post { onProgress(50) }
                }

                if (rawFrames.isEmpty()) {
                    mainHandler.post { onComplete(false, "No valid frames could be extracted.") }
                    return@thread
                }

                // Process contrast and sharpening for all frames
                val finalFrames = mutableListOf<ByteArray>()
                val center = (MATRIX_SIZE - 1) / 2f
                val radiusSq = (MATRIX_SIZE / 2f) * (MATRIX_SIZE / 2f)

                for ((index, frame) in rawFrames.withIndex()) {
                    val processedFrame = ByteArray(MATRIX_SIZE * MATRIX_SIZE)
                    
                    // Extract to 2D grid for spatial filtering
                    val grid = Array(MATRIX_SIZE) { FloatArray(MATRIX_SIZE) }
                    var frameMin = 255f
                    var frameMax = 0f
                    for (y in 0 until MATRIX_SIZE) {
                        for (x in 0 until MATRIX_SIZE) {
                            val distSq = (x - center) * (x - center) + (y - center) * (y - center)
                            val v = (frame[y * MATRIX_SIZE + x].toInt() and 0xFF).toFloat()
                            grid[y][x] = v
                            if (distSq <= radiusSq) {
                                if (v < frameMin) frameMin = v
                                if (v > frameMax) frameMax = v
                            }
                        }
                    }
                    
                    if (frameMax <= frameMin) frameMax = frameMin + 1
                    val frameRange = frameMax - frameMin

                    // Apply 3x3 Sharpening kernel
                    val sharpenedGrid = Array(MATRIX_SIZE) { FloatArray(MATRIX_SIZE) }
                    for (y in 0 until MATRIX_SIZE) {
                        for (x in 0 until MATRIX_SIZE) {
                            if (y == 0 || y == MATRIX_SIZE - 1 || x == 0 || x == MATRIX_SIZE - 1 || !sharpen) {
                                sharpenedGrid[y][x] = grid[y][x]
                            } else {
                                val sum = 5 * grid[y][x] - grid[y-1][x] - grid[y+1][x] - grid[y][x-1] - grid[y][x+1]
                                sharpenedGrid[y][x] = sum.coerceIn(0f, 255f)
                            }
                        }
                    }

                    for (y in 0 until MATRIX_SIZE) {
                        for (x in 0 until MATRIX_SIZE) {
                            val distSq = (x - center) * (x - center) + (y - center) * (y - center)
                            val idx = y * MATRIX_SIZE + x
                            if (distSq <= radiusSq) {
                                val original = sharpenedGrid[y][x]
                                
                                val stretched = ((original - frameMin) / frameRange).coerceIn(0f, 1f)
                                
                                // Apply contrast multiplier
                                val adjustedStretched = ((stretched - 0.5f) * contrastMulti + 0.5f).coerceIn(0f, 1f)
                                
                                var contrasted = adjustedStretched * adjustedStretched * (3f - 2f * adjustedStretched)
                                var finalValue = (contrasted * 255f).toInt()
                                
                                if (invertColors) {
                                    finalValue = 255 - finalValue
                                }
                                processedFrame[idx] = finalValue.coerceIn(0, 255).toByte()
                            } else {
                                processedFrame[idx] = 0 // Mask out corners
                            }
                        }
                    }
                    finalFrames.add(processedFrame)
                    
                    val prog = 50 + (index.toFloat() / rawFrames.size * 40).toInt()
                    mainHandler.post { onProgress(prog) }
                }

                // Handle Ping-Pong mode (PlaybackMode == 2) for multi-frame media
                if (playbackMode == 2 && finalFrames.size > 2) {
                    val reverseFrames = finalFrames.subList(1, finalFrames.size - 1).reversed()
                    finalFrames.addAll(reverseFrames)
                }

                // Write to frames_slotX.bin
                val outFile = File(context.filesDir, "frames_slot$slotIndex.bin")
                FileOutputStream(outFile).use { fos ->
                    val header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                    header.putInt(finalFrames.size)
                    header.putInt(targetFps)
                    header.putInt(playbackMode)
                    fos.write(header.array())

                    for (frame in finalFrames) {
                        fos.write(frame)
                    }
                }
                
                mainHandler.post { onProgress(100) }
                mainHandler.post { onComplete(true, null) }
            } catch (e: Throwable) {
                e.printStackTrace()
                mainHandler.post { onComplete(false, e.message ?: "Unknown error occurred.") }
            }
        }
    }

    private fun extractAndDownsampleFrame(bitmap: Bitmap, videoCx: Float, videoCy: Float, videoRadius: Float): ByteArray? {
        val cropSide = (videoRadius * 2).toInt()
        val left = (videoCx - videoRadius).toInt().coerceIn(0, bitmap.width - 1)
        val top = (videoCy - videoRadius).toInt().coerceIn(0, bitmap.height - 1)
        val width = cropSide.coerceAtMost(bitmap.width - left)
        val height = cropSide.coerceAtMost(bitmap.height - top)
        
        if (width <= 0 || height <= 0) return null

        val square = Bitmap.createBitmap(bitmap, left, top, width, height)
        val result = downsampleSquareFrame(square)
        if (square != bitmap) square.recycle()
        return result
    }

    private fun downsampleSquareFrame(square: Bitmap): ByteArray? {
        val small = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val scaleX = square.width.toFloat() / MATRIX_SIZE
        val scaleY = square.height.toFloat() / MATRIX_SIZE
        
        val pixels = IntArray(square.width * square.height)
        square.getPixels(pixels, 0, square.width, 0, 0, square.width, square.height)
        
        val outPixels = IntArray(MATRIX_SIZE * MATRIX_SIZE)
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                var rSum = 0L
                var gSum = 0L
                var bSum = 0L
                var count = 0
                
                val startX = (x * scaleX).toInt()
                val endX = ((x + 1) * scaleX).toInt().coerceAtMost(square.width)
                val startY = (y * scaleY).toInt()
                val endY = ((y + 1) * scaleY).toInt().coerceAtMost(square.height)
                
                for (sy in startY until endY) {
                    val offset = sy * square.width
                    for (sx in startX until endX) {
                        val px = pixels[offset + sx]
                        rSum += android.graphics.Color.red(px)
                        gSum += android.graphics.Color.green(px)
                        bSum += android.graphics.Color.blue(px)
                        count++
                    }
                }
                
                if (count > 0) {
                    outPixels[y * MATRIX_SIZE + x] = android.graphics.Color.rgb(
                        (rSum / count).toInt(),
                        (gSum / count).toInt(),
                        (bSum / count).toInt()
                    )
                }
            }
        }
        small.setPixels(outPixels, 0, MATRIX_SIZE, 0, 0, MATRIX_SIZE, MATRIX_SIZE)
        
        val grayFrame = convertToGrayscaleByteArray(small)
        small.recycle()
        return grayFrame
    }

    private fun convertToGrayscaleByteArray(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val grayBytes = ByteArray(width * height)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = android.graphics.Color.red(color)
            val g = android.graphics.Color.green(color)
            val b = android.graphics.Color.blue(color)
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            grayBytes[i] = gray.toByte()
        }
        return grayBytes
    }
}
