package com.example.g22.TimeSlotView

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.g22.custom_format
import java.time.format.DateTimeFormatter
import java.util.*

class DateTimePickerFragment(val timeEnabled: Boolean) : DialogFragment(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    val timeslotVM by activityViewModels<TimeSlotVM>()
    lateinit var selectedDate: Date

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        c.time = timeslotVM.dateTimeLD.value!!
        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)

        return DatePickerDialog(requireActivity(), this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        timeslotVM.setDate(year, month, day)

        val calendar: Calendar = Calendar.getInstance()
        calendar.time = timeslotVM.dateTimeLD.value!!
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(requireActivity(), this, hour, minute, true)
        if (timeEnabled)
            timePickerDialog.show()
    }

    override fun onTimeSet(p0: TimePicker?, hourOfDay: Int, minute: Int) {
        timeslotVM.setTime(hourOfDay, minute)
    }

}