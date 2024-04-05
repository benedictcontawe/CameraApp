package com.example.cameraapp

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun onSetObservers(scope : CoroutineScope) {
        startCamera()
        binder?.buttonShutterRecord?.setOnTouchListener(this@VideoFragment)
        binder?.buttonLensFlip?.setOnTouchListener(this@VideoFragment)
        scope.launch( block = {
            binder?.getViewModel()?.observeRecording()?.collectLatest( action = { isRecording ->
                logDebug(TAG,"observeRecording $isRecording")
                if (isRecording == true) {
                    binder?.buttonShutterRecord?.setImageResource(R.drawable.ic_recording)
                    captureVideo()
                } else {
                    binder?.buttonShutterRecord?.setImageResource(R.drawable.ic_record)
                    binder?.getViewModel()?.stopRecording()
                }
            } )
        } )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if(isActionUp && isInsideBounds(view) && view == binder?.buttonShutterRecord) {
            binder?.getViewModel()?.playRecording()
            binder?.getViewModel()?.playVibrate()
            binder?.getViewModel()?.toggleRecording()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonLensFlip) {
            binder?.getViewModel()?.flipCamera()
            startCamera()
            true
        } else super.onTouchFragment(view, event)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCamera() { Coroutines.main(this@VideoFragment, {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        binder?.getViewModel()?.cameraProvider = cameraProviderFuture.get()
        cameraProviderFuture.addListener(
            {
                try {
                    binder?.getViewModel()?.preview?.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                    binder?.getViewModel()?.cameraProvider?.unbindAll()
                    binder?.getViewModel()?.flipRecorder()
                    binder?.getViewModel()?.cameraProvider?.bindToLifecycle(
                        binder?.getLifecycleOwner()!!,
                        binder?.getViewModel()?.getCameraSelector()!!,
                        binder?.getViewModel()?.preview,
                        binder?.getViewModel()?.videoCapture!!,
                    )
                } catch (e : ExecutionException) {
                    logError(TAG, e.message, e)
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor( requireContext() )
        )
    } ) }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun captureVideo() { Coroutines.main(this@VideoFragment, {
        binder?.getViewModel()?.startRecording(
            requireActivity().getContentResolver(),
            binder?.getViewModel()?.getContentValues(null, null)!!,
            ContextCompat.getMainExecutor(requireContext()),
            binder?.getViewModel()?.getRecordingListener()!!
        )
    } ) }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        binder?.getViewModel()?.cameraProvider?.unbindAll()
        super.onDestroy()
    }
}