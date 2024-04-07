package com.example.cameraapp

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executor

class CameraViewModel : BaseAndroidViewModel {
    companion object {
        private val TAG = CameraViewModel::class.java.getSimpleName()
    }

    public var keepSplashAlive  : Boolean
    private var liveCameraGranted : MutableLiveData<Boolean>
    private var liveVideoGranted : MutableLiveData<Boolean>

    private val audio : AudioManager

    private val isRecording : MutableStateFlow<Boolean>
    public var recorder : Recorder? = null
    public var recording : Recording? = null
    public var videoCapture : VideoCapture<Recorder>? = null

    //public var cameraProvider : ProcessCameraProvider? = null
    public var lensFacing : Int = CameraSelector.LENS_FACING_FRONT ?: CameraSelector.LENS_FACING_BACK
    private val vibrator : Vibrator
    private val vibratorManager : VibratorManager?

    constructor(application : Application) : super(application) {
        keepSplashAlive = true
        liveCameraGranted = MutableLiveData<Boolean>()
        liveVideoGranted = MutableLiveData<Boolean>()
        isRecording = MutableStateFlow(false)
        audio = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.getDefaultVibrator()
        } else {
            vibratorManager = null
            vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        keepSplashAlive = false
    }
    //region Camera Permission Methods
    public fun checkCameraPermission(permissionResultResultLauncher: ActivityResultLauncher<String>) {
        ManifestPermission.checkSelfPermission (
            getApplication<Application>(),
            ManifestPermission.cameraPermission,
            isGranted = {
                grantedCameraPermission()
            },
            isDenied = {
                deniedCameraPermission()
                ManifestPermission.requestPermission(
                    permissionResultResultLauncher,
                    ManifestPermission.cameraPermission
                )
            }
        )
    }

    public fun checkVideoPermission() : Boolean {
        return ManifestPermission.checkSelfPermission(getApplication<Application>(), ManifestPermission.microphonePermission.first())
    }

    public fun checkVideoPermission(permissionResultResultLauncher: ActivityResultLauncher<Array<String>>) {
        ManifestPermission.checkSelfPermission (
            getApplication<Application>(),
            ManifestPermission.videoRecordPermission,
            isGranted = {
                grantedVideoPermission()
            },
            isDenied = {
                deniedVideoPermission()
                ManifestPermission.requestPermission(
                    permissionResultResultLauncher,
                    ManifestPermission.videoRecordPermission
                )
            }
        )
    }

    public fun grantedCameraPermission() {
        liveCameraGranted.setValue(true)
    }

    public fun deniedCameraPermission() {
        liveCameraGranted.setValue(false)
    }

    public fun grantedVideoPermission() {
        liveVideoGranted.setValue(true)
    }

    public fun deniedVideoPermission() {
        liveVideoGranted.setValue(false)
    }

    public fun observeCameraPermission() : LiveData<Boolean> {
        return liveCameraGranted
    }

    public fun observeVideoPermission() : LiveData<Boolean> {
        return liveVideoGranted
    }
    //endregion
    //region Navigation Route Methods
    public fun getMainRoute() : String {
        return getString(R.string.camerax)
    }

    public fun getCameraRoute() : String {
        return getString(R.string.take_photo)
    }

    public fun getVideoRoute() : String {
        return getString(R.string.record_video)
    }
    //endregion
    //region Image and Video Methods
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun getCameraSelector() : CameraSelector {
        return CameraSelector.Builder().requireLensFacing(lensFacing).build()
    }

    public fun flipCamera() { Coroutines.io(this@CameraViewModel, {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) lensFacing = CameraSelector.LENS_FACING_FRONT
        else if (lensFacing == CameraSelector.LENS_FACING_FRONT) lensFacing = CameraSelector.LENS_FACING_BACK
    } ) }

    public fun playVibrate() { Coroutines.io(this@CameraViewModel, {
        val isAllowed : Boolean = audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL || audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAllowed)
            vibrator.vibrate(VibrationEffect.createWaveform(Constants.VIBRATE_PATTERN,Constants.VIBRATE_ONCE))
        else if(isAllowed) vibrator.vibrate(Constants.VIBRATE_PATTERN,Constants.VIBRATE_ONCE)
    } ) }
    //endregion
    //region Image Methods
    public fun playShutter() { Coroutines.io(this@CameraViewModel, {
        if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            val sound : MediaActionSound = MediaActionSound()
            sound.play(MediaActionSound.SHUTTER_CLICK);
        }
    } ) }
    //endregion
    //region Video Methods
    public fun toggleRecording() { Coroutines.io(this@CameraViewModel, work = {
        Log.d(TAG,"toggleRecording")
        if (isRecording.value == true) isRecording.emit(false)
        else isRecording.emit(true)
    } ) }

    public fun playRecording() { Coroutines.io(this@CameraViewModel, {
        Log.d(TAG,"toggleRecording")
        val sound : MediaActionSound = MediaActionSound()
        if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL && isRecording.value == true) {
            sound.play(MediaActionSound.START_VIDEO_RECORDING);
        } else if (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            sound.play(MediaActionSound.STOP_VIDEO_RECORDING);
        }
    } ) }

    public fun observeRecording() : StateFlow<Boolean> {
        return isRecording.asStateFlow()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun getResolutions(selector : CameraSelector, provider : ProcessCameraProvider) : Map<Quality, Size> {
        return selector.filter(provider.availableCameraInfos).firstOrNull()?.let { camInfo ->
            QualitySelector.getSupportedQualities(camInfo).associateWith { quality ->
                QualitySelector.getResolution(camInfo, quality)!!
            }
        } ?: emptyMap()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun flipRecorder() {
        recorder = null
        videoCapture = null
        recorder = Recorder.Builder().setQualitySelector(getQualitySelector()).build()
        videoCapture = VideoCapture.withOutput(recorder!!)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setRecording(contentResolver : ContentResolver, contentValues : ContentValues) : PendingRecording {
        return videoCapture?.getOutput()
            ?.prepareRecording(getApplication(), getMediaStoreOutputOptions(contentResolver, contentValues))
            ?.withAudioEnabled()!!
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public fun startRecording(recording : Recording) {
        if (this.recording != null) {
            this.recording?.stop()
            this.recording = null
        }
        this.recording = recording
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun pauseRecording() {
        recording?.pause()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun resumeRecording() {
        recording?.resume()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun stopRecording() {
        recording?.stop()
        this.recording = null
    }

    public fun getRecordingListener() : Consumer<VideoRecordEvent> {
        return object : Consumer<VideoRecordEvent> {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun accept(event : VideoRecordEvent?) {
                if (event is VideoRecordEvent.Start) {
                    Log.d(TAG, "Video Record Event Start")
                } else if (event is VideoRecordEvent.Finalize) {
                    Log.d(TAG, "Video Record Event Finalize")
                } else if (event is VideoRecordEvent.Resume) {
                    Log.d(TAG, "Video Record Event Resume")
                } else if (event is VideoRecordEvent.Pause) {
                    Log.d(TAG, "Video Record Event Pause")
                } else if (event is VideoRecordEvent.Status) {
                    Log.d(TAG, "Video Record Event Status")
                } else {
                    Log.d(TAG, "Video Record Event else")
                }
            }
        }
    }
    //endregion
    //region File Storage Methods
    fun getFileExt(fileName : String) : String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun getOutputFileOptions(suffix : String?) : ImageCapture.OutputFileOptions {
        return ImageCapture.OutputFileOptions.Builder (
            getCacheFile(suffix ?: Constants.IMAGE_EXTENSION)
        ).build()
    }

    public fun getFile() : File {
        val dir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getFilesDir()
            else getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        val filePathFolder : File = File(dir,String.format(getString(R.string.camerax)))
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String = "${System.currentTimeMillis()}${Constants.IMAGE_SUFFIX}"

        val fileValue : File
        fileValue = File.createTempFile(fileName, Constants.IMAGE_EXTENSION, filePathFolder)

        return fileValue
    }

    public fun getCacheFile(suffix : String = Constants.IMAGE_EXTENSION) : File {
        val cacheDir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getCacheDir()
            else getApplication<Application>().getExternalCacheDir()!!

        val filePathFolder : File = File(cacheDir, getString(R.string.camerax))
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String = "${System.currentTimeMillis()}${Constants.IMAGE_SUFFIX}"

        val fileValue : File = File.createTempFile(fileName, suffix, filePathFolder)

        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun logImageSaved(output : ImageCapture.OutputFileResults) {
        Log.d(TAG,"logImageSaved ${output.getSavedUri()}")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public fun getMediaStoreOutputOptions(contentResolver : ContentResolver, contentValues : ContentValues) : MediaStoreOutputOptions {
        return MediaStoreOutputOptions.Builder (
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
    }

    public fun getContentValues(name : String?, path : String?) : ContentValues {
        val values : ContentValues = ContentValues() // This PC\Benedict's Galaxy J4+\Phone\video
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name ?: "${System.currentTimeMillis()}${Constants.IMAGE_SUFFIX}")
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
        super.onCleared()
    }
}