package com.example.cameraapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraapp.databinding.MainBinder
import kotlinx.coroutines.CoroutineScope

public class MainActivity : BaseActivity(), View.OnClickListener, MainListener {

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
    }

    private var binder : MainBinder? = null
    private val optionViewModel : OptionViewModel by lazy { ViewModelProvider(this@MainActivity).get(OptionViewModel::class.java) }
    private val cameraViewModel : CameraViewModel by lazy { ViewModelProvider(this@MainActivity).get(CameraViewModel::class.java) }

    override fun onCreate(savedInstanceState : Bundle?) {
        binder = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        binder?.setViewModel(optionViewModel)
        binder?.setLifecycleOwner(this@MainActivity)
        super.onCreate(savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        binder?.imageView?.setOnClickListener(this@MainActivity)
    }

    override fun onClick(view : View?) {
        if (view == binder?.imageView && optionViewModel.isShowed().not() == true)
            showBottomSheetFragment( OptionBottomSheetDialogFragment.newInstance(this@MainActivity) )
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun launchCamera() {
        //val cameraIntent : Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.createCameraPictureFile())
        //startActivityForResult(cameraIntent, ManifestPermission.CAMERA_PERMISSION_CODE)
        addToBackStackFragment(R.id.frame_layout, CameraFragment.newInstance())
    }

    override fun launchVideo() {
        addToBackStackFragment(R.id.frame_layout, VideoFragment.newInstance())
    }

    override fun launchGallery() { showToast("launch Gallery")
        //TODO: Create Gallery Fragment for Custom
        val galleryIntent : Intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //galleryIntent.setType("image/*")
        //galleryIntent.setAction(Intent.ACTION_GET_CONTENT)
        /*
        startActivityForResult(
            galleryIntent /*Intent.createChooser(galleryIntent, "Select Picture")*/,
            ManifestPermission.GALLERY_PERMISSION_CODE
        )
        */
        galleryLauncher.launch(galleryIntent)
    }

    val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("$TAG PERMISSIONS", "Gallery Launcher result: ${result.resultCode} ${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            super.onActivityResult(ManifestPermission.GALLERY_PERMISSION_CODE, result.resultCode, data)
            //result.getData()?.getData()
        }
    }

    override fun onRequestPermissionsGranted(requestCode : Int) {
        super.onRequestPermissionsGranted(requestCode)
        optionViewModel.checkRequestPermissionsResult(requestCode)
    }

    override fun onRequestPermissionsNeverAskAgain() {
        super.onRequestPermissionsNeverAskAgain()
        ManifestPermission.showRationaleDialog(this@MainActivity,"Go to App Permission Settings?")
    }
}