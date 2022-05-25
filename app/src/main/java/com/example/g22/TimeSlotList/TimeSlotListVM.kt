package com.example.g22.TimeSlotList

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.g22.model.TimeSlot
import com.example.g22.model.AppRepository
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.util.*

class TimeSlotListVM(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()

    private var tsListListenerRegistration: ListenerRegistration? = null

    //val tsListLD: LiveData<List<TimeSlot>> = repo.getAllTimeSlots()
    private val _tsListLD: MutableLiveData<List<TimeSlot>> =
        MutableLiveData<List<TimeSlot>>().also {
            it.value = emptyList()
        }

    val tsListLD: LiveData<List<TimeSlot>> = _tsListLD

    var hasBeenAdded: MutableLiveData<Boolean> = MutableLiveData(false)
    var hasBeenEdited: MutableLiveData<Boolean> = MutableLiveData(false)

    var sortParam = ""
    var titleSearched = ""
    var ownerFilter = ""
    var locationFilter = ""
    var dateFilter = ""

    fun addTimeslot(item: TimeSlot) {
        //repo.addTimeSlot(item)

        val doc_ref = db.collection("offers").document()
        item.id = doc_ref.id

        doc_ref
            .set(item)
            .addOnSuccessListener {
                // TODO: add snackback
            }
            .addOnFailureListener {
                // TODO: add error snackbar
            }
        for (skill in item.skills) {
            db.collection("skills")
                .document(skill.toLowerCase())
                .get()
                .addOnSuccessListener {
                    if (it != null && it.exists())
                        updateSkillOffers(it, item, skill)
                    else {
                        val tmpList = hashMapOf("offers" to listOf(item.id))
                        db.collection("skills")
                            .document(skill.toLowerCase())
                            .set(tmpList)
                    }
                }
        }

    }

    fun updateSkillOffers(it: DocumentSnapshot, item: TimeSlot, skill: String) {
        val tmpList = emptyList<String>().toMutableList()
        if (it.get("offers") != null)
            tmpList != it.get("offers") as MutableList<String>

        tmpList.add(item.id)
        db.collection("skills")
            .document(skill.toLowerCase())
            .update("offers", tmpList)
    }

    fun observeMyOffers() {
        tsListListenerRegistration?.remove()

        if (Firebase.auth.currentUser == null) {
            _tsListLD.value = emptyList()
        } else {
            tsListListenerRegistration = db.collection("offers")
                .whereEqualTo("owner", Firebase.auth.currentUser?.uid.toString())
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.d("error", "firebase failure")
                        return@addSnapshotListener
                        //ToDo: add a snackbar
                    }
                    if (value != null) {
                        if (!value.isEmpty)
                            _tsListLD.value = value.toObjects(TimeSlot::class.java)!!
                        else
                            _tsListLD.value = emptyList()
                    }
                }
        }
    }

    fun observeSkillOffers(skill: String) {
        tsListListenerRegistration?.remove()
        tsListListenerRegistration = db.collection("offers")
            .whereArrayContains("skills", skill)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                    //ToDo: add a snackbar
                }
                if (value != null) {
                    if (!value.isEmpty)
                        _tsListLD.value = value.toObjects(TimeSlot::class.java)!!
                    else
                        _tsListLD.value = emptyList()
                    restoreFilters(skill)
                }
            }
    }

    fun searchByTitle(title: String, skill: String) {
        if(title == "") {
            db.collection("offers")
                .whereArrayContains("skills", skill)
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
                        _tsListLD.value = emptyList()
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

    fun sortByDate() { _tsListLD.value = _tsListLD.value?.sortedBy { it.date.toString() } }
    fun sortByTitle() { _tsListLD.value = _tsListLD.value?.sortedBy { it.title } }
    fun sortByLocation() { _tsListLD.value = _tsListLD.value?.sortedBy { it.location } }

override fun onCleared() {
    super.onCleared()

    // Clear all snapshot listeners
    tsListListenerRegistration?.remove()
}
}