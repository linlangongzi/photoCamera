package com.example.photocamera

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object FileUtils
{
    fun createJPGImageWithPattern(baseFolder: File, pattern: String): File
    {
        return File(
            baseFolder,
            SimpleDateFormat(pattern, Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg"
        )
    }
    fun getAllFilePathsFromDirectory(directory: File): List<String>
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
    fun getOutputDirectory(context: Context): File
    {
        val externalMediaDirectoryPath = context.getExternalFilesDirs(Environment.DIRECTORY_PICTURES)
        val mediaDir = externalMediaDirectoryPath.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
        // Implementation
    }

    fun getFileNameFromPath(filePath: String): String
    {
        val file = File(filePath)
        return file.name
    }
}