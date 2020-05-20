package com.example.cameraapp.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cameraapp.OptionBottomSheetViewModel
import com.example.cameraapp.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
//import androidx.camera.core.PreviewConfig

class MainActivity : BaseActivity(), View.OnClickListener {

    private lateinit var optionBottomSheetViewModel : OptionBottomSheetViewModel

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initiateCameraX()
        image_view.setOnClickListener(this)
        button_download.setOnClickListener(this)
        optionBottomSheetViewModel = ViewModelProvider(this).get(OptionBottomSheetViewModel::class.java)
        optionBottomSheetViewModel.observePhotoPath().observe(this, Observer { imagePath ->
            when {
                imagePath.isNotBlank() -> {
                    Toast.makeText(this,"isNotBlank",Toast.LENGTH_SHORT).show()
                    Glide.with(this)
                        .asBitmap()
                        .placeholder(R.mipmap.ic_launcher)
                        .load(imagePath)
                        .apply(RequestOptions.circleCropTransform())
                        .into(image_view)
                }
                imagePath.isNullOrBlank() -> {
                    Toast.makeText(this,"isNullOrBlank",Toast.LENGTH_SHORT).show()
                    Glide.with(this)
                        .asBitmap()
                        .placeholder(R.mipmap.ic_launcher)
                        .load("")
                        .apply(RequestOptions.circleCropTransform())
                        .into(image_view)
                }
            }
        })
    }

    override fun onClick(view : View) {
        when(view) {
            image_view -> {
                if (!optionBottomSheetViewModel.isShowed()) {
                    showBottomSheetFragment(
                        OptionBottomSheetDialogFragment.newInstance()
                    )
                }
            }
            button_download -> {
                //optionBottomSheetViewModel.downloadImage("http://213.212.61.24:9090/i/u/11jmbds5.jpg?fd57ed1113d02359df53c61c0a4726d53bcd2952")
                optionBottomSheetViewModel.downloadImage(edit_text_download.getText().toString())
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
    //endregion
    override fun grantedCode(requestCode: Int) {
        optionBottomSheetViewModel.setGrantedRequestCode(requestCode)
    }
}
