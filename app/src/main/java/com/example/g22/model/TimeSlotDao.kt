package com.example.g22.model

import androidx.lifecycle.LiveData

interface TimeSlotDao {

    fun getAll() : LiveData<List<TimeSlot>>

    fun addTimeSlot(item: TimeSlot)

    fun getTimeSlot(id: Long) : TimeSlot?

    fun updateTimeSlot(item: TimeSlot) : Boolean

}