package com.example.g22.model

enum class Status {
    PENDING,
    REJECTED,
    REJECTED_BALANCE,
    CONFIRMED
}

data class Conversation(
    val id: String,
    val offerId: String,
    val requestorUid: String,
    val receiverUid: String,
    val offerTitle: String,
    val requestorName: String,
    val receiverName: String,
    val receiverUnseen: Int,
    val requestorUnseen: Int,
    val status: Status,
    val reviewedOfferer: Boolean = false,
    val reviewedRequestor: Boolean = false
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        0,
        0,
        Status.PENDING,
        false,
        false
    )
}