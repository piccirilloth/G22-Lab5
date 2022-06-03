package com.example.g22

import android.app.Application
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.g22.TimeSlotList.Advertisement
import com.example.g22.model.TimeSlot
import kotlinx.coroutines.*
import java.io.File
import java.text.DateFormat
import java.util.*

fun Date.custom_format() : String {
    return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT)
        .format(this)
}

fun List<TimeSlot>.toAdvertisementList() : List<Advertisement> {
    val l: MutableList<Advertisement> = mutableListOf()
    this.forEach { l.add(Advertisement.FromTimeSlot(it)) }
    return l
}

// Profile pictures handling
val LOCAL_PERSISTENT_PROFILE_PICTURE_PATH = "profile_picture.jpg"
val LOCAL_TMP_PROFILE_PICTURE_PATH = "tmp_profile_picture.jpg"

fun String.isValidImagePath(): Boolean {
    return this != String.NullImagePath()
}

fun String.Companion.NullImagePath(): String = ""

fun String.Companion.LoadingImagePath(): String = ".loading"

fun ImageView.loadFromDisk(application: Application, lifecycleScope: LifecycleCoroutineScope, localPath: String) {
    val imgView = this
    lifecycleScope.launch {
        // Load bitmap with Dispatchers.IO
        val bitmap = withContext(Dispatchers.IO) {
            if (localPath == String.LoadingImagePath())
                return@withContext BitmapFactory.decodeResource(application.resources, R.drawable.ic_baseline_downloading_24)
            if (!localPath.isValidImagePath())
                return@withContext BitmapFactory.decodeResource(application.resources, R.drawable.user_icon)


            try {
                val appDir = application.filesDir
                val localFile = File(appDir.path, localPath)

                val inputStream = localFile.inputStream()
                val bitmap = inputStream.use {
                    BitmapFactory.decodeStream(it)
                }
                return@withContext bitmap
            } catch (e: Exception) {
                return@withContext null
            }
        }

        if (bitmap != null)
            imgView.setImageBitmap(bitmap)
    }
}