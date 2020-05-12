package com.example.cameraapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.util.*

class OptionBottomSheetViewModel : AndroidViewModel {

    companion object {
        private val TAG = OptionBottomSheetViewModel::class.java.simpleName
    }

    private var isActive : Boolean = false
    private val liveMediaPermission : MutableLiveData<Int> = MutableLiveData()

    constructor(application: Application) : super(application) {

    }
    //region
    fun setResume() { Log.d(TAG,"setResume()")
        isActive = true
    }

    fun setPause() { Log.d(TAG,"setPause()")
        isActive = false
    }

    fun isShowed() : Boolean = isActive
    //endregion
    //region
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
    //endregion
    fun checkActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        //TODO Finish this
        when {
            resultCode == Activity.RESULT_OK && requestCode == OptionBottomSheetDialogFragment.FINAL_TAKE_PHOTO -> {
                //Camera
            }
            resultCode == Activity.RESULT_OK && requestCode == OptionBottomSheetDialogFragment.FINAL_CHOOSE_PHOTO -> {
                //Gallery
            }
            else -> {

            }
        }
    }

    fun createCameraPictureFile() : Uri {
        val packageName : String = getApplication<Application>().applicationContext.packageName
        val authority : String = "$packageName.fileprovider"
        val uriValue : Uri
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                uriValue = FileProvider.getUriForFile(getApplication<Application>(), authority, getImageFile())
            }
            else -> {
                uriValue = Uri.fromFile(getImageFile())
            }
        }
        return uriValue
    }

    private fun getImageFile() : File {
        // This PC\Galaxy J4+\Phone\Android\data\com.example.cameraapp\cache\CameraX
        var cacheDir : File = getApplication<Application>().getCacheDir()
        if (isExternalStorageWritable()) {
            cacheDir = getApplication<Application>().getExternalCacheDir()!!
        }

        var filePath : File
        filePath = File(cacheDir,"CameraX")
        //filePath = Environment.getExternalStorageDirectory().getPath()
        if (!filePath.exists()) {
            filePath.mkdirs()
        }

        var fileName : String
        //fileName = "${UUID.randomUUID()}_cameraXSample.jpg"
        fileName = "${UUID.randomUUID()}_cameraXSample"

        var fileValue : File
        fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName,".JPG",filePath)
        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}