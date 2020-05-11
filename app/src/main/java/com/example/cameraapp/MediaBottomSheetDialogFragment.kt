package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MediaBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel : MediaBottomSheetViewModel
    private val mainActivity by lazy { activity as MainActivity }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        return inflater.inflate(R.layout.fragment_option, container, false)
    }

    override fun onActivityCreated(savedInstanceState : Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(mainActivity).get(MediaBottomSheetViewModel::class.java)
        viewModel.observeGrantedRequestCode().observe(viewLifecycleOwner, Observer {
                grantedCode -> when(grantedCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE -> launchCamera()
            ManifestPermission.GALLERY_PERMISSION_CODE -> launchImageDocuments()
        }
            viewModel.acknowledgeGrantedRequestCode()
        })
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
        ManifestPermission.checkSelfPermission(mainActivity,ManifestPermission.galleryPermissions,
            isGranted = { launchImageDocuments() }, isDenied = {
                ManifestPermission.requestPermissions(mainActivity,
                    ManifestPermission.galleryPermissions,
                    ManifestPermission.GALLERY_PERMISSION_CODE)
            }
        )
    }

    public fun launchCamera() {

    }

    private fun launchImageDocuments() {

    }

    public fun launchEditPhoto() {

    }
}