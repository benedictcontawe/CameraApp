package com.example.cameraapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

public class LuminosityAnalyzer : ImageAnalysis.Analyzer {

    companion object {
        private val TAG = LuminosityAnalyzer::class.java.getSimpleName()
    }

    private val listener : LumaListener

    constructor(listener : LumaListener) {
        this.listener = listener
    }

    private fun ByteBuffer.toByteArray() : ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun analyze(image : ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        //listener(luma)

        image.close()
    }
}