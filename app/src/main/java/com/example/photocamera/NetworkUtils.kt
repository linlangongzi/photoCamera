package com.example.photocamera

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket

object NetworkUtils
{
//    class MyAppDirectoryProvider(private val context: Context) {
//        fun getAppDirectory(): File {
//            return context.filesDir
//        }
//    }

    fun getLocalPortNumber(): Int
    {
        var serverSocket: ServerSocket? = null
        try {
            // Create a server socket with a port number of 0
            // The port number of 0 will let the system automatically assign an available port
            serverSocket = ServerSocket(0)
            // Retrieve the actual assigned local port number
            return serverSocket.localPort
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            serverSocket?.close()
        }
        return -1 // Indicates an error if we couldn't obtain the port number
    }

    suspend fun sendSelectedPictures(selectedPaths: List<String>, receiverIp: String, receiverPort: Int, context: Context) {
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