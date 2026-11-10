package com.preet.detoxlauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockWidget(
    clockSize: String,
    isClockVisible: Boolean,
    isDateVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isClockVisible && !isDateVisible) return

    var currentTime by remember { mutableStateOf(Date()) }

    // Live clock ticker updating every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val formattedTime = timeFormat.format(currentTime)
    val formattedDate = dateFormat.format(currentTime)

    val timeFontSize = when (clockSize) {
        "COMPACT" -> 44.sp
        "MEDIUM" -> 64.sp
        else -> 82.sp // LARGE
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isClockVisible) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = timeFontSize,
                    lineHeight = timeFontSize,
                    fontWeight = FontWeight.ExtraLight
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (isDateVisible) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
