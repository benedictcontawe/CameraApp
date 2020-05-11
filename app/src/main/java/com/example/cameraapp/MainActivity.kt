package com.example.cameraapp

import android.os.Bundle
import android.os.Environment
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
//import androidx.camera.core.PreviewConfig

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initiateCameraX()
    }

    override fun onResume() {
        super.onResume()
        ManifestPermission.checkSelfPermission(this@MainActivity, ManifestPermission.cameraPermission,
            isGranted = {
                Toast.makeText(this@MainActivity,"Camera Permission Granted!",Toast.LENGTH_LONG).show()
            },
            isDenied = {
                ManifestPermission.requestPermission(this@MainActivity, ManifestPermission.cameraPermission, ManifestPermission.CAMERA_PERMISSION_CODE)
            }
        )
    }
    //region Camera X Methods
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private fun initiateCameraX() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    }

    private fun startCameraX() {
        val cameraSelector : CameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.bindToLifecycle(this, cameraSelector)
        }, ContextCompat.getMainExecutor(this))

        //view_camera.bindToLifecycle(this)
        //view_camera.takePicture(getImageFile(),object : ImageCapture.OnImageCapturedCallback)

        //CameraX.unbindAll()

        //val preview = createPreviewUseCase()
    }

    private fun updateTransform() {

    }

    private fun getImageFile() : File {
        val filePath : String = Environment.getExternalStorageDirectory().getPath()
        val fileName : String = "cameraXSample.jpg"
        return File(filePath,fileName)
    }
    //endregion
    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE -> {
                ManifestPermission.checkPermissionsResult(this@MainActivity, permissions, grantResults,
                    isGranted = {
                        Toast.makeText(this@MainActivity,"Camera Permission Granted!",Toast.LENGTH_LONG).show()
                    },
                    isDenied = {
                        ManifestPermission.requestPermission(this@MainActivity, ManifestPermission.cameraPermission, ManifestPermission.CAMERA_PERMISSION_CODE)
                    },
                    isNeverAskAgain = {
                        ManifestPermission.showRationalDialog(this@MainActivity,"Go to App Permission Settings?")
                    }
                )
            }
        }
    }
}
