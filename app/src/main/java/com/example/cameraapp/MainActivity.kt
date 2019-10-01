package com.example.cameraapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate
import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.graphics.Matrix
import android.util.Rational
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.TimeUnit

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), LifecycleOwner {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // Add this after onCreate

    private lateinit var viewFinder: TextureView

    private fun startCamera() {

    }

    private fun updateTransform() {

    }
}
