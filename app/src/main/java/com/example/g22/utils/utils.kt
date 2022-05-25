package com.example.g22

import com.example.g22.TimeSlotList.Advertisement
import com.example.g22.model.TimeSlot
import java.sql.Time
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

fun String.isValidFirebaseID(): Boolean {
    return this != ""
}

// Profile pictures handling
val LOCAL_PERSISTENT_PROFILE_PICTURE_PATH = "profile_picture.jpg"
val LOCAL_TMP_PROFILE_PICTURE_PATH = "tmp_profile_picture.jpg"

fun String.isValidImagePath(): Boolean {
    return this != ""
}

fun String.Companion.NullImagePath(): String = ""