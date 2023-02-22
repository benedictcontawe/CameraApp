package com.example.cameraapp

import android.app.Application
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public class OptionViewModel : BaseAndroidViewModel {

    companion object {
        private val TAG = OptionViewModel::class.java.getSimpleName()
    }

    private var isActive : Boolean = false
    private val liveMediaPermission : MutableSharedFlow<Int>
    private val liveMediaUri : MutableLiveData<Uri?> = MutableLiveData()
    private val liveFilePath : MutableLiveData<String> = MutableLiveData()

    constructor(application : Application) : super(application) {
        liveMediaPermission = MutableSharedFlow()
    }
    //region Life Cycle Aware Methods
    fun setResume() { logDebug(TAG,"setResume()")
        isActive = true
    }

    fun setPause() { logDebug(TAG,"setPause()")
        isActive = false
    }

    fun isShowed() : Boolean = isActive
    //endregion
    //region Granted Request Code Methods
    public fun checkRequestPermissionsResult(requestCode : Int) { Coroutines.io(this@OptionViewModel, {
        if (requestCode == ManifestPermission.CAMERA_PERMISSION_CODE || requestCode == ManifestPermission.VIDEO_RECORD_PERMISSION_CODE || requestCode == ManifestPermission.GALLERY_PERMISSION_CODE) {
            liveMediaPermission.emit(requestCode)
        }
    } ) }

    fun acknowledgeGrantedRequestCode() { Coroutines.io(this@OptionViewModel, {
        liveMediaPermission.emit(ManifestPermission.NIL_PERMISSION_CODE)
    } ) }

    fun observeGrantedRequestCode() : SharedFlow<Int> = liveMediaPermission.asSharedFlow()
    //endregion
    //region Photo Uri
    public fun observePhotoUri() : MutableLiveData<Uri?> = liveMediaUri
    public fun observePhotoPath() : LiveData<String> = liveFilePath
    //endregion
    public fun deletePhoto() {
        liveMediaUri.setValue(null)
    }

    private fun getRealPathFromURI(contentUri : Uri) : String? {
        var cursor : Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = getApplication<Application>().getContentResolver().query(contentUri, proj, null, null, null)
            val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } catch (e : Exception) {
            logError(TAG, "getRealPathFromURI Exception : $e")
            ""
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }
}