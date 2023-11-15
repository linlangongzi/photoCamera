package com.example.photocamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CameraManager
{
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraExecutors: ExecutorService
    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var surfaceProvider: SurfaceProvider
    private var imageCapture : ImageCapture?= null

    fun initCamera(context: Context, owner: LifecycleOwner, surfaceProvider: SurfaceProvider)
    {
        this.lifecycleOwner = owner
        this.surfaceProvider = surfaceProvider
        this.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        this.imageCapture = ImageCapture.Builder().build()

        initializeExecutors()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            // Bind the camera
            bindCameraUseCases(context)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun initializeExecutors()
    {
        try {
            cameraExecutors = Executors.newSingleThreadExecutor()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error initializing camera executors: ${e.message}", e)
        }
    }

    private fun bindCameraUseCases(context: Context)
    {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()
        // Set up the preview use case
        val preview = Preview.Builder()
            .build()
            .also {it ->
                it.setSurfaceProvider(surfaceProvider)
            }

        try {
            cameraProvider.unbindAll()
            // Bind use cases to the camera
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.d(Constants.TAG, "Error binding camera use cases", e)
        }

    }

    fun takePhoto(context: Context, onPhotoCaptured: (Bitmap) -> Unit, onPhotoSaved: (String) -> Unit)
    {
        val imageCapture = imageCapture ?: return
        val photoFile = FileUtil.createJPGImageWithPattern(FileUtil.getOutputDirectory(context), Constants.FILE_NAME_PATTERN)

        cameraExecutors.execute {
            try {
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults)
                        {
                            val savedUri = Uri.fromFile(photoFile)
                            onPhotoSaved.invoke(savedUri.toString())

                            val bitmap: Bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            onPhotoCaptured.invoke(bitmap)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(Constants.TAG, "Photo capture failed: ${exception.message}", exception)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error taking photo: ${e.message}", e)
            }
        }
    }

    fun releaseCamera(context: Context)
    {
        // Unbind use cases and release the camera provider
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error unbinding use cases: ${e.message}", e)
        }
        // Shutdown the cameraExecutors
        finally {
            try {
                cameraExecutors.shutdown()
                if (!cameraExecutors.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.e(Constants.TAG, "Camera executors did not terminate in time")
                }
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error shutting down camera executors: ${e.message}", e)
            }
        }
        // Set imageCapture to null to indicate that the camera is released
        imageCapture = null
    }
}