package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraProvider
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.VideoBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutionException

public class VideoFragment : BaseFragment() {

    companion object {
        private val TAG = VideoFragment::class.java.getSimpleName()

        fun newInstance() : VideoFragment {
            return VideoFragment()
        }
    }

    private var binder : VideoBinder? = null
    private val viewModel : CameraViewModel by lazy { ViewModelProvider(requireActivity()).get(CameraViewModel::class.java) }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_video,container,false)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(getViewLifecycleOwner())
        return binder?.root ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        startCamera()
        binder?.buttonShutterRecord?.setOnTouchListener(this@VideoFragment)
        binder?.buttonLensFlip?.setOnTouchListener(this@VideoFragment)
        scope.launch( block = {
            binder?.getViewModel()?.observeRecording()?.collectLatest( action = { isRecording ->
                logError(TAG,"observeRecording $isRecording")
                if (isRecording == true) binder?.buttonShutterRecord?.setImageResource(R.drawable.ic_recording)
                else binder?.buttonShutterRecord?.setImageResource(R.drawable.ic_record)
            } )
        } )
    }

    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if(isActionUp && isInsideBounds(view) && view == binder?.buttonShutterRecord) {
            binder?.getViewModel()?.toggleRecording()
            captureVideo()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonLensFlip) {
            binder?.getViewModel()?.flipCamera()
            startCamera()
            true
        } else super.onTouchFragment(view, event)
    }

    private fun startCamera() { Coroutines.main(this@VideoFragment, {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
        cameraProviderFuture.addListener(
            {
                try {
                    binder?.getViewModel()?.preview?.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                    cameraProvider.unbindAll()
                    //cameraProvider.bindToLifecycle(binder?.getLifecycleOwner()!!, binder?.getViewModel()?.getCameraSelector()!!, binder?.getViewModel()?.preview, binder?.getViewModel()?.videoCapture!!)
                } catch (e : ExecutionException) {
                    logError(TAG, e.message, e)
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    } ) }

    private fun captureVideo() { Coroutines.main(this@VideoFragment, {

    } ) }
}