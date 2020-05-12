package com.example.cameraapp

import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
//import androidx.camera.core.PreviewConfig

class MainActivity : BaseActivity(), View.OnClickListener {

    private lateinit var viewModel : OptionBottomSheetViewModel

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initiateCameraX()
        image_view.setOnClickListener(this)
        viewModel = ViewModelProvider(this).get(OptionBottomSheetViewModel::class.java)
    }

    override fun onClick(view : View) {
        when(view) {
            image_view -> {
                showBottomSheetFragment(
                    OptionBottomSheetDialogFragment.newInstance()
                )
            }
        }
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
}
