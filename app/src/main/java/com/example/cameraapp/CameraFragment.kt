package com.example.cameraapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.CameraBinder
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ExecutionException

class CameraFragment : BaseFragment() {

    companion object {
        private val TAG = CameraFragment::class.java.getSimpleName()

        fun newInstance() : CameraFragment {
            return CameraFragment()
        }
    }

    private var binder : CameraBinder? = null
    private val viewModel : OptionBottomSheetViewModel by lazy { ViewModelProvider(requireActivity()).get(OptionBottomSheetViewModel::class.java) }
    private val imageCapture : ImageCapture by lazy { ImageCapture.Builder().build() }
    private var lensFacing : Int = CameraSelector.LENS_FACING_BACK

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_camera,container,false)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(getViewLifecycleOwner())
        return binder?.root ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override suspend fun onSetObservers(scope: CoroutineScope) {
        startCamera()
        binder?.buttonShutterCapture?.setOnTouchListener(this@CameraFragment)
        binder?.buttonLensFlip?.setOnTouchListener(this@CameraFragment)
    }

    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if(isActionUp && isInsideBounds(view) && view == binder?.buttonShutterCapture) {
            showToast("Shutter Capture")
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonLensFlip) {
            flipCamera()
            startCamera()
            true
        } else super.onTouchFragment(view, event)
    }

    private fun flipCamera() {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) lensFacing = CameraSelector.LENS_FACING_BACK
        else if (lensFacing == CameraSelector.LENS_FACING_BACK) lensFacing = CameraSelector.LENS_FACING_FRONT
    }

    private fun startCamera() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        cameraProviderFuture.addListener({
            try {
                preview.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@CameraFragment, cameraSelector, preview)
            } catch (e : ExecutionException) {
                logError(TAG, e.message, e)
            } catch (e : InterruptedException) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext())
        )
        //val cameraIntent : Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel?.createCameraPictureFile())
        //startActivityForResult(cameraIntent, CAMERA_MEDIA_CODE)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case

        // Create time-stamped output file to hold the image
        /*val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )*/

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output : ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    showToast(msg)
                    logDebug(TAG, msg)
                }
                override fun onError(exc : ImageCaptureException) {
                    logError(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            })
    }

    /*private fun bindCameraUseCases() {
        // Make sure that there are no other use cases bound to CameraX
        CameraX.unbindAll()

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
        }.build()
        val preview = Preview(previewConfig)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
        }.build()
        imageCapture = ImageCapture(imageCaptureConfig)

        // Apply declared configs to CameraX using the same lifecycle owner
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }*/
}