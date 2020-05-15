package com.example.cameraapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
    private val liveMediaUri : MutableLiveData<Uri> = MutableLiveData()
    private val liveFilePath : MutableLiveData<String> = MutableLiveData()

    constructor(application: Application) : super(application) {

    }
    //region Life Cycle Aware Methods
    fun setResume() { Log.d(TAG,"setResume()")
        isActive = true
    }

    fun setPause() { Log.d(TAG,"setPause()")
        isActive = false
    }

    fun isShowed() : Boolean = isActive
    //endregion
    //region Granted Request Code Methods
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
    //region Photo Uri
    public fun observePhotoUri() : LiveData<Uri> = liveMediaUri
    public fun observePhotoPath() : LiveData<String> = liveFilePath
    //endregion
    fun createCameraPictureFile() : Uri {
        val packageName : String = getApplication<Application>().applicationContext.packageName
        val authority : String = "$packageName.fileprovider"
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                FileProvider.getUriForFile(getApplication(), authority, getImageFile())
            }
            else -> {
                Uri.fromFile(getImageFile())
            }
        }
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

    private fun getImagePath(uri : Uri) : String {
        uri.getPath()
        return ""
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun getRealPathFromURI(contentUri : Uri) : String? {
        var cursor : Cursor? = null
        return try {
            val proj =
                arrayOf(MediaStore.Images.Media.DATA)
            cursor = getApplication<Application>().getContentResolver().query(contentUri, proj, null, null, null)
            val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(column_index)
        } catch (e: Exception) {
            Log.e(TAG, "getRealPathFromURI Exception : $e")
            ""
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }

    fun checkActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        Log.d(TAG,"checkActivityResult($requestCode,$resultCode,$data)")
        when {
            requestCode == OptionBottomSheetDialogFragment.CAMERA_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Camera
                Log.d(TAG,"CAMERA_MEDIA_CODE")
                Log.d(TAG,"data - $data")
                //liveMediaUri.setValue(data?.getData())
            }
            requestCode == OptionBottomSheetDialogFragment.GALLERY_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Gallery
                Log.d(TAG,"GALLERY_MEDIA_CODE")
                Log.d(TAG,"data Intent - ${data}")
                Log.d(TAG,"data uri - ${data?.getData()}")
                Log.d(TAG,"data path - ${data?.getData()!!.getPath()}")
                //liveMediaUri.setValue(data?.getData())
                liveFilePath.setValue(
                    getRealPathFromURI(data?.getData()!!)
                )
            }
            else -> {

            }
        }
    }
}