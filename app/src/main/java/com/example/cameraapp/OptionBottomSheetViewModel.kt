package com.example.cameraapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class OptionBottomSheetViewModel : AndroidViewModel {

    companion object {
        private val TAG = OptionBottomSheetViewModel::class.java.simpleName
    }

    private var isActive : Boolean = false
    private var currentImagePath : String? = null
    private var currentImageUri : Uri? = null
    private val liveMediaPermission : MutableLiveData<Int> = MutableLiveData()
    private val liveMediaPath : MutableLiveData<String> = MutableLiveData()

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
    //region File Uri and Path
    public fun observePhotoPath() : LiveData<String> = liveMediaPath
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
        //fileName = "${UUID.randomUUID()}_cameraXSample"
        fileName = "${System.currentTimeMillis()}_cameraXSample"

        var fileValue : File
        //fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName,".JPG",filePath)

        currentImagePath = "file:" + fileValue.absolutePath
        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    public fun deletePhoto() {
        liveMediaPath.setValue(null)
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
        } catch (ex : Exception) {
            ex.printStackTrace()
            Log.e(TAG, "getRealPathFromURI Exception : $ex")
            ""
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }

    private fun compressImage(file : File) {
        //https://stackoverflow.com/questions/28760941/compress-image-file-from-camera-to-certain-size
        val bitmap : Bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val byteArrayOutputStream : ByteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
        val byteArray : ByteArray = byteArrayOutputStream.toByteArray()
        Log.d(TAG,"Compressed Image Size ${byteArrayOutputStream.size()}")
        val fileOutputStream : FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file.absolutePath)
            fileOutputStream.write(byteArray)
            fileOutputStream.flush() //to avoid out of memory error
            fileOutputStream.close()
        } catch (ex : IOException) {
            ex.printStackTrace()
        }
    }

    fun checkActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        Log.d(TAG,"checkActivityResult($requestCode,$resultCode,$data)")
        when {
            requestCode == OptionBottomSheetDialogFragment.CAMERA_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Camera
                Log.d(TAG,"CAMERA_MEDIA_CODE")
                Log.d(TAG,"data - $data")
                liveMediaPath.setValue(
                    currentImagePath
                )
            }
            requestCode == OptionBottomSheetDialogFragment.GALLERY_MEDIA_CODE && resultCode == Activity.RESULT_OK -> {
                //Gallery
                Log.d(TAG,"GALLERY_MEDIA_CODE")
                Log.d(TAG,"data Intent - ${data}")
                Log.d(TAG,"data uri - ${data?.getData()}")
                Log.d(TAG,"data path - ${data?.getData()!!.getPath()}")
                Log.d(TAG,"data real path - ${getRealPathFromURI(data?.getData()!!)}")
                liveMediaPath.setValue(
                    getRealPathFromURI(data?.getData()!!)
                )
            }
            else -> {

            }
        }
    }
}