package com.example.cameraapp

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseActivity : AppCompatActivity() {

    public fun showBottomSheetFragment(bottomSheetDialogFragment : BottomSheetDialogFragment) {
        bottomSheetDialogFragment.show(supportFragmentManager,bottomSheetDialogFragment.javaClass.name)
    }

    override fun onResume() {
        super.onResume()
        ManifestPermission.checkSelfPermission(this, ManifestPermission.cameraPermission,
            isGranted = {
                Toast.makeText(this,"Camera Permission Granted!",Toast.LENGTH_LONG).show()
            },
            isDenied = {
                ManifestPermission.requestPermissions(this, ManifestPermission.cameraPermission, ManifestPermission.CAMERA_PERMISSION_CODE)
            }
        )
    }

    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE -> {
                ManifestPermission.checkPermissionsResult(this, permissions, grantResults,
                    isGranted = {
                        Toast.makeText(this,"Camera Permission Granted!", Toast.LENGTH_LONG).show()
                    },
                    isDenied = {
                        ManifestPermission.requestPermissions(this, ManifestPermission.cameraPermission, ManifestPermission.CAMERA_PERMISSION_CODE)
                    },
                    isNeverAskAgain = {
                        ManifestPermission.showRationalDialog(this,"Go to App Permission Settings?")
                    }
                )
            }
        }
    }
}