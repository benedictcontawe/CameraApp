package com.example.cameraapp

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public object Coroutines {
    //region UI contexts
    fun main(work : suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Main.immediate).launch {
            work()
        }
    //endregion
    //region I/O operations
    fun io(work : suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.IO).launch {
            work()
        }

    fun io(viewModel : ViewModel, work : suspend (() -> Unit)) {
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            work()
        }
    }
    //endregion
    //region Uses heavy CPU computation
    fun default(work : suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Default).launch {
            work()
        }

    fun default(viewModel : ViewModel, work : suspend (() -> Unit)) =
        viewModel.viewModelScope.launch(Dispatchers.Default) {
            work()
        }
    //endregion
    //region No need to run on specific context
    fun unconfined(work : suspend (() -> Unit)) =
        CoroutineScope(Dispatchers.Unconfined).launch {
            work()
        }
    //endregion
}