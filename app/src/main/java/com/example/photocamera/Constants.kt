package com.example.photocamera

import android.Manifest

object Constants
{
    const val TAG = "cameraX"
    const val SERVER_PORT = 12345
    const val FILE_NAME_PATTERN = "yyyyMMdd-HH-mm-ss"
    const val REQUEST_PERMISSIONS_CODE = 100
    // Specify the directory path where the photos are stored
    const val PHOTO_DIRECTORY = "/storage/emulated/0/Android/media/com.example.photocamera/photoCamera"
    const val IMAGE_RECEIEVED_INTENT_FILTER = "com.example.photocamera.IMAGE_RECEIVED"
    val REQUIRED_CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}