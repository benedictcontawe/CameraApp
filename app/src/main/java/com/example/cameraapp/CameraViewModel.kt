package com.example.cameraapp

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.FileProvider
import androidx.core.util.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executor

public class CameraViewModel : BaseAndroidViewModel {

    companion object {
        private val TAG = CameraViewModel::class.java.getSimpleName()
    }

    private val audio : AudioManager
    public val imageCapture : ImageCapture by lazy { ImageCapture.Builder().build() }
    public val preview : Preview by lazy { Preview.Builder().build() }

    private val isRecording : MutableStateFlow<Boolean?>
    public val recorder : Recorder by lazy { Recorder.Builder().setQualitySelector(getQualitySelector()).build() }
    public var recording : Recording? = null
    public val videoCapture : VideoCapture<Recorder> by lazy { VideoCapture.withOutput(recorder) }

    public var cameraProvider : ProcessCameraProvider? = null
    public var lensFacing : Int = CameraSelector.LENS_FACING_BACK
    private val vibrator : Vibrator
    private val vibratorManager : VibratorManager?

    constructor(application : Application) : super(application) {
        isRecording = MutableStateFlow(null)
        audio = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.getDefaultVibrator()
        } else {
            vibratorManager = null
            vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    //region Image and Video Methods
    public fun getCameraSelector() : CameraSelector {
        return CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    public fun flipCamera() { Coroutines.io(this@CameraViewModel, {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) lensFacing = CameraSelector.LENS_FACING_BACK
        else if (lensFacing == CameraSelector.LENS_FACING_BACK) lensFacing = CameraSelector.LENS_FACING_FRONT
    } ) }
    //endregion
    //region Image Methods
    public fun playShutter() { Coroutines.io(this@CameraViewModel, {
        if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            val sound : MediaActionSound = MediaActionSound()
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }
    } ) }

    public fun playVibrate() { Coroutines.io(this@CameraViewModel, {
        val isAllowed : Boolean = audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL || audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAllowed)
            vibrator.vibrate(VibrationEffect.createWaveform(Constants.VIBRATE_PATTERN,Constants.VIBRATE_ONCE))
        else if(isAllowed) vibrator.vibrate(Constants.VIBRATE_PATTERN,Constants.VIBRATE_ONCE)
    } ) }
    //endregion
    //region Video Methods
    public fun toggleRecording() { Coroutines.io(this@CameraViewModel, work = {
        logDebug(TAG,"toggleRecording")
        if (isRecording.value == true) isRecording.emit(false)
        else isRecording.emit(true)
    } ) }

    public fun observeRecording() : StateFlow<Boolean?> {
        return isRecording.asStateFlow()
    }

    private fun getQualitySelector() : QualitySelector {
        return QualitySelector.fromOrderedList( listOf (
            Quality.UHD,
            Quality.FHD,
            Quality.HD,
            Quality.SD
        ), FallbackStrategy.lowerQualityOrHigherThan (
            Quality.SD
        ) )
    }

    public fun getResolutions(selector : CameraSelector, provider : ProcessCameraProvider) : Map<Quality, Size> {
        return selector.filter(provider.availableCameraInfos).firstOrNull()?.let { camInfo ->
            QualitySelector.getSupportedQualities(camInfo).associateWith { quality ->
                QualitySelector.getResolution(camInfo, quality)!!
            }
        } ?: emptyMap()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setRecording(contentResolver : ContentResolver, contentValues : ContentValues) : PendingRecording {
        return videoCapture.getOutput()
            .prepareRecording(getApplication(), getMediaStoreOutputOptions(contentResolver, contentValues))
            .withAudioEnabled()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public fun startRecording(contentResolver : ContentResolver, contentValues : ContentValues, listenerExecutor : Executor, listener : Consumer<VideoRecordEvent> ) {
        recording = setRecording(contentResolver, contentValues).start(listenerExecutor, listener)
    }

    public fun pauseRecording() {
        recording?.pause()
    }

    public fun resumeRecording() {
        recording?.resume()
    }

    public fun stopRecording() {
        recording?.stop()
    }

    public fun getRecordingListener() : Consumer<VideoRecordEvent> {
        return object : Consumer<VideoRecordEvent> {
            override fun accept(event : VideoRecordEvent?) {
                if (event is VideoRecordEvent.Start) {
                    logDebug(TAG, "Video Record Event Start")
                } else if (event is VideoRecordEvent.Finalize) {
                    logDebug(TAG, "Video Record Event Finalize")
                } else if (event is VideoRecordEvent.Resume) {
                    logDebug(TAG, "Video Record Event Resume")
                } else if (event is VideoRecordEvent.Pause) {
                    logDebug(TAG, "Video Record Event Pause")
                } else if (event is VideoRecordEvent.Status) {
                    logDebug(TAG, "Video Record Event Status")
                } else {
                    logDebug(TAG, "Video Record Event else")
                }
            }
        }
    }
    //endregion
    //region Saving Media Files Methods
    fun getFileExt(fileName : String) : String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }

    public fun createCameraPictureFile(suffix : String) : Uri {
        val packageName : String = getApplication<Application>().getApplicationContext().getPackageName()
        val authority : String = "$packageName.fileprovider"
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                FileProvider.getUriForFile(getApplication(), authority, getCacheFile(suffix))
            else -> Uri.fromFile(getCacheFile(suffix))
        }
    }

    public fun getFile() : File {
        val dir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getFilesDir()
            else getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        val filePathFolder : File = File(dir,getString(R.string.camerax))
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String = "${System.currentTimeMillis()}${getString(R.string._camera_x_sample)}"

        val fileValue : File
        fileValue = File.createTempFile(fileName, getString(R.string._jpg), filePathFolder)

        return fileValue
    }

    public fun getCacheFile(suffix : String) : File {
        //This PC\Benedict's Galaxy J4+\Phone\Android\data\com.example.cameraapp\cache\CameraX
        val cacheDir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getCacheDir()
            else getApplication<Application>().getExternalCacheDir()!!

        val filePathFolder : File = File(cacheDir,getString(R.string.camerax))
        //filePathFolder = Environment.getExternalStorageDirectory().getPath()
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String
        //fileName = "${UUID.randomUUID()}${getString(R.string._camera_x_sample)}.jpg"
        //fileName = "${UUID.randomUUID()}${getString(R.string._camera_x_sample)}"
        fileName = "${System.currentTimeMillis()}${getString(R.string._camera_x_sample)}"

        val fileValue : File
        //fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName, suffix, filePathFolder)

        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun getUri(file : File) : Uri {
        return Uri.fromFile(file)
    }

    public fun getOutputFileOptions(suffix : String) : ImageCapture.OutputFileOptions {
        return ImageCapture.OutputFileOptions.Builder(
            getCacheFile(suffix)
        ).build()
    }

    public fun logImageSaved(output : ImageCapture.OutputFileResults) {
        logDebug(TAG,"logImageSaved ${output.getSavedUri()}")
    }

    public fun getMediaStoreOutputOptions(contentResolver : ContentResolver, contentValues : ContentValues) : MediaStoreOutputOptions{
        return MediaStoreOutputOptions.Builder(contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
    }

    public fun getContentValues(name : String?, path : String?) : ContentValues {
        // This PC\Benedict's Galaxy J4+\Phone\video
        val values : ContentValues = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name ?: "${System.currentTimeMillis()}${getString(R.string._camera_x_sample)}")
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path ?: Environment.DIRECTORY_RECORDINGS)
        }
        return values
    }
    //endregion
    override fun onCleared() {
        recording?.close()
        super.onCleared()
    }
}