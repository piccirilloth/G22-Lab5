package com.example.g22.model

import com.google.firebase.auth.FirebaseUser

data class Profile (
    var id: String,
    var fullname: String,
    var email: String,
    var nickname: String,
    var location: String,
    var phone: String,
    var skills: List<String>,
    var description: String
        ) {

    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        "",
        emptyList(),
        "",
    )

    companion object {
        fun FromFirebaseUser(user: FirebaseUser): Profile {
            return Profile(
                user.uid,
                user.displayName ?: "",
                user.email ?: "",
                "nick_" + user.uid,
                "",
                user.phoneNumber ?: "",
                emptyList(),
                ""
            )
        }
    }
}