package com.example.g22.model

import android.graphics.Bitmap
import androidx.lifecycle.LiveData

interface ProfileDao {

    fun getProfile() : LiveData<Profile>

    fun setProfile(profile: Profile) : Boolean
}