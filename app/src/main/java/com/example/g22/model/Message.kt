package com.example.g22.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val messageId: String,
    var offer: String,
    var receiver: String,
    var sender: String,
    var text: String,
    @ServerTimestamp
    var time: Date?,
    var conversationId: String
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        Date(),
    ""
    )
}