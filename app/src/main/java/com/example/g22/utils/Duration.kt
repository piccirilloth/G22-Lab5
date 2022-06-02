package com.example.g22.utils

class Duration(val minutes: Int) {
    constructor(): this(
        0
    ) {}

    constructor(days: Int, hours: Int, minutes: Int) : this(minutes + hours * 60 + days * 24 * 60) {}

    fun toUnits() : Triple<Int, Int, Int> {
        var days = 0
        var hours = 0
        var mins = minutes
        if (minutes > 59) {
            hours = minutes / 60
            mins = minutes % 60

            if (hours > 23) {
                days = hours / 24
                hours = hours % 24
            }
        }
        return Triple(days, hours, mins)
    }

    fun toShortString() : String {
        var (days, hours, mins) = toUnits()
        hours = hours + days * 24

        if (hours > 0)
            return String.format("%02d h : %02d min", hours, mins)
        else
            return String.format("%02d min", mins)
    }

    override fun toString(): String {
        val (days, hours, mins) = toUnits()
        return "$days days : $hours hours : $mins : min"
    }
}