package com.filippi.devtrack.ui

import android.app.DatePickerDialog
import android.content.Context
import java.time.LocalDate

internal fun showDeadlinePicker(context: Context, initial: LocalDate, light: Boolean, onSelect: (Long) -> Unit) {
    val theme = if (light) android.R.style.Theme_Material_Light_Dialog_Alert
        else android.R.style.Theme_Material_Dialog_Alert
    DatePickerDialog(context, theme, { _, year, month, day ->
        onSelect(LocalDate.of(year, month + 1, day).toDeadline())
    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}
