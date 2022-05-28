package com.example.g22.model

data class Conversation(
    val offerId: String,
    val requestorUid: String,
    val receiverUid: String,
    val offerTitle: String,
    val requestorName: String,
    val receiverName: String,
    val notRedMessages: Int
) {
    constructor(): this(
        "",
        "",
        "",
        "",
        "",
        "",
        0
    )
}