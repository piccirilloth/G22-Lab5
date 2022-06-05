package com.example.g22.TimeSlotList

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.TimeSlot
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.*

class TimeSlotListVM(application: Application) : AndroidViewModel(application) {

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    private var tsListListenerRegistration: ListenerRegistration? = null

    // Live data
    private val _tsListLD: MutableLiveData<List<TimeSlot>> =
        MutableLiveData<List<TimeSlot>>().also {
            it.value = emptyList()
        }

    val tsListLD: LiveData<List<TimeSlot>> = _tsListLD
    val tsListLoadedLD = MutableLiveData<Boolean>(false)

    var hasBeenAdded: MutableLiveData<Boolean> = MutableLiveData(false)
    var hasBeenEdited: MutableLiveData<Boolean> = MutableLiveData(false)

    var sortParam = ""
    var titleSearched = ""
    var ownerFilter = ""
    var locationFilter = ""
    var dateFilter = ""

    fun addTimeslot(item: TimeSlot) {
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
        }
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }

    fun observeMyOffers() {
        tsListListenerRegistration?.remove()

        if (Firebase.auth.currentUser == null) {
            _tsListLD.value = emptyList()
        } else {
            // Set a loading screen while fetching data..
            tsListLoadedLD.value = false
            _tsListLD.value = emptyList()

            tsListListenerRegistration = db.collection("offers")
                .whereEqualTo("owner", Firebase.auth.currentUser?.uid.toString())
                .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (value != null) {
                        if (!value.isEmpty)
                            _tsListLD.postValue(value.toObjects(TimeSlot::class.java).sortedBy { it.accepted })
                        else
                            _tsListLD.postValue(emptyList())

                        tsListLoadedLD.postValue(true)
                    }
                }
        }
    }

    fun observeSkillOffers(skill: String) {
        tsListListenerRegistration?.remove()

        // Set a loading screen while fetching data..
        tsListLoadedLD.value = false
        _tsListLD.value = emptyList()

        tsListListenerRegistration = db.collection("offers")
            .whereEqualTo("accepted", false)
            .whereArrayContains("skills", skill)
            .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (!value.isEmpty)
                        _tsListLD.postValue(value.toObjects(TimeSlot::class.java))
                    else
                        _tsListLD.postValue(emptyList())

                    tsListLoadedLD.postValue(true)
                }
            }
    }

    fun searchByTitle(title: String, skill: String) {
        if(title == "") {
            db.collection("offers")
                .whereArrayContains("skills", skill)
                .whereEqualTo("accepted", false)
                .get()
                .addOnSuccessListener {
                    if(it.isEmpty)
                        _tsListLD.value = emptyList()
                    else
                        _tsListLD.value = it.toObjects(TimeSlot::class.java)
                }
        } else  {
            db.collection("offers")
                .whereArrayContains("skills", skill)
                .whereEqualTo("accepted", false)
                .get()
                .addOnSuccessListener {
                    if(it.isEmpty)
                        _tsListLD.value = emptyList()
                    else {
                        var tmpList = emptyList<TimeSlot>()
                        for(timeslot in it) {
                            val t = timeslot.get("title").toString()
                            if(t.lowercase().contains(title.lowercase()))
                                tmpList = tmpList.plus(timeslot.toObject(TimeSlot::class.java))
                        }
                        _tsListLD.value = tmpList
                    }
                }
        }
    }

    fun sort(filter: String) {
        if(filter == "Date")
            sortByDate()
        else if(filter == "Title")
            sortByTitle()
        else if(filter == "Location")
            sortByLocation()
    }

    fun applyFilters(owner: String, location: String, date: String, skill: String) {
        val filteredUsersList = emptyList<String>().toMutableList()
        var query = db.collection("offers")
            .whereEqualTo("accepted", false)
            .whereArrayContains("skills", skill)
        if (owner != "") {
            db.collection("users")
                .whereEqualTo("fullname", owner)
                .get()
                .addOnSuccessListener {
                    if (it != null && !it.isEmpty) {
                        for (user in it) {
                            filteredUsersList.add(user.get("id").toString())
                        }
                    }
                    if(!filteredUsersList.isEmpty()) {
                        query = query.whereIn("owner", filteredUsersList)
                        query.get().addOnSuccessListener {
                            filterByLocationAndDate(location, date, it)
                        }
                    } else
                        _tsListLD.postValue(emptyList())
                }
        }
        else {
            query.get().addOnSuccessListener {
                filterByLocationAndDate(location, date, it)
            }
        }
    }

    fun filterByLocationAndDate(location: String, date:String, it: QuerySnapshot) {
        if (it.isEmpty)
            _tsListLD.value = emptyList()
        else {
            var tmpList = emptyList<TimeSlot>()
            if(date == "") {
                for(timeslot in it) {
                    val t = timeslot.get("location").toString()
                    if(t.lowercase().contains(location.lowercase()))
                        tmpList = tmpList.plus(timeslot.toObject(TimeSlot::class.java))
                }
            } else {
                for(timeslot in it) {
                    val t = timeslot.get("location").toString()
                    val d: Timestamp = timeslot.get("date") as Timestamp
                    val timeslotDate: Date = d.toDate()
                    val dateToCompare = "${timeslotDate.year+1900}-${timeslotDate.month+1}-${timeslotDate.date}"
                    if(t.lowercase().contains(location.lowercase()) && date.compareTo(dateToCompare) == 0)
                        tmpList = tmpList.plus(timeslot.toObject(TimeSlot::class.java))
                }
            }
            _tsListLD.value = tmpList
        }
    }

    fun restoreFilters(skill: String) {
        if(titleSearched != "" )
            searchByTitle(titleSearched, skill)
        if(locationFilter != "" || dateFilter != "" || ownerFilter != "")
            applyFilters(ownerFilter, locationFilter, dateFilter, skill)
        sort(sortParam)
    }

    fun clearFilters() {
        titleSearched = ""
        locationFilter = ""
        dateFilter = ""
        ownerFilter = ""
        sortParam = ""
    }

    fun sortByDate() { _tsListLD.postValue(_tsListLD.value?.sortedBy { it.date.toString() }) }
    fun sortByTitle() { _tsListLD.postValue(_tsListLD.value?.sortedBy { it.title.toString() }) }
    fun sortByLocation() { _tsListLD.postValue(_tsListLD.value?.sortedBy { it.location.toString() }) }

    override fun onCleared() {
        super.onCleared()

        // Clear all snapshot listeners
        tsListListenerRegistration?.remove()
    }
}