package com.example.cameraapp

import android.app.Activity
import android.app.Application
import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.theartofdev.edmodo.cropper.CropImage
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
    //region Camera Methods
    public fun createCameraPictureFile() : Uri {
        val packageName : String = getApplication<Application>().getApplicationContext().getPackageName()
        val authority : String = "$packageName.provider"
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
        Log.d(TAG,"cacheDir - $cacheDir")

        val filePath : File
        filePath = File(cacheDir,"CameraX")
        //filePath = Environment.getExternalStorageDirectory().getPath()
        if (!filePath.exists()) {
            filePath.mkdirs()
        }
        Log.d(TAG,"filePath - $filePath")

        val fileName : String
        //fileName = "${UUID.randomUUID()}_cameraXSample.jpg"
        //fileName = "${UUID.randomUUID()}_cameraXSample"
        fileName = "${System.currentTimeMillis()}_cameraXSample"
        Log.d(TAG,"fileName - $fileName")

        val fileValue : File
        //fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName,".JPG",filePath)
        Log.d(TAG,"fileName - $fileValue")

        currentImagePath = "file:" + fileValue.absolutePath
        Log.d(TAG,"currentImagePath - $currentImagePath")
        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
    //endregion
    //region Gallery Methods
    private fun getPathFromURI(uri : Uri) : String? {
        val isFile : Boolean = "file".equals(uri.scheme, ignoreCase = true)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> { //DocumentProvider
                getKitKatPathFromURI(uri)
            }
            "content".equals(uri.scheme, ignoreCase = true) -> { //MediaStore (and general)
                getMediaStorePathFromURI(uri)
            }
            isFile -> { //File
                uri.getPath()
            }
            else -> {
                getDataColumn(uri,null,null)
            }
        }.toString()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getKitKatPathFromURI(uri : Uri) : String? {
        val isDocumentUri : Boolean = DocumentsContract.isDocumentUri(getApplication(), uri)
        val isFile : Boolean = "file".equals(uri.scheme, ignoreCase = true)
        return when {
            isDocumentUri && isExternalStorageDocument(uri) || isMediaDocument(uri) -> {
                val docId : String = DocumentsContract.getDocumentId(uri)
                val split : Array<String> = docId.split(":").toTypedArray()
                val type : String = split[0]
                val selection : String = "_id=?"
                val selectionArgs : Array<String> = arrayOf(split[1])
                when {
                    "primary".equals(type, ignoreCase = true) -> {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                    "image".equals(type) -> {
                        getDataColumn(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    "video".equals(type) -> {
                        getDataColumn(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    "audio".equals(type) -> {
                        getDataColumn(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    else -> {
                        getDataColumn(uri,null,null)
                    }
                }
            }
            isDocumentUri && isDownloadsDocument(uri) -> {
                val id : String = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    try {
                        getDataColumn(
                            ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                java.lang.Long.valueOf(id)
                            ),
                            null,
                            null
                        )
                    } catch (ex : NumberFormatException) {
                        ex.printStackTrace()
                        Log.e(TAG, "getKitKatPathFromURI NumberFormatException : ${ex.message}")
                        null
                    }
                } else {
                    getDataColumn(uri,null,null)
                }
            }
            isFile -> { //File
                uri.getPath()
            }
            else -> {
                getDataColumn(uri,null,null)
            }
        }
    }

    private fun getMediaStorePathFromURI(uri : Uri) : String? {
        return when(isGooglePhotosUri(uri)) {
            true -> {
                uri.getLastPathSegment()
            }
            false -> {
                getDataColumn(uri, null, null)
            }
        }
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Ferer Atlus
     */
    private fun getDataColumn(uri : Uri, selection : String?, selectionArgs : Array<String>?) : String? {
        var cursor : Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val proj = arrayOf(column)
        return try {
            cursor = getApplication<Application>().getContentResolver().query(uri, proj, selection, selectionArgs, null)
            val column_index: Int = cursor?.getColumnIndexOrThrow(column)!!
            cursor.moveToFirst()
            cursor.getString(column_index)
        } catch (ex : Exception) {
            ex.printStackTrace()
            Log.e(TAG, "getDataColumn Exception : ${ex.message}")
            ""
        } catch (ex : IllegalArgumentException) {
            Log.e(TAG, "getDataColumn IllegalArgumentException : ${ex.message}")
            ""
        } finally {
            cursor?.close()
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author Ferer Atlus
     */
    private fun isExternalStorageDocument(uri : Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author Ferer Atlus
     */
    private fun isDownloadsDocument(uri : Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author Ferer Atlus
     */
    fun isMediaDocument(uri : Uri) : Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     * @author Ferer Atlus
     */
    private fun isGooglePhotosUri(uri : Uri) : Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
    //endregion
    //region Edit Methods
    public fun getPickedImage() : Uri {
        return when (currentImageUri) {
            null -> {
                Uri.parse(currentImagePath)
            }
            else -> {
                currentImageUri
            }
        }!!
    }
    //endregion
    //region Delete Methods
    public fun deletePhoto() {
        liveMediaPath.setValue("")
    }
    //endregion
    //region Compress Image
    private fun compressImage(file : File) {
        //https://stackoverflow.com/questions/28760941/compress-image-file-from-camera-to-certain-size
        try {
            val bitmap : Bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val byteArrayOutputStream : ByteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
            val byteArray : ByteArray = byteArrayOutputStream.toByteArray()
            Log.d(TAG,"Compressed Image Size ${byteArrayOutputStream.size()}")
            val fileOutputStream : FileOutputStream

            fileOutputStream = FileOutputStream(file.absolutePath)
            fileOutputStream.write(byteArray)
            fileOutputStream.flush() //to avoid out of memory error
            fileOutputStream.close()
        } catch (ex : IOException) {
            ex.printStackTrace()
            Log.e(TAG, "compressImage IOException : ${ex.message}")
        }
    }
    //endregion
    fun checkActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        Log.d(TAG,"checkActivityResult($requestCode,$resultCode,$data)")
        Log.d(TAG,"requestCode - $requestCode")
        Log.d(TAG,"resultCode - $resultCode")
        Log.d(TAG,"data Intent - $data")
        Log.d(TAG,"data uri - ${data?.getData()}")
        Log.d(TAG,"data path - ${data?.getData()?.getPath()}")
        when {
            requestCode == OptionBottomSheetDialogFragment.CAMERA_MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                //Camera
                Log.d(TAG,"CAMERA_MEDIA_REQUEST_CODE")
                currentImageUri = null
                //Log.d(TAG,"File ${File(currentImagePath)}")
                //compressImage(File(currentImagePath))
                liveMediaPath.setValue(
                    currentImagePath
                )
            }
            requestCode == OptionBottomSheetDialogFragment.GALLERY_MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                //Gallery
                Log.d(TAG,"GALLERY_MEDIA_REQUEST_CODE")
                currentImagePath = getPathFromURI(data?.getData()!!)
                currentImageUri = data?.getData()
                liveMediaPath.setValue(
                    currentImagePath
                )
            }
            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                //Crop Image
                Log.d(TAG,"CROP_IMAGE_ACTIVITY_REQUEST_CODE")
                val result : CropImage.ActivityResult = CropImage.getActivityResult(data);
                liveMediaPath.setValue(
                    getPathFromURI(result.uri)
                )
            }
            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                val result : CropImage.ActivityResult = CropImage.getActivityResult(data);
                Log.e(TAG,"Unrecognized error code ${result.getError()}")
            }
            else -> { }
        }
    }
}