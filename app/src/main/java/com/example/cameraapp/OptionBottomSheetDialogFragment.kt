package com.example.cameraapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.beep.trade.view.fragments.BaseBottomSheetDialogFragment
import com.example.cameraapp.databinding.OptionBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

public class OptionBottomSheetDialogFragment : BaseBottomSheetDialogFragment {

    companion object {
        private val TAG = OptionBottomSheetDialogFragment::class.java.getSimpleName()

        public fun newInstance(listener : MainListener) : OptionBottomSheetDialogFragment {
            return OptionBottomSheetDialogFragment(listener)
        }
    }

    private var binder : OptionBinder? = null
    private val viewModel : OptionViewModel by lazy { ViewModelProvider(requireActivity()).get(OptionViewModel::class.java) }
    private var listener : MainListener? = null

    constructor() {

    }

    constructor(listener : MainListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(true)
    }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_option, container, false)
        binder?.setViewModel(viewModel)
        binder?.setLifecycleOwner(getViewLifecycleOwner())
        return binder?.root ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override suspend fun onSetObservers(scope : CoroutineScope) {
        binder?.buttonTakePhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.buttonRecordVideo?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.buttonChoosePhoto?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        binder?.buttonCancel?.setOnTouchListener(this@OptionBottomSheetDialogFragment)
        scope.launch( block = { binder?.getViewModel()?.observeGrantedRequestCode()?.collectLatest( action = { grantedCode ->
            if (grantedCode == ManifestPermission.CAMERA_PERMISSION_CODE) {
                listener?.launchCamera()
                dismissNow() //binder?.getViewModel()?.acknowledgeGrantedRequestCode()
            } else if (grantedCode == ManifestPermission.GALLERY_PERMISSION_CODE) {
                listener?.launchGallery()
                dismissNow() //binder?.getViewModel()?.acknowledgeGrantedRequestCode()
            } else if (grantedCode == ManifestPermission.VIDEO_RECORD_PERMISSION_CODE) {
                listener?.launchVideo()
                dismissNow()
            }
        } ) } )
    }

    override fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        return if (isActionUp && isInsideBounds(view) && view == binder?.buttonTakePhoto) {
            onLaunchCamera()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonRecordVideo) {
            onLaunchVideo()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonChoosePhoto) {
            onLaunchGallery()
            true
        } else if (isActionUp && isInsideBounds(view) && view == binder?.buttonCancel) {
            dismissNow()
            true
        } else super.onTouchFragment(view, event)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setResume()
    }

    override fun onPause() {
        viewModel.setPause()
        super.onPause()
    }

    private fun onLaunchCamera() {
        ManifestPermission.checkSelfPermission(
            requireContext(), ManifestPermission.cameraPermission,
            isGranted = {
                this.listener?.launchCamera()
                dismissNow()
            }, isDenied = {
                ManifestPermission.requestPermissions(
                    requireActivity(),
                    ManifestPermission.cameraPermission,
                    ManifestPermission.CAMERA_PERMISSION_CODE
                )
            }
        )
    }

    private fun onLaunchVideo() {
        ManifestPermission.checkSelfPermission(
            requireContext(), ManifestPermission.videoRecordPermission,
            isGranted = {
                this.listener?.launchVideo()
                dismissNow()
            }, isDenied = {
                ManifestPermission.requestPermissions(
                    requireActivity(),
                    ManifestPermission.videoRecordPermission,
                    ManifestPermission.VIDEO_RECORD_PERMISSION_CODE
                )
            }
        )
    }

    private fun onLaunchGallery() {
        ManifestPermission.checkSelfPermission(
                requireContext(), ManifestPermission.galleryPermissions,
                isGranted = {
                    this.listener?.launchGallery()
                    dismissNow()
                }, isDenied = {
                    ManifestPermission.requestPermissions(
                        requireActivity(),
                        ManifestPermission.galleryPermissions,
                        ManifestPermission.GALLERY_PERMISSION_CODE
                )
            }
        )
    }
}