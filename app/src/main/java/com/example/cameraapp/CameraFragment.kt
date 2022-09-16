package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
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
    private val viewModel : CameraViewModel by lazy { ViewModelProvider(requireActivity()).get(CameraViewModel::class.java) }
    private val imageCapture : ImageCapture by lazy { ImageCapture.Builder().build() }
    private var lensFacing : Int = CameraSelector.LENS_FACING_BACK

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_camera,container,false)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(getViewLifecycleOwner())
        return binder?.root ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        startCamera()
        binder?.buttonShutterCapture?.setOnTouchListener(this@CameraFragment)
        binder?.buttonLensFlip?.setOnTouchListener(this@CameraFragment)
    }

    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if(isActionUp && isInsideBounds(view) && view == binder?.buttonShutterCapture) {
            binder?.buttonShutterCapture?.setOnTouchListener(null)
            binder?.getViewModel()?.playShutter()
            binder?.getViewModel()?.playVibrate()
            takePhoto()
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
        cameraProviderFuture.addListener(
            {
                try {
                    preview.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(binder?.getLifecycleOwner()!!, cameraSelector, preview, imageCapture)
                } catch (e : ExecutionException) {
                    logError(TAG, e.message, e)
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun takePhoto() {
        val photoFile = binder?.getViewModel()?.getCacheFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile!!).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output : ImageCapture.OutputFileResults) {
                    binder?.getViewModel()?.logImageSaved(output)
                    binder?.buttonShutterCapture?.setOnTouchListener(this@CameraFragment)
                }
                override fun onError(exc : ImageCaptureException) {
                    logError(TAG, "Photo capture failed: ${exc.message}", exc)
                    binder?.buttonShutterCapture?.setOnTouchListener(this@CameraFragment)
            }
        })
    }
}