package com.example.g22.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

class SharedPrefDatabase private constructor(private val appContext: Context, private val dbName: String)
    : AppDatabase, TimeSlotDao, ProfileDao {

    private val sharedPreferences = appContext.getSharedPreferences(dbName, Context.MODE_PRIVATE)

    private val _timeslotListLD = MutableLiveData<List<TimeSlot>>().also {
        it.value = loadTimeSlotList()
    }

    private val _profileDataLD = MutableLiveData<Profile>().also {
        it.value = loadProfileData()
    }

    /***
     * AppDatabase methods
     */
    override fun timeslotDao(): TimeSlotDao {
        return this
    }

    override fun profileDao(): ProfileDao {
        return this
    }

    /***
     * TimeSlotDao methods
     */
    override fun getAll(): LiveData<List<TimeSlot>> {
        return _timeslotListLD
    }

    override fun addTimeSlot(ts: TimeSlot) {
//        val sharedPrefEditor = sharedPreferences.edit()
//
//        val tsListStr = sharedPreferences.getString("timeslotList", "")
//        var tsList = JSONArray()
//        if(tsListStr != "")
//            tsList = JSONArray(tsListStr)
//        tsList.put(JSONObject(ts.toJSONString()))
//
//        sharedPrefEditor.putString("timeslotList", tsList.toString())
//        sharedPrefEditor.apply()
//
//        // It causes the observers of the timeslotList to be notified
//        _timeslotListLD.value = loadTimeSlotList()
    }

    override fun getTimeSlot(id: Long) : TimeSlot? {
//        val ts = _timeslotListLD.value?.find { it.id == id }
//        return ts
        return null
    }

    override fun updateTimeSlot(item: TimeSlot): Boolean {
//        val sharedPrefEditor = sharedPreferences.edit()
//
//        val tsListStr = sharedPreferences.getString("timeslotList", "")
//        var tsList: JSONArray
//        if(tsListStr != "")
//            tsList = JSONArray(tsListStr)
//        else
//            return false
//
//        for (i in 0 until tsList.length()) {
//            val tsJson = tsList.getJSONObject(i)
//            val ts = TimeSlot.fromJSONString(tsJson.toString())
//            if (ts.id == item.id) {
//                tsList.put(i, JSONObject(item.toJSONString()))
//                sharedPrefEditor.putString("timeslotList", tsList.toString())
//                sharedPrefEditor.apply()
//
//                // It causes the observers of the timeslotList to be notified
//                _timeslotListLD.value = loadTimeSlotList()
//                return true
//            }
//        }
//        return false

        return true
    }

    override fun getProfile(): LiveData<Profile> {
        return _profileDataLD
    }

    override fun setProfile(profile: Profile) : Boolean {
        val sharedPrefEditor = sharedPreferences.edit()

//        sharedPrefEditor.putString("profileData", profile.toJSONString())
        sharedPrefEditor.apply()

        _profileDataLD.value = loadProfileData()
        return true
    }

    companion object {
        @Volatile // Guarantees that after a write operation the cache needs to be flushed (for multiple threads)
        private var INSTANCE: SharedPrefDatabase? = null

        fun getDatabase(context: Context, dbName: String) : AppDatabase =
            (
                    INSTANCE ?:
                    synchronized(this) {
                        val i = INSTANCE ?: SharedPrefDatabase(context, dbName)
                        INSTANCE = i
                        INSTANCE
                    }
                    )!!
    }

    /***
     * Utilities
     */
    private fun loadTimeSlotList() : List<TimeSlot> {
        val l: MutableList<TimeSlot> = mutableListOf()

//        val tsListStr = sharedPreferences.getString("timeslotList", "")
//        if (tsListStr != "") {
//            val tsListJson = JSONArray(tsListStr)
//            for (i in 0 until tsListJson.length()) {
//                val tsJson = tsListJson.getJSONObject(i)
//                val ts = TimeSlot.fromJSONString(tsJson.toString())
//                l.add(ts)
//            }
//        }

        return l
    }

    private fun loadProfileData() : Profile {
//        val profileDataJSONString = sharedPreferences.getString("profileData", "")
//        return if(profileDataJSONString != null && profileDataJSONString != "")
//                    Profile.fromJSONString(profileDataJSONString)
//                else
//                    Profile.emptyProfile()
        return Profile()
    }

}