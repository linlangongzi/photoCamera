package com.example.photocamera

import android.util.Log
import com.example.photocamera.Constants.TAG
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object ServerSocketHandler
{

    private lateinit var imageReceivedCallback: ImageReceivedCallback
    private var serverThread: Thread? = null
    private var isServerRunning = false

    fun init(callback: ImageReceivedCallback)
    {
        imageReceivedCallback = callback
    }

    fun startServer(serverPort: Int)
    {
        // Check if the server thread is already running
        if (serverThread == null || !serverThread!!.isAlive) {
            // Create a new thread only if the previous one is not running
            serverThread = thread(start = true) {
                try {
                    val serverSocket = ServerSocket(serverPort)
                    isServerRunning = true
                    println("Server listening on port $serverPort")

                    while (isServerRunning) {
                        val clientSocket = serverSocket.accept()

                        thread {
                            handleClientConnection(clientSocket)
                        }
                    }

                } catch (e: BindException) {
                    Log.e(TAG, "Error: Port $serverPort is not available. Choose a different port.")
                } catch (e: IOException) {
                    Log.e(TAG, "Error: An I/O error occurred while starting the server.", e)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error: Insufficient permissions to start the server.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error: Unexpected exception occurred.", e)
                }
            }
        }
    }

    fun stopServer() {
        isServerRunning = false
        // Optionally, you may want to interrupt the server thread
        serverThread?.interrupt()
    }

    private fun handleClientConnection(clientSocket: Socket)
    {
        try {
            val dis = DataInputStream(clientSocket.getInputStream())

            // Read the number of files to expect
            val numberOfFiles = dis.readInt()

            // Loop to receive each file
            for (i in 0 until numberOfFiles) {
                // Read the file name and length
                val fileName = dis.readUTF()
                val fileLength = dis.readInt()

                // Create a FileOutputStream to write the file
                val file = File("${Constants.PHOTO_DIRECTORY}/received_$fileName")
                val fos = FileOutputStream(file)

                // Read the file content into a byte array
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0

                while (totalBytesRead < fileLength) {
                    bytesRead = dis.read(buffer, 0, (fileLength - totalBytesRead).coerceAtMost(buffer.size))
                    if (bytesRead == -1) break
                    fos.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                }

                imageReceivedCallback.onImageReceived(file.toString())
                fos.close()
                println("File received: ${file.absolutePath}")
            }

            // Close the input stream and socket
            dis.close()
            clientSocket.close()

        }  catch (e: IOException) {
            println("Error: An I/O error occurred while handling the client connection.")
            e.printStackTrace()
        } catch (e: SecurityException) {
            println("Error: Insufficient permissions to handle the client connection.")
        } catch (e: Exception) {
            println("Error: Unexpected exception occurred while handling the client connection.")
            e.printStackTrace()
        }
    }
}
