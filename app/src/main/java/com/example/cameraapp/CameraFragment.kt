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

public class CameraFragment : BaseFragment() {

    companion object {
        private val TAG = CameraFragment::class.java.getSimpleName()

        fun newInstance() : CameraFragment {
            return CameraFragment()
        }
    }

    private var binder : CameraBinder? = null
    private val viewModel : CameraViewModel by lazy { ViewModelProvider(requireActivity()).get(CameraViewModel::class.java) }

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
            takePicture()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonLensFlip) {
            binder?.getViewModel()?.flipCamera()
            startCamera()
            true
        } else super.onTouchFragment(view, event)
    }

    private fun startCamera() { Coroutines.main(this@CameraFragment, {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        binder?.getViewModel()?.cameraProvider = cameraProviderFuture.get()
        cameraProviderFuture.addListener( {
            try {
                binder?.getViewModel()?.preview?.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                binder?.getViewModel()?.cameraProvider?.unbindAll()
                binder?.getViewModel()?.cameraProvider?.bindToLifecycle (
                    binder?.getLifecycleOwner()!!,
                    binder?.getViewModel()?.getCameraSelector()!!,
                    binder?.getViewModel()?.preview,
                    binder?.getViewModel()?.imageCapture!!
                )
            } catch (e : ExecutionException) {
                logError(TAG, e.message, e)
            } catch (e : InterruptedException) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor( requireContext()) )
    } ) }

    private fun takePicture() { Coroutines.main(this@CameraFragment, {
        binder?.getViewModel()?.imageCapture?.takePicture (
            binder?.getViewModel()?.getOutputFileOptions(null)!!,
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
    } ) }

    override fun onDestroy() {
        binder?.getViewModel()?.cameraProvider?.unbindAll()
        super.onDestroy()
    }
}