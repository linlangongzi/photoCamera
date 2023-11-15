package com.example.photocamera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.photocamera.NetworkUtils.getMyIPAddress
import com.example.photocamera.NetworkUtils.isValidIpAddress
import com.example.photocamera.databinding.ActivityShareBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.Socket

class ShareActivity : AppCompatActivity()
{
    private lateinit var shareBinding: ActivityShareBinding
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: CRecyclerViewAdapter

    private lateinit var outputDirectory : File

    private val receiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle the received intent here and update your ListView
            updateListView()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        shareBinding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(shareBinding.root)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        // Observe changes in the photoPath LiveData
        sharedViewModel.photoPath.observe(this, Observer {
            // Handle the new photo path, update Customized ListView
            updateListView()
        })

        // Initialze Output Directory
        outputDirectory = FileUtil.getOutputDirectory(this)

        layoutManager = LinearLayoutManager(this)
        shareBinding.photoListRecyclerView.layoutManager = layoutManager

        adapter = CRecyclerViewAdapter(getItemList())
        shareBinding.photoListRecyclerView.adapter = adapter

        shareBinding.shareBtn.setOnClickListener{
            lifecycleScope.launch {
                sendPhoto()
            }
        }

        // Return to MainActivity
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        shareBinding.showMyIp.text = "My IP Address : " + NetworkUtils.getMyIPAddress(this)
        shareBinding.showMyPort.text = NetworkUtils.getLocalPortNumber().toString()
    }
    private fun getItemList(): MutableList<ImageSelectionItem>
    {
        val itemList = mutableListOf<ImageSelectionItem>()
        val allFilePathsFromDirectory = FileUtil.getAllFilePathsFromDirectory(outputDirectory)

        for (filePath in allFilePathsFromDirectory) {
            itemList.add(ImageSelectionItem(filePath, FileUtil.getFileNameFromPath(filePath), false))
        }
        return itemList.asReversed()
    }

    override fun onResume()
    {
        super.onResume()
        val filter = IntentFilter(Constants.IMAGE_RECEIEVED_INTENT_FILTER)
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
    private fun updateListView()
    {
        val newItemList = getItemList().reversed()
        adapter.run {
            clear()
            addAll(newItemList)
            notifyDataSetChanged()
        }
    }
    private fun showMyIp()
    {
        // Set the IP address to the TextView
        val ipAddress = getMyIPAddress(this)
        shareBinding.showMyIp.text = "IP Address: $ipAddress"
        // Set the Port
        val portNumber = NetworkUtils.getLocalPortNumber()
        shareBinding.showMyPort.append("$portNumber")
    }

    private fun getSelectedPaths(): List<String>
    {
        val recyclerView = shareBinding.photoListRecyclerView
        val adapter = recyclerView.adapter as? CRecyclerViewAdapter
        return adapter?.getSelectedOnes() ?: emptyList()
    }

    private suspend fun sendPhoto()
    {
        val ipAddress = shareBinding.receiverIP.text
        val receiverServerPort = shareBinding.receiverPort.text.toString()
        val paths = getSelectedPaths()
        if (receiverServerPort.isNotEmpty()) {
            try {
                val port = receiverServerPort.toInt()
                if(paths.isNotEmpty()) {
                    if (isValidIpAddress(ipAddress.toString())) {
                        sendSelectedPictures(paths, ipAddress.toString(), port, this)
                    } else {
                        Toast.makeText(this@ShareActivity, "Invalid IP Address $ipAddress", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShareActivity, "No Picture Selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                // Handle the case where the port is not a valid integer
                Toast.makeText(this@ShareActivity, "Invalid Port Number", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where the input string is empty
            Toast.makeText(this@ShareActivity, "Port Number Cannot be empty", Toast.LENGTH_SHORT).show()
        }

    }

    private suspend fun sendSelectedPictures(selectedPaths: List<String>, receiverIp: String, receiverPort: Int, context: Context) {
        // GlobalScope.launch(Dispatchers.IO) starts a coroutine in the IO dispatcher,
        // which is suitable for performing IO-bound operations, such as socket operations.
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Connect to the server
                val socket = Socket(receiverIp, receiverPort)
                val outputStream = socket.getOutputStream()
                val dos = DataOutputStream(outputStream)

                if (socket.isConnected) {
                    // Send the number of files to expect
                    dos.writeInt(selectedPaths.size)
                    // Iterate through selected paths and send each picture
                    for (path in selectedPaths) {
                        val file = File(path)
                        if (file.exists()) {
                            // Send the file name and length
                            dos.writeUTF(file.name)
                            dos.writeInt(file.length().toInt())

                            // Read the file content into a byte array
                            val fileBytes = file.readBytes()

                            // Send the file content
                            dos.write(fileBytes, 0, fileBytes.size)
                        }
                    }
                } else {
                    Log.d(Constants.TAG, "Can not connect to the server at : ${receiverIp}")
                }
                // Close the DataOutputStream and socket
                dos.close()
                socket.close()
            } catch (e: ConnectException) {
                showErrorOnMainThread("Connection refused. Make sure the server is running.", context)
            } catch (e: IOException) {
                showErrorOnMainThread("IO exception occurred: ${e.message}", context)
            }
        }
    }

    private suspend fun showErrorOnMainThread(errorMessage: String, context: Context)
    {
        // withContext(Dispatchers.Main) function to switch to the main (UI) thread when needed.
        // This is especially useful when you need to perform UI-related operations from a background thread or coroutine.
        withContext(Dispatchers.Main) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }


}