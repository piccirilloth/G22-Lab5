package com.example.g22.SkillsList

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class SkillsListVM(application: Application) : AndroidViewModel(application) {
    // TODO: Implement the ViewModel
    val db = FirebaseFirestore.getInstance()

    private var skillsListListenerRegistration: ListenerRegistration? = null

    val skillsListLD: MutableLiveData<List<String>> = MutableLiveData<List<String>>().also {
        skillsListListenerRegistration =
            db.collection("skills")
                .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (value != null && !value.isEmpty) {
                        it.postValue(value.toList().map { it.id })
                    } else {
                        it.postValue(emptyList())
                    }
                }
    }

    fun searchBySkill(skill: String) {
        skillsListListenerRegistration?.remove()

        db.collection("skills")
            .addSnapshotListener(Dispatchers.IO.asExecutor()) { value, error ->
                if (error != null) {
                    Log.d("error", "firebase failure")
                    return@addSnapshotListener
                    //ToDo: add a snackbar
                }
                if (value == null || value.isEmpty)
                    skillsListLD.postValue(emptyList())
                else {
                    if (skill == "")
                        skillsListLD.postValue(value.toList().map { it.id })
                    else
                        skillsListLD.postValue(
                            value.toList()
                            .map { it.id }
                            .filter { it.contains(skill) }
                        )
                }
            }
    }
}