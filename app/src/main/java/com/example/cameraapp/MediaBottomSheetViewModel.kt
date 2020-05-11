package com.example.cameraapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MediaBottomSheetViewModel : ViewModel() {

    companion object {
        private val TAG = MediaBottomSheetViewModel::class.java.simpleName
    }

    private var isActive : Boolean = false
    private val liveMediaPermission : MutableLiveData<Int> = MutableLiveData()

    fun setResume() {
        Log.d(TAG,"setResume()")
        isActive = true
    }

    fun setPause() {
        Log.d(TAG,"setPause()")
        isActive = false
    }

    fun isShowed() : Boolean = isActive

    fun setGrantedRequestCode(grantedRequestCode : Int) {
        Log.d(TAG,"setGrantedRequestCode($grantedRequestCode)")
        when(grantedRequestCode) {
            ManifestPermission.CAMERA_PERMISSION_CODE, ManifestPermission.GALLERY_PERMISSION_CODE -> {
                liveMediaPermission.setValue(grantedRequestCode)
            }
        }
    }

    fun acknowledgeGrantedRequestCode() {
        liveMediaPermission.setValue(0)
    }

    fun observeGrantedRequestCode() : LiveData<Int> = liveMediaPermission
}