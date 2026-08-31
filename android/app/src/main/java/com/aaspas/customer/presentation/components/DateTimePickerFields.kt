package com.aaspas.customer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aaspas.customer.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DisplayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayText = remember(value) { formatDateForDisplay(value) }
    val openPicker = { if (enabled) showPicker = true }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.picker_tap_select_date)) },
            trailingIcon = {
                IconButton(onClick = openPicker, enabled = enabled) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.picker_select_date))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = openPicker),
            )
        }
    }

    if (showPicker) {
        val initialMillis = parseIsoDateToMillis(value) ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millisToIsoDate(millis))
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showPicker by remember { mutableStateOf(false) }
    val (initialHour, initialMinute) = remember(value) { parseHourMinute(value) }
    val openPicker = { if (enabled) showPicker = true }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.picker_tap_select_time)) },
            trailingIcon = {
                IconButton(onClick = openPicker, enabled = enabled) {
                    Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.picker_select_time))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = openPicker),
            )
        }
    }

    if (showPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.picker_select_time), style = MaterialTheme.typography.titleMedium) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(formatHourMinute(timePickerState.hour, timePickerState.minute))
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

internal fun parseHourMinute(time: String): Pair<Int, Int> {
    val parts = time.trim().split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

internal fun formatHourMinute(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

internal fun parseIsoDateToMillis(isoDate: String): Long? {
    if (isoDate.isBlank()) return null
    return runCatching {
        LocalDate.parse(isoDate.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

internal fun millisToIsoDate(millis: Long): String {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

internal fun formatDateForDisplay(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return runCatching {
        DisplayDateFormatter.format(LocalDate.parse(isoDate.trim()))
    }.getOrElse { isoDate }
}
