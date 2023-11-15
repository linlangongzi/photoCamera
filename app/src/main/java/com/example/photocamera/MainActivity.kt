package com.example.photocamera

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.photocamera.databinding.ActivityMainBinding

class MainActivity() : AppCompatActivity(), ImageReceivedCallback
{

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // RequestCameraPermission
        // Initialize permissionManager if it's not initialized yet
        if (!::permissionManager.isInitialized) {
            permissionManager = PermissionManager
        }
        if (!::cameraManager.isInitialized) {
            cameraManager = CameraManager
        }

        permissionManager.requestCameraPermission(
            this,
            Constants.REQUEST_PERMISSIONS_CODE,
            ::onCameraPermissionResult
        )

        ServerSocketHandler.init(this)
        ServerSocketHandler.startServer(NetworkUtils.getLocalPortNumber())

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        binding.captureButton.setOnClickListener {
            cameraManager.takePhoto(
                context = this,
                onPhotoCaptured = { capturedBitMap ->
                    binding.photoPreview.setImageBitmap(capturedBitMap)
                },
                onPhotoSaved = { savePhotoPath ->
                    sharedViewModel.setPhotoPath(savePhotoPath)
                }
            )
        }

        binding.photoPreview.setOnClickListener{
            val intent = Intent(this, ShareActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onImageReceived(imagePath: String)
    {
        runOnUiThread {
            broadcastImageReceived()
        }
    }

    private fun broadcastImageReceived()
    {
        val intent = Intent(Constants.IMAGE_RECEIEVED_INTENT_FILTER)
        sendBroadcast(intent)
    }

    private fun onCameraPermissionResult(isPermissionGranted: Boolean)
    {
        if(isPermissionGranted) {
            cameraManager.initCamera(this, this, binding.cameraView.surfaceProvider)
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionResult(requestCode, grantResults)
    }

    override fun onDestroy()
    {
        try {
            ServerSocketHandler.stopServer()
            cameraManager.releaseCamera(this)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

}