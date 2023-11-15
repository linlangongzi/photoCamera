package com.example.photocamera

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import java.lang.Exception
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Enumeration
import java.util.regex.Pattern

object NetworkUtils
{

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

    fun getMyIPAddress(applicationContext : Context): String {

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
            ).hostAddress!!
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

    fun isValidIpAddress(input: String): Boolean
    {
        val ipAddressPattern =
            buildString {
        append("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
    }
        val pattern = Pattern.compile(ipAddressPattern)
        val matcher = pattern.matcher(input)
        return matcher.matches()
    }
}