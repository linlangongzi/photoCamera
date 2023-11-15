package com.example.photocamera

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager
{
    private var permissionCallback: ((Boolean) -> Unit)? = null
    fun requestCameraPermission(activity: Activity, requestCode: Int, callback: (Boolean) -> Unit)
    {
        permissionCallback = callback
        if (isCameraPermissionGranted(activity)) {
            permissionCallback?.invoke(true)
        } else {
            ActivityCompat.requestPermissions(activity, Constants.REQUIRED_CAMERA_PERMISSIONS, requestCode)
        }
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray)
    {
        if (requestCode == Constants.REQUEST_PERMISSIONS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, invoke the callback with true
                permissionCallback?.invoke(true)
            } else {
                // Permission denied, invoke the callback with false
                permissionCallback?.invoke(false)
            }
        }
    }

    private fun isCameraPermissionGranted(activity: Activity): Boolean
    {
        return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}