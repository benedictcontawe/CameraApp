package com.example.cameraapp

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG = BaseActivity::class.java.simpleName
    }

    public fun getContext() : Context = this

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.d(TAG,"onCreate()")
        //checkPermission()
    }

    fun showToast(message: String) {
        Log.d(TAG,"showToast($message)")
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show()
    }

    public fun showBottomSheetFragment(bottomSheetDialogFragment : BottomSheetDialogFragment) {
        Log.d(TAG,"showBottomSheetFragment($bottomSheetDialogFragment)")
        bottomSheetDialogFragment.show(supportFragmentManager,bottomSheetDialogFragment.javaClass.name)
    }

    private fun checkPermission() {
        Log.d(TAG,"checkPermission()")
        ManifestPermission.checkSelfPermission(this, ManifestPermission.allPermissions,
            isGranted = {
                Toast.makeText(this,"Camera Permission Granted!",Toast.LENGTH_LONG).show()
            },
            isDenied = {
                ManifestPermission.requestPermissions(this, ManifestPermission.allPermissions, ManifestPermission.ALL_PERMISSION_CODE)
            }
        )
    }

    abstract fun grantedCode(requestCode : Int)

    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG,"onRequestPermissionsResult($requestCode,$permissions,$grantResults)")
        ManifestPermission.checkPermissionsResult(this, permissions, grantResults,
            isGranted = { Log.d(TAG,"Granted!")
                grantedCode(requestCode)
            },
            isDenied = { Log.d(TAG,"Denied!") },
            isNeverAskAgain = { Log.d(TAG,"Never Ask Again!")
                ManifestPermission.showRationalDialog(this,"Go to App Permission Settings?")
            }
        )
    }
}