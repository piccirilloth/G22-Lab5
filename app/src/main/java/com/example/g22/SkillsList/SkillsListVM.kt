package com.example.g22.SkillsList

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SkillsListVM(application: Application) : AndroidViewModel(application) {
    // TODO: Implement the ViewModel
    val db = FirebaseFirestore.getInstance()

    private var skillsListListenerRegistration: ListenerRegistration? = null

    val skillsListLD: MutableLiveData<List<String>> = MutableLiveData<List<String>>().also {
        skillsListListenerRegistration =
            db.collection("skills")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Log.d("error", "firebase failure")
                        return@addSnapshotListener
                        //ToDo: add a snackbar
                    }
                    if (value != null && !value.isEmpty) {
                        val tmpList = emptyList<String>().toMutableList()
                        for (document in value) {
                            tmpList.add(document.id)
                        }
                        it.value = tmpList
                    } else {
                        it.value = emptyList()
                        //ToDo: inserire messaggio di lista vuota
                    }
                }
    }

    fun searchBySkill(skill: String) {
        if (skillsListListenerRegistration != null)
            skillsListListenerRegistration!!.remove()

        db.collection("skills")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                    //ToDo: add a snackbar
                }
                if (value == null || value.isEmpty)
                    skillsListLD.value = emptyList()
                else {
                    val tmpList = emptyList<String>().toMutableList()
                    if (skill == "") {
                        for (document in value)
                            tmpList.add(document.id)
                    } else {
                        for (document in value) {
                            if (document.id.contains(skill))
                                tmpList.add(document.id)
                        }
                    }
                    skillsListLD.value = tmpList
                }
            }
    }
}