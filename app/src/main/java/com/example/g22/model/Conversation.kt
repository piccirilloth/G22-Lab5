package com.example.g22.model

enum class Status {
    PENDING,
    REJECTED,
    CONFIRMED
}

data class Conversation(
    val offerId: String,
    val requestorUid: String,
    val receiverUid: String,
    val offerTitle: String,
    val requestorName: String,
    val receiverName: String,
    val receiverUnseen: Int,
    val requestorUnseen: Int,
    val status: Status
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        "",
        0,
        0,
        Status.PENDING
    )
}