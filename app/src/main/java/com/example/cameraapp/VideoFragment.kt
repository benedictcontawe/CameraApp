package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.VideoBinder
import kotlinx.coroutines.CoroutineScope
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
    }

    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if(isActionUp && isInsideBounds(view) && view == binder?.buttonShutterRecord) {
            binder?.buttonShutterRecord?.setOnTouchListener(null)
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
        val preview = Preview.Builder().build()
        cameraProviderFuture.addListener(
            {
                try {
                    preview.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(binder?.getLifecycleOwner()!!, binder?.getViewModel()?.getCameraSelector()!!, preview, binder?.getViewModel()?.imageCapture!!)
                } catch (e : ExecutionException) {
                    logError(TAG, e.message, e)
                } catch (e : InterruptedException) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    } ) }

    private fun captureVideo() { Coroutines.main(this@VideoFragment, {
        /*val videoCapture = this.videoCapture ?: return

        viewBinding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }*/
    } ) }
}