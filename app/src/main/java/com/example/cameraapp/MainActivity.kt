package com.example.cameraapp

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.MainBinder
import kotlinx.coroutines.CoroutineScope

public class MainActivity : BaseActivity(), View.OnClickListener {

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private val CAMERA_REAR = CameraSelector.DEFAULT_BACK_CAMERA
    }

    private var binder : MainBinder? = null
    private val viewModel : OptionBottomSheetViewModel by lazy { ViewModelProvider(this@MainActivity).get(OptionBottomSheetViewModel::class.java) }
    private val imageCapture : ImageCapture by lazy { ImageCapture.Builder().build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        binder = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        binder?.setLifecycleOwner(this@MainActivity)
        super.onCreate(savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        binder?.imageView?.setOnClickListener(this@MainActivity)
    }

    override fun onClick(view : View?) {
        if (view == binder?.imageView && viewModel.isShowed()?.not() == true)
            showBottomSheetFragment( OptionBottomSheetDialogFragment.newInstance() )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
        /*cameraProviderFuture.addListener({
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binder?.previewView?.getSurfaceProvider())
            try {

            } catch (e : Exception) {

            }
        })*/
    }

    override fun onResume() {
        super.onResume()
    }


    override fun onPause() {
        super.onPause()
    }
    /*
    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e(TAG,"requestCode - $requestCode")
        Log.e(TAG,"resultCode - $resultCode")
        Log.e(TAG,"data - $data")
        viewModel.checkActivityResult(requestCode,resultCode,data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
    */
}