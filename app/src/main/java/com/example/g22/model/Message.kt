package com.example.g22.model

import java.util.Date

data class Message(
    var offer: String,
    var receiver: String,
    var sender: String,
    var text: String,
    var time: Date,
    var conversationId: String
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        Date(),
    ""
    )
}