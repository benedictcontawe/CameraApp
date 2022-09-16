package com.example.cameraapp

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.*
import androidx.camera.core.ImageCapture
import androidx.core.content.FileProvider
import java.io.File

class CameraViewModel : BaseAndroidViewModel {

    companion object {
        private val TAG = CameraViewModel::class.java.getSimpleName()
    }

    private val audio : AudioManager

    private val vibrator : Vibrator
    private val vibratorManager : VibratorManager?

    constructor(application : Application) : super(application) {
        audio = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.getDefaultVibrator()
        } else {
            vibratorManager = null
            vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    public fun createCameraPictureFile() : Uri {
        val packageName : String = getApplication<Application>().getApplicationContext().getPackageName()
        val authority : String = "$packageName.fileprovider"
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                FileProvider.getUriForFile(getApplication(), authority, getCacheFile())
            }
            else -> {
                Uri.fromFile(getCacheFile())
            }
        }
    }

    public fun getFile() : File {
        val dir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getFilesDir()
            else getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        val filePathFolder : File = File(dir,"CameraX")
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String = "${System.currentTimeMillis()}_cameraXSample"

        val fileValue : File
        fileValue = File.createTempFile(fileName,".JPG", filePathFolder)

        return fileValue
    }

    public fun getCacheFile() : File {
        // This PC\Galaxy J4+\Phone\Android\data\com.example.cameraapp\cache\CameraX
        val cacheDir : File =
            if (isExternalStorageWritable().not()) getApplication<Application>().getCacheDir()
            else getApplication<Application>().getExternalCacheDir()!!

        val filePathFolder : File = File(cacheDir,"CameraX")
        //filePathFolder = Environment.getExternalStorageDirectory().getPath()
        if (!filePathFolder.exists()) filePathFolder.mkdirs()

        val fileName : String
        //fileName = "${UUID.randomUUID()}_cameraXSample.jpg"
        //fileName = "${UUID.randomUUID()}_cameraXSample"
        fileName = "${System.currentTimeMillis()}_cameraXSample"

        val fileValue : File
        //fileValue = File(filePath,fileName)
        fileValue = File.createTempFile(fileName,".JPG", filePathFolder)

        return fileValue
    }

    private fun isExternalStorageWritable() : Boolean {
        val state : String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    private fun getUri(file : File) : Uri {
        return Uri.fromFile(file)
    }

    public fun logImageSaved(output : ImageCapture.OutputFileResults) {
        logDebug(TAG,"logImageSaved ${output.getSavedUri()}")
    }

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

}