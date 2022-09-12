package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.beep.trade.view.fragments.BaseBottomSheetDialogFragment
import com.example.cameraapp.databinding.OptionBinder
import kotlinx.coroutines.CoroutineScope

class OptionBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private val mainActivity by lazy { activity as MainActivity }
    private val viewModel : OptionBottomSheetViewModel by lazy { ViewModelProvider(requireActivity()).get(OptionBottomSheetViewModel::class.java) }
    private var binder : OptionBinder? = null

    companion object {
        private val TAG = OptionBottomSheetDialogFragment::class.java.getSimpleName()

        fun newInstance() : OptionBottomSheetDialogFragment {
            return OptionBottomSheetDialogFragment()
        }
    }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_option, container, false)
        binder?.setLifecycleOwner(getViewLifecycleOwner())
        return binder?.root ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binder?.textViewTakePhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.textViewChoosePhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.textViewEditPhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.textViewDeletePhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.textViewCancel?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        viewModel?.observeGrantedRequestCode()?.observe(viewLifecycleOwner, Observer {
                grantedCode -> when(grantedCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE -> {
                launchCamera()
                viewModel?.acknowledgeGrantedRequestCode()
            }
            ManifestPermission.GALLERY_PERMISSION_CODE -> {
                launchGallery()
                viewModel?.acknowledgeGrantedRequestCode()
            }
        }
        })
    }

    override fun onTouchFragment(view: View, event: MotionEvent): Boolean {
        return if (isActionUp && isInsideBounds(view) && view == binder?.textViewTakePhoto) {
            onLaunchCamera()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.textViewChoosePhoto) {
            onLaunchGallery()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.textViewEditPhoto) {
            launchEditPhoto()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.textViewDeletePhoto) {
            //viewModel?.deletePhoto()
            dismiss()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.textViewCancel) {
            dismiss()
            true
        } else super.onTouchFragment(view, event)
    }

    override fun onResume() {
        super.onResume()
        viewModel?.setResume()
    }

    override fun onPause() {
        viewModel?.setPause()
        super.onPause()
    }

    private fun onLaunchCamera() {
        ManifestPermission.checkSelfPermission(mainActivity, ManifestPermission.cameraPermission,
            isGranted = { launchCamera() },
            isDenied = { ManifestPermission.requestPermissions(mainActivity,
                ManifestPermission.cameraPermission,
                ManifestPermission.CAMERA_PERMISSION_CODE) }
        )
    }

    private fun onLaunchGallery() {
        ManifestPermission.checkSelfPermission( mainActivity,ManifestPermission.galleryPermissions,
            isGranted = { launchGallery() }, isDenied = {
                ManifestPermission.requestPermissions(mainActivity,
                    ManifestPermission.galleryPermissions,
                    ManifestPermission.GALLERY_PERMISSION_CODE)
            }
        )
    }

    private fun launchCamera() {
        //val cameraIntent : Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel?.createCameraPictureFile())
        //startActivityForResult(cameraIntent, CAMERA_MEDIA_CODE)
    }

    private fun launchGallery() {
        //val galleryIntent : Intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        //startActivityForResult(galleryIntent, GALLERY_MEDIA_CODE)
    }

    public fun launchEditPhoto() {
        //TODO: Use Camera X Feature
        //startActivityForResult(editIntent, CROP_MEDIA_CODE)
        //mainActivity.showToast("Edit Mode Under Construction")
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(TAG,"requestCode - $requestCode")
        Log.e(TAG,"resultCode - $resultCode")
        Log.e(TAG,"data - $data")
        viewModel?.checkActivityResult(requestCode,resultCode,data)
    }*/
}