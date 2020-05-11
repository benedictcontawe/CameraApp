package com.example.cameraapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object ManifestPermission {

    private val TAG = ManifestPermission::class.java.simpleName

    const val PERMISSION_SETTINGS_CODE = 1000
    const val CAMERA_PERMISSION_CODE = 1004
    const val GALLERY_PERMISSION_CODE = 1006

    val cameraPermission = Manifest.permission.CAMERA

    val galleryPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun checkSelfPermission(context : Context, permission : String, isGranted : () -> Unit = {}, isDenied : () -> Unit = {}) {
        Log.d(TAG,"checkSelfPermission($context,$permission,isGranted(),isDenied())")
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"allGranted()")
            isGranted()
        }
        else {
            Log.d(TAG,"denied()")
            isDenied()
        }
    }

    fun requestPermission(activity : Activity, permission : String, requestCode : Int) {
        Log.d(TAG,"requestPermissions($activity,$permission,$requestCode")
        ActivityCompat.requestPermissions(activity, arrayOf(permission),requestCode)
    }

    fun checkPermissionsResult(activity : Activity, permissions : Array<String>, grantResults : IntArray,isGranted : () -> Unit, isNeverAskAgain : () -> Unit = {}, isDenied : () -> Unit) {
        when {
            grantResults.filter { results -> results ==  PackageManager.PERMISSION_DENIED}.isEmpty() -> {
                Log.d(TAG,"isGranted()")
                isGranted()
            }
            permissions.filter { permission -> ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) && ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED }.none() -> {
                Log.d(TAG,"isNeverAskAgain()")
                isNeverAskAgain()
            }
            grantResults.filter { results -> results ==  PackageManager.PERMISSION_DENIED}.isNotEmpty() -> {
                Log.d(TAG,"isDenied()")
                isDenied()
            }
        }
    }

    public fun showRationalDialog(activity : Activity, message : String) {
        Log.d(TAG,"showRationalDialog($activity,$message")
        val builder = activity.let { AlertDialog.Builder(it) }
        builder.setTitle("Manifest Permissions")
        builder.setMessage(message)
        builder.setPositiveButton("SETTINGS") { dialog, which ->
            dialog.dismiss()
            showAppPermissionSettings(activity)
        }
        builder.setNegativeButton("NOT NOW") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showAppPermissionSettings(activity : Activity) {
        Log.d("PermissionsResult", "showAppPermissionSettings()")
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        activity.startActivityForResult(intent, PERMISSION_SETTINGS_CODE)
    }
}