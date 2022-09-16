package com.example.cameraapp

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.MainBinder
import kotlinx.coroutines.CoroutineScope

public class MainActivity : BaseActivity(), View.OnClickListener, MainListener {

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
    }

    private var binder : MainBinder? = null
    private val viewModel : OptionBottomSheetViewModel by lazy { ViewModelProvider(this@MainActivity).get(OptionBottomSheetViewModel::class.java) }

    override fun onCreate(savedInstanceState : Bundle?) {
        binder = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(this@MainActivity)
        super.onCreate(savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        binder?.imageView?.setOnClickListener(this@MainActivity)
    }

    override fun onClick(view : View?) {
        if (view == binder?.imageView && viewModel.isShowed().not() == true)
            showBottomSheetFragment( OptionBottomSheetDialogFragment.newInstance(this@MainActivity) )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun launchCamera() {
        addToBackStackFragment(R.id.frame_layout, CameraFragment.newInstance())
    }

    override fun launchGallery() { showToast("launch Gallery")
        //val galleryIntent : Intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //startActivityForResult(galleryIntent, GALLERY_MEDIA_CODE)
    }

    override fun onRequestPermissionsGranted(requestCode : Int) {
        super.onRequestPermissionsGranted(requestCode)
        viewModel.checkRequestPermissionsResult(requestCode)
    }
}