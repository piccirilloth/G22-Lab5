package com.example.g22

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.TimeSlotList.Advertisement
import com.example.g22.model.TimeSlot
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.File
import java.text.DateFormat
import java.util.*

fun Date.custom_format() : String {
    return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT)
        .format(this)
}

fun Date.custom_format_no_time() : String {
    return DateFormat.getDateInstance(DateFormat.DEFAULT)
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
                return@withContext application.resources.getDrawable(R.drawable.ic_baseline_downloading_24).toBitmap()
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

// Support the snackbar handling
data class SnackbarMessage(val msg: String, val duration: Int) {

    var snackbar: Snackbar? = null

    fun make(view: View): Snackbar {
        snackbar = Snackbar.make(view, msg, duration)
        return snackbar!!
    }

    fun makeAndShow(view: View): Snackbar {
        val s = make(view)
        s.show()
        return s
    }
}

open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content

}

fun List<Event<SnackbarMessage>>.addMessage(msg: String, duration: Int): List<Event<SnackbarMessage>> {
    return this.filter { !it.hasBeenHandled || (it.hasBeenHandled && it.peekContent().snackbar?.isShown == true) }.plus(Event(SnackbarMessage(msg, duration)))
}

fun MutableLiveData<List<Event<SnackbarMessage>>>.addMessage(msg: String, duration: Int) {
    this.value = this.value?.addMessage(msg, duration)
}

fun MutableLiveData<List<Event<SnackbarMessage>>>.postMessage(msg: String, duration: Int) {
    this.postValue(this.value?.addMessage(msg, duration))
}

fun LiveData<List<Event<SnackbarMessage>>>.observeAndShow(lifecycleOwner: LifecycleOwner, view: View, lifecycleScope: LifecycleCoroutineScope) {
    this.observe(lifecycleOwner) {
        lifecycleScope.launch {
            // Wait until current shown snackbars dismiss
            it.filter { it.hasBeenHandled }.forEach { event ->
                val m = event.peekContent()
                if (m.snackbar?.isShown == true)
                    delay(m.duration.fromSnackbarDuration() + 100)
            }

            // Show pending messages
            it.forEach { event ->
                val msg = event.getContentIfNotHandled()
                if (msg != null) {
                    msg.makeAndShow(view)
                    delay(msg.duration.fromSnackbarDuration() + 100)
                }
            }
        }
    }
}

fun Int.fromSnackbarDuration(): Long {
    if (this == Snackbar.LENGTH_LONG)
        return 2750
    if (this == Snackbar.LENGTH_SHORT)
        return 1500
    if (this == Snackbar.LENGTH_INDEFINITE)
        return 0

    return this.toLong()
}