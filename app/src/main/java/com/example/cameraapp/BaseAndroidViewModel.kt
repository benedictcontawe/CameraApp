package com.example.cameraapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel

abstract public class BaseAndroidViewModel : AndroidViewModel {

    companion object {
        private val TAG = BaseAndroidViewModel::class.java.getSimpleName()
    }

    constructor(application : Application) : super(application) { Log.d(TAG, "constructor")

    }
    //region Log Methods
    protected fun logDebug(TAG : String, message : String) {
        Log.d(TAG, message)
    }

    protected fun logError(TAG : String, message : String) {
        Log.e(TAG, message)
    }

    protected fun logError(TAG : String, message : String, throwable : Throwable) {
        Log.e(TAG, message, throwable)
    }
    //endregion
    //region Get String Methods
    protected fun getString(id : Int) : String {
        return String.format(getApplication<Application>().getResources().getString(id))
    }

    protected fun getString(id : Int, value : String?) : String {
        return String.format(getApplication<Application>().getResources().getString(id, value))
    }

    protected fun getString(id : Int, value : Int) : String {
        return String.format(getApplication<Application>().getResources().getString(id, getString(value)))
    }

    protected fun getString(id : Int, firstValue : String?, secondValue : String?) : String {
        return String.format(getApplication<Application>().getResources().getString(id, firstValue, secondValue))
    }
    //endregion
    override fun onCleared() {
        super.onCleared()
    }
}