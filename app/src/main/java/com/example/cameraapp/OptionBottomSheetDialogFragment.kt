package com.example.cameraapp

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_option.*

class OptionBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private lateinit var viewModel : OptionBottomSheetViewModel
    private val mainActivity by lazy { activity as MainActivity }

    companion object {
        private val TAG = OptionBottomSheetDialogFragment::class.java.simpleName

        fun newInstance() : OptionBottomSheetDialogFragment {
            return OptionBottomSheetDialogFragment()
        }

        const val FINAL_TAKE_PHOTO = 1
        const val FINAL_CHOOSE_PHOTO = 2
        const val FINAL_CROP_PHOTO = 3
    }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        return inflater.inflate(R.layout.fragment_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_btn_take_photo.setOnClickListener(this)
        tv_btn_choose_photo.setOnClickListener(this)
        tv_btn_edit_photo.setOnClickListener(this)
        tv_btn_delete_photo.setOnClickListener(this)
        tv_btn_cancel_photo.setOnClickListener(this)
    }

    override fun onActivityCreated(savedInstanceState : Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(mainActivity).get(OptionBottomSheetViewModel::class.java)
        viewModel.observeGrantedRequestCode().observe(viewLifecycleOwner, Observer {
                grantedCode -> when(grantedCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE -> launchCamera()
            ManifestPermission.GALLERY_PERMISSION_CODE -> launchImageDocuments()
        }
            viewModel.acknowledgeGrantedRequestCode()
        })
    }

    override fun onClick(view : View) {
        when(view) {
            tv_btn_take_photo -> {
                accessCamera()
            }
            tv_btn_choose_photo -> {
                dismiss()
            }
            tv_btn_edit_photo -> {
                dismiss()
            }
            tv_btn_delete_photo -> {
                dismiss()
            }
            tv_btn_cancel_photo -> {
                dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.setPause()
    }

    public fun accessCamera() {
        ManifestPermission.checkSelfPermission(mainActivity, ManifestPermission.cameraPermission,
            isGranted = { launchCamera() },
            isDenied = { ManifestPermission.requestPermissions(mainActivity,
                ManifestPermission.cameraPermission,
                ManifestPermission.CAMERA_PERMISSION_CODE) }
        )
    }

    public fun accessGallery() {
        ManifestPermission.checkSelfPermission( mainActivity,ManifestPermission.galleryPermissions,
            isGranted = { launchImageDocuments() }, isDenied = {
                ManifestPermission.requestPermissions(mainActivity,
                    ManifestPermission.galleryPermissions,
                    ManifestPermission.GALLERY_PERMISSION_CODE)
            }
        )
    }

    private fun launchCamera() {
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE),FINAL_TAKE_PHOTO)
    }

    private fun launchImageDocuments() {

    }

    public fun launchEditPhoto() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(TAG,"requestCode - $requestCode")
        Log.e(TAG,"resultCode - $resultCode")
        Log.e(TAG,"data - $data")
    }
}