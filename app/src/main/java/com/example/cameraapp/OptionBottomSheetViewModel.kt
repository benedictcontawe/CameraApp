package com.example.cameraapp

import android.app.Application
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.*

class OptionBottomSheetViewModel : AndroidViewModel {

    companion object {
        private val TAG = OptionBottomSheetViewModel::class.java.getSimpleName()
    }

    private var isActive : Boolean = false
    private var currentImagePath : String? = null
    private val liveMediaPermission : MutableSharedFlow<Int>
    private val liveMediaUri : MutableLiveData<Uri?> = MutableLiveData()
    private val liveFilePath : MutableLiveData<String> = MutableLiveData()

    constructor(application: Application) : super(application) {
        liveMediaPermission = MutableSharedFlow()
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
    public fun checkRequestPermissionsResult(requestCode : Int) { Coroutines.io(this@OptionBottomSheetViewModel, {
        if (requestCode == ManifestPermission.CAMERA_PERMISSION_CODE || requestCode == ManifestPermission.GALLERY_PERMISSION_CODE) {
            liveMediaPermission.emit(requestCode)
        }
    } ) }

    fun acknowledgeGrantedRequestCode() { Coroutines.io(this@OptionBottomSheetViewModel, {
        liveMediaPermission.emit(ManifestPermission.NIL_PERMISSION_CODE)
    } ) }

    fun observeGrantedRequestCode() : SharedFlow<Int> = liveMediaPermission.asSharedFlow()
    //endregion
    //region Photo Uri
    public fun observePhotoUri() : MutableLiveData<Uri?> = liveMediaUri
    public fun observePhotoPath() : LiveData<String> = liveFilePath
    //endregion
    fun createCameraPictureFile() : Uri {
        val packageName : String = getApplication<Application>().getApplicationContext().getPackageName()
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
        val cacheDir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getCacheDir()
            else getApplication<Application>().getExternalCacheDir()!!

        val filePath : File
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

        currentImagePath = "file:" + fileValue.getAbsolutePath()
        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    public fun deletePhoto() {
        liveMediaUri.setValue(null)
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

    /*fun checkActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        Log.d(TAG,"checkActivityResult($requestCode,$resultCode,$data)")
        when {
            requestCode == OptionBottomSheetDialogFragment.CAMERA_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Camera
                Log.d(TAG,"CAMERA_MEDIA_CODE")
                Log.d(TAG,"data Intent - ${data}")
                Log.d(TAG,"data uri - ${data?.getData()}")
                Log.d(TAG,"data path - ${data?.getData()?.getPath()}")
                liveMediaUri.setValue(
                    Uri.parse(currentImagePath)
                )
            }
            requestCode == OptionBottomSheetDialogFragment.GALLERY_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Gallery
                Log.d(TAG,"GALLERY_MEDIA_CODE")
                Log.d(TAG,"data Intent - ${data}")
                Log.d(TAG,"data uri - ${data?.getData()}")
                Log.d(TAG,"data path - ${data?.getData()?.getPath()}")
                liveMediaUri.setValue(
                    data?.getData()
                )
            }
            else -> {

            }
        }
    }*/
}