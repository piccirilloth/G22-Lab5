package com.example.g22.model

interface AppDatabase {
    fun timeslotDao() : TimeSlotDao

    fun profileDao() : ProfileDao
}
