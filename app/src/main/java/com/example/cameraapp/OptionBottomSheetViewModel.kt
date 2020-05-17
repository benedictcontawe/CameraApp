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

        private fun isPrimary(type : String) : Boolean {
            return "primary".equals(type, ignoreCase = true)
        }

        private fun isImage(type : String) : Boolean {
            return "image".equals(type)
        }

        private fun isVideo(type : String) : Boolean {
            return "video".equals(type)
        }

        private fun isAudio(type : String) : Boolean {
            return "audio".equals(type)
        }

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        private fun isDocumentUri(application : Application, uri : Uri) : Boolean {
            return DocumentsContract.isDocumentUri(application, uri)
        }

        private fun isContent(uri : Uri) : Boolean {
            return "content".equals(uri.scheme, ignoreCase = true)
        }

        private fun isFile(uri : Uri) : Boolean {
            return "file".equals(uri.scheme, ignoreCase = true)
        }
    }

    private var isActive : Boolean = false
    private var currentImagePath : String? = null
    private var currentImageUri : Uri? = null
    private val liveMediaPermission : MutableLiveData<Int> = MutableLiveData()
    private val liveMediaPath : MutableLiveData<String> = MutableLiveData()

    constructor(application: Application) : super(application) {
        Log.d(TAG,"constructor")
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
        Log.d(TAG,"acknowledgeGrantedRequestCode()")
        liveMediaPermission.setValue(0)
    }

    fun observeGrantedRequestCode() : LiveData<Int> = liveMediaPermission
    //endregion
    //region Observer Method
    public fun observePhotoPath() : LiveData<String> = liveMediaPath
    //endregion
    //region Camera Methods
    fun createCameraPictureFile() : Uri {
        Log.d(TAG,"createCameraPictureFile()")
        val packageName : String = getApplication<Application>().getApplicationContext().getPackageName()
        val authority : String = "$packageName.provider"
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                Log.d(TAG,"Build.VERSION.SDK_INT >= Build.VERSION_CODES.N")
                FileProvider.getUriForFile(getApplication(), authority, getImageFile())
            }
            else -> {
                Log.d(TAG,"else")
                Uri.fromFile(getImageFile())
            }
        }
    }

    private fun getImageFile() : File {
        Log.d(TAG,"getImageFile()")
        var cacheDir : File = getApplication<Application>().getCacheDir()
        if (isExternalStorageWritable()) {
            cacheDir = getApplication<Application>().getExternalCacheDir()!!
        }
        Log.d(TAG,"cacheDir - $cacheDir")

        val filePath : File
        filePath = File(cacheDir,"Photo")
        //filePath = Environment.getExternalStorageDirectory().getPath()
        if (!filePath.exists()) {
            filePath.mkdirs()
        }
        Log.d(TAG,"filePath - $filePath")

        val fileName : String
        fileName = "JPEG_${System.currentTimeMillis()}"
        Log.d(TAG,"fileName - $fileName")

        val fileValue : File
        //fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName,".JPG",filePath)
        Log.d(TAG,"fileValue - $fileValue")

        currentImagePath = "file:" + fileValue.absolutePath
        Log.d(TAG,"currentImagePath - $currentImagePath")
        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        Log.d(TAG,"isExternalStorageWritable() : ${Environment.MEDIA_MOUNTED == state}")
        return Environment.MEDIA_MOUNTED == state
    }
    //endregion
    //region Gallery Methods
    public fun getPathFromURI(uri : Uri) : String? {
        Log.d(TAG,"getPathFromURI($uri)")
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> { //DocumentProvider
                Log.d(TAG,"Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT")
                getKitKatPathFromURI(uri)
            }
            isContent(uri) -> { //MediaStore (and general)
                Log.d(TAG,"isContent($uri)")
                getMediaStorePathFromURI(uri)
            }
            isFile(uri) -> { //File
                Log.d(TAG,"isFile($uri)")
                uri.getPath()
            }
            else -> {
                Log.d(TAG,"else")
                getDataColumn(uri,null,null)
            }
        }.toString()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getKitKatPathFromURI(uri : Uri) : String? {
        Log.d(TAG,"getKitKatPathFromURI($uri)")
        return when {
            isDocumentUri(getApplication(), uri) && isExternalStorageDocument(uri) || isMediaDocument(uri) -> {
                val docId : String = DocumentsContract.getDocumentId(uri)
                val split : Array<String> = docId.split(":").toTypedArray()
                val type : String = split[0]
                val selection : String = "_id=?"
                val selectionArgs : Array<String> = arrayOf(split[1])
                when {
                    isPrimary(type) -> {
                        Log.d(TAG,"isPrimary($type)")
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }
                    isImage(type) -> {
                        Log.d(TAG,"isImage($type)")
                        getDataColumn(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    isVideo(type) -> {
                        Log.d(TAG,"isVideo($type)")
                        getDataColumn(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    isAudio(type) -> {
                        Log.d(TAG,"isAudio($type)")
                        getDataColumn(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            selection, selectionArgs
                        )
                    }
                    else -> {
                        Log.d(TAG,"else")
                        getDataColumn(uri,null,null)
                    }
                }
            }
            isDocumentUri(getApplication(), uri) && isDownloadsDocument(uri) -> {
                Log.d(TAG,"isDocumentUri(getApplication(), uri) && isDownloadsDocument(uri)")
                val id : String = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    Log.d(TAG,"!TextUtils.isEmpty(id)")
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
                    Log.d(TAG,"else")
                    getDataColumn(uri,null,null)
                }
            }
            isContent(uri) -> { //MediaStore (and general)
                Log.d(TAG,"isContent($uri) ${getMediaStorePathFromURI(uri)}")
                getMediaStorePathFromURI(uri)
            }
            isFile(uri) -> { //File
                Log.d(TAG,"isFile($uri) ${uri.getPath()}")
                uri.getPath()
            }
            else -> {
                Log.d(TAG,"else")
                getDataColumn(uri,null,null)
            }
        }
    }

    private fun getMediaStorePathFromURI(uri : Uri) : String? {
        Log.d(TAG,"getMediaStorePathFromURI($uri)")
        return when(isGooglePhotosUri(uri)) {
            true -> {
                Log.d(TAG,"true ${uri.getLastPathSegment()}")
                uri.getLastPathSegment()
            }
            false -> {
                Log.d(TAG,"false ${getDataColumn(uri, null, null)}")
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
            Log.d(TAG,"try")
            cursor = getApplication<Application>().getContentResolver().query(uri, proj, selection, selectionArgs, null)
            val column_index: Int = cursor?.getColumnIndexOrThrow(column)!!
            cursor.moveToFirst()
            Log.d(TAG,"getDataColumn($uri,$selection,$selectionArgs) : ${cursor.getString(column_index)}")
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
    private fun isExternalStorageDocument(uri : Uri) : Boolean {
        Log.d(TAG,"isExternalStorageDocument($uri)")
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author Ferer Atlus
     */
    private fun isDownloadsDocument(uri : Uri) : Boolean {
        Log.d(TAG,"isDownloadsDocument($uri)")
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author Ferer Atlus
     */
    fun isMediaDocument(uri : Uri) : Boolean {
        Log.d(TAG,"isMediaDocument($uri)")
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     * @author Ferer Atlus
     */
    private fun isGooglePhotosUri(uri : Uri) : Boolean {
        Log.d(TAG,"isGooglePhotosUri($uri)")
        return "com.google.android.apps.photos.content" == uri.authority
    }
    //endregion
    //region Edit Methods
    public fun getPickedImage() : Uri {
        Log.d(TAG,"getPickedImage()")
        return when (currentImageUri) {
            null -> {
                Log.d(TAG,"currentImageUri == null")
                Uri.parse(currentImagePath)
                //Uri.fromFile(File(currentImagePath))
            }
            else -> {
                Log.d(TAG,"else")
                currentImageUri
            }
        }!!
    }
    //endregion
    //region Delete Methods
    public fun deletePhoto() {
        Log.d(TAG,"deletePhoto()")
        liveMediaPath.setValue("")
    }
    //endregion
    //region Compress Image
    private fun compressImage(file : File) {
        Log.d(TAG, "compressImage($file)")
        //https://stackoverflow.com/questions/28760941/compress-image-file-from-camera-to-certain-size
        Log.e(TAG,"File Length Before ${file.size}")
        Log.e(TAG,"File size in KB Before ${file.sizeInKb}")
        Log.e(TAG,"File size in MB Before ${file.sizeInMb}")
        try {
            Log.d(TAG, "try")
            val bitmap : Bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val byteArrayOutputStream : ByteArrayOutputStream = ByteArrayOutputStream()
            Log.e(TAG,"Compressing Image Size ${byteArrayOutputStream.size()}")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
            val byteArray : ByteArray = byteArrayOutputStream.toByteArray()
            Log.e(TAG,"Compressed Image Size ${byteArrayOutputStream.size()}")

            val fileOutputStream : FileOutputStream
            fileOutputStream = FileOutputStream(file.absolutePath)
            fileOutputStream.write(byteArray)
            fileOutputStream.flush() //to avoid out of memory error
            fileOutputStream.close()

            Log.e(TAG,"File Length After ${file.size}")
            Log.e(TAG,"File size in KB After ${file.sizeInKb}")
            Log.e(TAG,"File size in MB After ${file.sizeInMb}")
        } catch (ex : IOException) {
            ex.printStackTrace()
            Log.e(TAG, "compressImage IOException : ${ex.message}")
        }
    }

    private fun compressImageToOneMB(file : File) {
        Log.d(TAG, "compressImageToOneMB($file)")
        Log.e(TAG, "File Length Before ${file.size}")
        Log.e(TAG, "File size in KB Before ${file.sizeInKb}")
        Log.e(TAG, "File size in MB Before ${file.sizeInMb}")
        when (file.sizeInMb >= 1.0) {
            true -> {
                Log.e(TAG, "true file.sizeInMb >= 1.0")
                try {
                    Log.d(TAG, "try")
                    val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
                    Log.e(TAG, "Compressing Image Size ${byteArrayOutputStream.size()}")
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 10, byteArrayOutputStream)
                    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                    Log.e(TAG, "Compressed Image Size ${byteArrayOutputStream.size()}")

                    val fileOutputStream: FileOutputStream
                    fileOutputStream = FileOutputStream(file.absolutePath)
                    fileOutputStream.write(byteArray)
                    fileOutputStream.flush() //to avoid out of memory error
                    fileOutputStream.close()

                    Log.e(TAG, "File Length After ${file.size}")
                    Log.e(TAG, "File size in KB After ${file.sizeInKb}")
                    Log.e(TAG, "File size in MB After ${file.sizeInMb}")
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    Log.e(TAG, "compressImage IOException : ${ex.message}")
                }
            }
            false -> {
                Log.e(TAG, "false file.sizeInMb >= 1.0")
            }
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
                Log.e(TAG,"File - ${File(currentImagePath)}")
                Log.e(TAG,"File Length ${File(currentImagePath).size}")
                Log.e(TAG,"File size in KB ${File(currentImagePath).sizeInKb}")
                Log.e(TAG,"File size in MB ${File(currentImagePath).sizeInMb}")
                liveMediaPath.setValue(
                    currentImagePath
                )
            }
            requestCode == OptionBottomSheetDialogFragment.GALLERY_MEDIA_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                //Gallery
                Log.d(TAG,"GALLERY_MEDIA_REQUEST_CODE")
                currentImagePath = getPathFromURI(data?.getData()!!)
                currentImageUri = data?.getData()
                Log.e(TAG,"File - ${File(getPathFromURI(currentImageUri!!))}")
                Log.e(TAG,"File Length ${File(getPathFromURI(currentImageUri!!)).size}")
                Log.e(TAG,"File size in KB ${File(getPathFromURI(currentImageUri!!)).sizeInKb}")
                Log.e(TAG,"File size in MB ${File(currentImagePath).sizeInMb}")
                liveMediaPath.setValue(
                    currentImagePath
                )
            }
            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                //Crop Image
                Log.d(TAG,"CROP_IMAGE_ACTIVITY_REQUEST_CODE")
                val result : CropImage.ActivityResult = CropImage.getActivityResult(data)
                Log.e(TAG,"uri path - ${getPathFromURI(result.uri)}")
                Log.e(TAG,"File - ${File(getPathFromURI(result.uri))}")
                compressImageToOneMB(
                    File(getPathFromURI(result.uri))
                )
                liveMediaPath.setValue(
                    getPathFromURI(result.uri)
                )
            }
            requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                Log.d(TAG,"CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE")
                val result : CropImage.ActivityResult = CropImage.getActivityResult(data);
                Log.e(TAG,"Unrecognized error code ${result.getError()}")
            }
            else -> { }
        }
    }
}