package com.preet.detoxlauncher.data.repository

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.preet.detoxlauncher.DetoxApplication
import com.preet.detoxlauncher.data.local.dao.FocusSessionDao
import com.preet.detoxlauncher.data.local.entity.FocusSessionEntity
import com.preet.detoxlauncher.data.model.FocusSessionType
import com.preet.detoxlauncher.data.model.PomodoroState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FocusRepository(
    private val context: Context,
    private val focusSessionDao: FocusSessionDao,
    private val scope: CoroutineScope
) {
    private val _pomodoroState = MutableStateFlow(PomodoroState())
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private var timerJob: Job? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun startFocusSession(durationMinutes: Int = 25) {
        timerJob?.cancel()
        val totalSecs = durationMinutes * 60
        _pomodoroState.value = PomodoroState(
            sessionType = FocusSessionType.FOCUS,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            isPaused = false
        )

        scope.launch(Dispatchers.IO) {
            val sessionId = focusSessionDao.insertSession(
                FocusSessionEntity(
                    startTime = System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    sessionType = FocusSessionType.FOCUS.name,
                    completed = false
                )
            )
            _pomodoroState.value = _pomodoroState.value.copy(currentSessionId = sessionId)
        }

        startTicker()
    }

    fun startBreak(durationMinutes: Int = 5, isLongBreak: Boolean = false) {
        timerJob?.cancel()
        val totalSecs = durationMinutes * 60
        val type = if (isLongBreak) FocusSessionType.LONG_BREAK else FocusSessionType.SHORT_BREAK
        _pomodoroState.value = PomodoroState(
            sessionType = type,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            isRunning = true,
            isPaused = false
        )
        startTicker()
    }

    fun pause() {
        if (_pomodoroState.value.isRunning && !_pomodoroState.value.isPaused) {
            timerJob?.cancel()
            _pomodoroState.value = _pomodoroState.value.copy(isPaused = true)
            updateNotification("Session Paused", _pomodoroState.value.formattedRemainingTime)
        }
    }

    fun resume() {
        if (_pomodoroState.value.isPaused) {
            _pomodoroState.value = _pomodoroState.value.copy(isPaused = false)
            startTicker()
        }
    }

    fun stop() {
        timerJob?.cancel()
        _pomodoroState.value = PomodoroState()
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun startTicker() {
        timerJob = scope.launch(Dispatchers.Default) {
            while (_pomodoroState.value.remainingSeconds > 0) {
                delay(1000L)
                val newRemaining = (_pomodoroState.value.remainingSeconds - 1).coerceAtLeast(0)
                _pomodoroState.value = _pomodoroState.value.copy(remainingSeconds = newRemaining)
                updateNotification(
                    _pomodoroState.value.sessionType.displayName,
                    _pomodoroState.value.formattedRemainingTime
                )
            }
            onSessionCompleted()
        }
    }

    private suspend fun onSessionCompleted() {
        val current = _pomodoroState.value
        val sessionId = current.currentSessionId

        if (sessionId != null && current.sessionType == FocusSessionType.FOCUS) {
            withContext(Dispatchers.IO) {
                focusSessionDao.insertSession(
                    FocusSessionEntity(
                        id = sessionId,
                        startTime = System.currentTimeMillis() - (current.totalSeconds * 1000L),
                        durationMinutes = current.totalSeconds / 60,
                        sessionType = current.sessionType.name,
                        completed = true
                    )
                )
            }
        }

        _pomodoroState.value = _pomodoroState.value.copy(
            isRunning = false,
            remainingSeconds = 0
        )
        updateNotification("Focus Session Finished!", "Great job! Take a well-deserved break.")
    }

    private fun updateNotification(title: String, content: String) {
        try {
            val notification = NotificationCompat.Builder(context, DetoxApplication.NOTIFICATION_CHANNEL_DETOX)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setOngoing(_pomodoroState.value.isRunning && !_pomodoroState.value.isPaused)
                .setOnlyAlertOnce(true)
                .build()
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Graceful notification handling
        }
    }

    suspend fun getTodayFocusMinutes(): Int = withContext(Dispatchers.IO) {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        focusSessionDao.getTodayFocusMinutes(midnight) ?: 0
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
