package com.example.cameraapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

public object ManifestPermission {

    private val TAG = ManifestPermission::class.java.getSimpleName()

    const val cameraPermission = Manifest.permission.CAMERA

    val videoRecordPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
        )
    } else {
        arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    val microphonePermission = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    val galleryPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun checkSelfPermission(context : Context, permission : String,) : Boolean {
        Log.d(TAG,"checkSelfPermission($context,$permission, isGranted(), isDenied())")
        val isGranted : Boolean
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"isGranted()")
            isGranted = true
        } else {
            Log.d(TAG,"denied()")
            isGranted = false
        }
        return isGranted
    }

    fun checkSelfPermission(context : Context, permission : String, isGranted : () -> Unit = {}, isDenied : () -> Unit = {}) {
        Log.d(TAG,"checkSelfPermission($context,$permission, isGranted(), isDenied())")
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"isGranted()")
            isGranted()
        } else {
            Log.d(TAG,"denied()")
            isDenied()
        }
    }

    fun checkSelfPermission(context : Context, permissions : Array<String>, isGranted : () -> Unit = {}, isDenied : () -> Unit = {}) {
        Log.d(TAG,"checkSelfPermission($context,$permissions, isGranted(), isDenied())")
        if (permissions.filter { permission -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED }.isEmpty()) {
            Log.d(TAG,"allGranted()")
            isGranted()
        } else {
            Log.d(TAG,"denied()")
            isDenied()
        }
    }

    fun requestPermission(permissionResultResultLauncher : ActivityResultLauncher<String>, permission : String) {
        Log.d(TAG,"requestPermission($permissionResultResultLauncher,$permission")
        permissionResultResultLauncher.launch(permission)
    }

    fun requestPermission(permissionResultResultLauncher : ActivityResultLauncher<Array<String>>, permissions : Array<String>) {
        Log.d(TAG,"requestPermission($permissionResultResultLauncher,$permissions")
        permissionResultResultLauncher.launch(permissions)
    }
}