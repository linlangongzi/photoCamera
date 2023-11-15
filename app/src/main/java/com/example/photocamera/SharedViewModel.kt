package com.example.photocamera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// to establish communication between the MainActivity and ShareActivity
// to notify the latter when a new photo is captured.
// One approach is to use a shared ViewModel that both activities can observe.
class SharedViewModel : ViewModel()
{
    private var _photoPath = MutableLiveData<String>()
    val photoPath: LiveData<String> get() = _photoPath

    fun setPhotoPath(path: String) { _photoPath.value = path }
}