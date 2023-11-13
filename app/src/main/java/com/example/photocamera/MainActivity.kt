package com.example.photocamera

import com.example.photocamera.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
//import com.example.photocamera.databinding.ActivityMainRedesignBinding

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Enumeration
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class MainActivity() : AppCompatActivity(), ImageReceivedCallback {

    private lateinit var binding: ActivityMainBinding
//    private lateinit var binding: ActivityMainRedesignBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraExecutors: ExecutorService
    private lateinit var outputDirectory : File
    private lateinit var adapter: CustomAdapter

    private var imageCapture : ImageCapture ?= null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        binding = ActivityMainRedesignBinding(layoutInflater)
        setContentView(binding.root)

        // Initialze Output Directory
        outputDirectory = getOutputDirectory()

        // RequestCameraPermission
        requestCameraPermission()
        //

        ServerSocketHandler.init(this)
        ServerSocketHandler.startServer(NetworkUtils.getLocalPortNumber())

        showMyIp()

        cameraExecutors = Executors.newSingleThreadExecutor()

        binding.camera.setOnClickListener{
            takePhoto()
        }

        adapter = CustomAdapter(this, R.layout.list_item_layout, R.id.textViewItem, mutableListOf())
        binding.photoPathList.adapter = adapter
        updateListView()

        binding.share.setOnClickListener{
            lifecycleScope.launch {
                sendPhoto()
            }
        }
    }

    override fun onImageReceived(imagePath: String)
    {
        runOnUiThread {
            // Update UI on the main thread
            // For example, update a list of image paths and refresh the ListView
            updateListView()
//            imagePaths.add(imagePath)
//            customAdapter.notifyDataSetChanged()
        }
    }

    private fun showMyIp()
    {
        val ipAddress = getMyIPAddress()
        // Set the IP address to the TextView
        binding.myipAdress.text = "IP Address: $ipAddress"

        val portNumber = NetworkUtils.getLocalPortNumber()
        binding.myipAdress.append("\nPort Number: $portNumber")
    }

    private fun getMyIPAddress(): String {

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo

            if (wifiInfo != null && wifiInfo.networkId != -1) {
                // Using WIFI
                val ipAddress = wifiInfo.ipAddress
                return InetAddress.getByAddress(
                    byteArrayOf(
                        (ipAddress and 0xFF).toByte(),
                        (ipAddress shr 8 and 0xFF).toByte(),
                        (ipAddress shr 16 and 0xFF).toByte(),
                        (ipAddress shr 24 and 0xFF).toByte()
                    )
                ).hostAddress
            } else {
                // Using Cellar Data
                val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface: NetworkInterface = interfaces.nextElement()
                    val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address: InetAddress = addresses.nextElement()
                        if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                            return address.hostAddress
                        }
                    }
                }
            }

        return "N/A"
    }

    private fun isValidIpAddress(input: String): Boolean {
        val ipAddressPattern =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"

        val pattern = Pattern.compile(ipAddressPattern)
        val matcher = pattern.matcher(input)
        return matcher.matches()
    }

    private suspend fun sendPhoto()
    {
        val ipAddress = binding.editTextIp.text
        val receiverServerPort = binding.editTextPort.text.toString()
        val paths = getSelectedPaths()
        if (receiverServerPort.isNotEmpty()) {
            try {
                val port = receiverServerPort.toInt()
                if(paths.isNotEmpty()) {
                    if (isValidIpAddress(ipAddress.toString())) {
                        NetworkUtils.sendSelectedPictures(paths, ipAddress.toString(), port, this)
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid IP Address $ipAddress", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No Picture Selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                // Handle the case where the port is not a valid integer
                Toast.makeText(this@MainActivity, "Invalid Port Number", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where the input string is empty
            Toast.makeText(this@MainActivity, "Port Number Cannot be empty", Toast.LENGTH_SHORT).show()
        }

    }

    private fun getSelectedPaths(): List<String>
    {
        val selectedPaths = mutableListOf<String>()
        val listView = binding.photoPathList
        for (i in 0 until listView.count) {
            val checkBox = listView.getChildAt(i)?.findViewById<CheckBox>(R.id.checkBoxItem)
            if (checkBox?.isChecked == true) {
                val item = listView.getItemAtPosition(i) as? String
                item?.let {
                    selectedPaths.add(it)
                }
            }
        }
        return selectedPaths
    }

    private fun updateListView()
    {
        val directoryPaths = getOutputDirectory()  // Implement this method to get paths
        val filePath = getFilePathsFromDirectory(directoryPaths)
        adapter.clear()
        adapter.addAll(filePath.reversed())
        adapter.notifyDataSetChanged()
    }

    private fun getFilePathsFromDirectory(directory: File): List<String>
    {
        val filePaths = mutableListOf<String>()
        // Check if the directory is not null and it exists
        if (directory.exists() && directory.isDirectory) {
            // List all files in the directory
            val files = directory.listFiles()

            // Check if files is not null
            if (files != null) {
                for (file in files) {
                    // Add the absolute path of each file to the list
                    filePaths.add(file.absolutePath)
                }
            }
        }

        return filePaths
    }

    private fun getOutputDirectory():File
    {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun createFile(baseFolder: File): File
    {
        return File(
            baseFolder,
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"
        )
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = createFile(outputDirectory)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture( outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults)
                {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo saved: $savedUri"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    updateListView()
                }

                override fun onError(exception: ImageCaptureException)
                {
                    Log.e(Constants.TAG, "Photo capture failed: ${exception.message}", exception)
                }
            })
    }

    private fun initializeCamera()
    {
        // Initialize the camera
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Bind the camera
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestCameraPermission()
    {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        } else {
            // Permission already granted, initialize the camera
            initializeCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize the camera
                initializeCamera()
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this,"Permissions Denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun bindCameraUseCases()
    {
        val cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()
        // Set up the preview use case
        val preview = Preview.Builder()
            .build()
            .also {mPreiview ->
                mPreiview.setSurfaceProvider(binding.cameraView.surfaceProvider)
            }
        // Select the back camera
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            // Bind use cases to the camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview)
        } catch (e: Exception) {
            Log.d(Constants.TAG, "Camera Started Failed", e)
        }
        imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.d(Constants.TAG, "Camera Started Failed", e)
        }
    }

    override fun onDestroy() {
        try {
            ServerSocketHandler.stopServer()
            cameraExecutors.shutdown()
            cameraExecutors.awaitTermination(5, TimeUnit.SECONDS) // Adjust the timeout as needed
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

}