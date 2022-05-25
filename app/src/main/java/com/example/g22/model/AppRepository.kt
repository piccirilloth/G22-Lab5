package com.example.g22.model

import android.app.Application
import androidx.lifecycle.LiveData
import com.example.g22.R

class AppRepository(application: Application) {
    private val timeslotDao = SharedPrefDatabase
        .getDatabase(application, application.getString(R.string.app_name))
        .timeslotDao()

    private val profileDao = SharedPrefDatabase
        .getDatabase(application, application.getString(R.string.app_name))
        .profileDao()

    /**
     *     Expose methods to fetch database and to modify database
     */

    /**
     * TimeSlot methods
     */
    fun getAllTimeSlots() : LiveData<List<TimeSlot>> {
        return timeslotDao.getAll()
    }

    fun addTimeSlot(item: TimeSlot) {
        timeslotDao.addTimeSlot(item)
    }

    fun getTimeSlot(id: Long) : TimeSlot? {
        return timeslotDao.getTimeSlot(id)
    }

    fun updateTimeSlot(item: TimeSlot) : Boolean {
        return timeslotDao.updateTimeSlot(item)
    }


    /**
     * Profile methods
     */
    fun getProfile() : LiveData<Profile> {
        return profileDao.getProfile()
    }

    fun setProfile(profile: Profile) : Boolean {
        return profileDao.setProfile(profile)
    }
}