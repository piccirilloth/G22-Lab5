package com.example.g22.TimeSlotView

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.g22.Event
import com.example.g22.SnackbarMessage
import com.example.g22.addMessage
import com.example.g22.model.TimeSlot
import com.example.g22.utils.Duration
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

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
    val invalidTimeslotLD = MutableLiveData(false)

    // Snackbar handling
    private val _snackbarMessages = MutableLiveData<List<Event<SnackbarMessage>>>(emptyList())
    val snackbarMessages: LiveData<List<Event<SnackbarMessage>>>
        get() = _snackbarMessages


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
            viewModelScope.launch {
                val tsRes = firestoreGetTimeSlot(id)
                if (tsRes.isSuccess) {
                    val ts = tsRes.getOrThrow()
                    _currTimeSlotLD.postValue(ts)
                    _datetimeLD.postValue(ts.date)
                    _skillsLD.postValue(ts.skills)
                    timeslotLoadedLD.postValue(true)
                } else {
                    _snackbarMessages.addMessage("Error loading the time slot!", Snackbar.LENGTH_LONG)
                    invalidTimeslotLD.value = true
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

        // We use GlobalScope to ensure that the writing to db is independent from the view model lifecycle
        GlobalScope.launch {

            val res = firestoreUpdateTimeslot(tmp)

            if (res.isSuccess) {
                viewModelScope.launch {
                    _snackbarMessages.addMessage("Timeslot correctly updated!", Snackbar.LENGTH_LONG)
                }
            } else {
                viewModelScope.launch {
                    _snackbarMessages.addMessage("Error updating the timeslot!", Snackbar.LENGTH_LONG)
//                    setCurrentTimeSlot(_currTimeSlotLD.value!!.id, true)
                }
            }

        }
    }

    fun addTimeslot(item: TimeSlot) {
        GlobalScope.launch {
            val result = firebaseNewTimeslot(item)

            if (result.isSuccess) {
                viewModelScope.launch {
                    _snackbarMessages.addMessage("Timeslot created successfully!", Snackbar.LENGTH_LONG)
                }
            } else {
                viewModelScope.launch {
                    _snackbarMessages.addMessage("Error while creating the timeslot!", Snackbar.LENGTH_LONG)
                }
            }
        }
    }


    /**
     * Firestore async suspend functions
     */

    private suspend fun firestoreUpdateTimeslot(ts: TimeSlot): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val docRef = db.collection("offers")
                    .document(ts.id)

                db.runTransaction { transaction ->
                    val oldSnapshot = transaction.get(docRef)
                    val old = oldSnapshot.get("skills") as List<String>
                    val new = ts.skills.minus(old)
                    val oldRemoved = old.minus(ts.skills)
                    val toCreate = emptyList<String>().toMutableList()

                    val toAddSnapshots = emptyMap<String, List<String>>().toMutableMap()

                    for (skill in new) {
                        val tmpSnap = transaction.get(db.collection("skills").document(skill))
                        if (tmpSnap.exists()) {
                            toAddSnapshots[skill] =
                                (tmpSnap.get("offers") as List<String>).plus(ts.id)
                        } else {
                            toCreate.add(skill)
                        }
                    }

                    val toRemoveSnapshots = emptyMap<String, List<String>>().toMutableMap()
                    for (skill in oldRemoved) {
                        val tmpSnap = transaction.get(db.collection("skills").document(skill))
                        if (tmpSnap.exists()) {
                            toRemoveSnapshots[skill] =
                                (tmpSnap.get("offers") as List<String>).minus(ts.id)
                        } else {
                            throw Exception("Skill not found. Failure")
                        }
                    }

                    for (entry in toAddSnapshots.entries.iterator()) {
                        transaction.update(
                            db.collection("skills").document(entry.key),
                            "offers",
                            entry.value
                        )
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
                        transaction.set(
                            db.collection("skills").document(skill),
                            hashMapOf("offers" to listOf(ts.id))
                        )
                    }

                    transaction.set(db.collection("offers").document(ts.id), ts)
                }.await()

                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private suspend fun firestoreGetTimeSlot(id: String): Result<TimeSlot> {
        return withContext(Dispatchers.IO) {
            try {
                val documentSnapshot = db.collection("offers")
                    .document(id)
                    .get()
                    .await()
                return@withContext Result.success(documentSnapshot.toObject(TimeSlot::class.java)!!)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private suspend fun firebaseNewTimeslot(item: TimeSlot): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val newTSDocRef = db.collection("offers").document()
                item.id = newTSDocRef.id

                // Transaction to write on two collections (this should be done with lambda functions on server)
                db.runTransaction { transaction ->
                    val toCreateSkills = emptyList<String>().toMutableList()
                    val toUpdateSkills = emptyMap<String, List<String>>().toMutableMap()

                    // Get operations
                    for (skill in item.skills) {
                        val docSnapshot = transaction.get(db.collection("skills").document(skill.toLowerCase()))
                        if (docSnapshot.exists()) {
                            val docList = docSnapshot.get("offers") as List<String>
                            toUpdateSkills[skill] = docList.plus(newTSDocRef.id)
                        } else {
                            toCreateSkills.add(skill)
                        }
                    }

                    // Set operations
                    for (skill in toCreateSkills) {
                        transaction.set(
                            db.collection("skills").document(skill),
                            hashMapOf("offers" to listOf(newTSDocRef.id))
                        )
                    }

                    for (entry in toUpdateSkills.entries.iterator()) {
                        transaction.update(
                            db.collection("skills").document(entry.key),
                            "offers",
                            entry.value
                        )
                    }

                    transaction.set(newTSDocRef, item)
                }.await()

                return@withContext Result.success(Unit)
            } catch(e: Exception) {
                return@withContext Result.failure(e)
            }
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