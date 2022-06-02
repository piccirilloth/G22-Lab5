package com.example.g22.TimeSlotView

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.g22.model.TimeSlot
import com.example.g22.utils.Duration
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.Executors

class TimeSlotVM(application: Application): AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var currTsListenerRegistration: ListenerRegistration? = null
    private var otherProfileListenerRegistration: ListenerRegistration? = null

    /**
     *  Mutable live data for fields of TimeSlotShowFragment and TimeSlotEditFragment
     */
    private val _currTimeSlotLD = MutableLiveData(TimeSlot.Empty())
    private val _datetimeLD = MutableLiveData(Date())
    private val _skillsLD = MutableLiveData<List<String>>(emptyList())
    private val _ownerLD = MutableLiveData("")

    /**
     * Exposed functionalities to activities / fragments
     */
    val currTimeSlotLD: LiveData<TimeSlot> = _currTimeSlotLD
    val dateTimeLD: LiveData<Date> = _datetimeLD
    val skillsLD: LiveData<List<String>> = _skillsLD
    val ownerLD: LiveData<String> = _ownerLD

    val timeslotLoadedLD = MutableLiveData(false)


    fun setCurrentTimeSlot(id: String, keepUpdated: Boolean) {
        currTsListenerRegistration?.remove()

        // Set a loading screen while fetching data..
        timeslotLoadedLD.value = false
        _currTimeSlotLD.value = TimeSlot.Empty()
        _datetimeLD.value = Date()
        _skillsLD.value = emptyList()
        _ownerLD.value = ""

        if (keepUpdated) {
            // Used for timeslot showing (keep update)
            currTsListenerRegistration = db.collection("offers")
                .document(id)
                .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, e ->
                    if (e != null) {
                        Log.d("error", "firebase failure")
                        return@addSnapshotListener
                    }
                    if (value != null && value.exists()) {
                        val ts = value.toObject(TimeSlot::class.java)!!
                        _currTimeSlotLD.postValue(ts)
                        _datetimeLD.postValue(ts.date)

                        // Load other profile data
                        otherProfileListenerRegistration?.remove()
                        otherProfileListenerRegistration = db.collection("users")
                            .document(ts.owner)
                            .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, e ->
                                if (e != null) {
                                    return@addSnapshotListener
                                }
                                if (value != null && value.exists()) {
                                    _ownerLD.postValue(value.getString("fullname"))
                                    timeslotLoadedLD.postValue(true)
                                }
                            }
                    }
                }
        } else {
            // Used for timeslot edit (load only one time)
            viewModelScope.launch(Dispatchers.IO) {
                val tsRes = firestoreGetTimeSlot(id)
                if (tsRes.isSuccess) {
                    val ts = tsRes.getOrThrow()
                    _currTimeSlotLD.postValue(ts)
                    _datetimeLD.postValue(ts.date)
                    _skillsLD.postValue(ts.skills)
                    timeslotLoadedLD.postValue(true)
                } else {
                    Toast.makeText(getApplication(), "Error loading the timeslot", Toast.LENGTH_SHORT)
                    // TODO: Set a flag to pop the back stack
                }
            }
        }

    }

    fun setCurrentTimeSlotEmpty() {
        currTsListenerRegistration?.remove()
        otherProfileListenerRegistration?.remove()

        _currTimeSlotLD.value = TimeSlot.Empty()
        _datetimeLD.value = Date()
        _skillsLD.value = emptyList()
    }

    fun saveEditedTimeSlot(title: String, duration: Int, location: String, description: String) {
        val tmp = _currTimeSlotLD.value!!.copy()
        tmp.title = title
        tmp.date = _datetimeLD.value!!
        tmp.duration = Duration(duration)
        tmp.location = location
        tmp.description = description
        tmp.skills = _skillsLD.value?.map { it.lowercase() } ?: emptyList()

        val docRef = db.collection("offers")
            .document(tmp.id)

        db.runTransaction { transaction ->
            val oldSnapshot = transaction.get(docRef)
            val old = oldSnapshot.get("skills") as List<String>
            val new = tmp.skills.minus(old)
            val oldRemoved = old.minus(tmp.skills)
            val toCreate = emptyList<String>().toMutableList()

            val toAddSnapshots = emptyMap<String, List<String>>().toMutableMap()

            for (skill in new) {
                val tmpSnap = transaction.get(db.collection("skills").document(skill))
                if (tmpSnap.exists()) {
                    toAddSnapshots[skill] = (tmpSnap.get("offers") as List<String>).plus(tmp.id)
                }
                else {
                    toCreate.add(skill)
                }
            }

            val toRemoveSnapshots = emptyMap<String, List<String>>().toMutableMap()
            for (skill in oldRemoved) {
                val tmpSnap = transaction.get(db.collection("skills").document(skill))
                if (tmpSnap.exists()) {
                    toRemoveSnapshots[skill] = (tmpSnap.get("offers") as List<String>).minus(tmp.id)
                }
                else {
                    throw Exception("Skill not found. Failure")
                }
            }

            for (entry in toAddSnapshots.entries.iterator()) {
                transaction.update(db.collection("skills").document(entry.key), "offers", entry.value)
            }

            for (entry in toRemoveSnapshots.entries.iterator()) {
                if (entry.value.isEmpty()) {
                    transaction.delete(db.collection("skills").document(entry.key))
                } else {
                    transaction.update(
                        db.collection("skills").document(entry.key),
                        "offers",
                        entry.value
                    )
                }
            }

            for (skill in toCreate) {
                transaction.set(db.collection("skills").document(skill), hashMapOf("offers" to listOf(tmp.id)))
            }

            transaction.set(db.collection("offers").document(tmp.id), tmp)
        }
            .addOnSuccessListener {
            }
            .addOnFailureListener {
                Toast.makeText(getApplication(), "Error editing the timeslot!", Toast.LENGTH_SHORT)
                //ToDo snackbar
                setCurrentTimeSlot(_currTimeSlotLD.value!!.id, true)
            }
    }


    /**
     * Firestore async suspend functions
     */

    private suspend fun firestoreGetTimeSlot(id: String): Result<TimeSlot> {
        return try {
            val documentSnapshot = db.collection("offers")
                .document(id)
                .get()
                .await()
            return Result.success(documentSnapshot.toObject(TimeSlot::class.java)!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }

    }

    /**
     * Functions to handle date and time pickers
     */
    fun setDate(y: Int, m: Int, d: Int) {
        val c = Calendar.getInstance()
        c.set(y, m, d)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)

        _datetimeLD.value = c.time
    }

    fun setTime(hourOfDay: Int, minute: Int) {
        val c = Calendar.getInstance()
        c.time = _datetimeLD.value!!
        c.set(Calendar.HOUR_OF_DAY, hourOfDay)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)

        _datetimeLD.value = c.time
    }

    fun addSkill(skill: String) {
        _skillsLD.value = _skillsLD.value?.plus(skill)
    }

    fun removeSkill(skill: String) {
        _skillsLD.value = _skillsLD.value?.minus(skill)
    }

    /**
     * ViewModel callbacks
     */
    override fun onCleared() {
        super.onCleared()

        // Clear all snapshot listeners
        currTsListenerRegistration?.remove()
        otherProfileListenerRegistration?.remove()
    }

}