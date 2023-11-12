package com.example.photocamera

import android.Manifest

object Constants {
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 100
    // Specify the directory path where the photos are stored
    const val PHOTO_DIRECTORY = "/storage/emulated/0/Android/media/com.example.photocamera/photoCamera"

    const val SERVER_PORT = 12345
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}