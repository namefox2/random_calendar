package com.letsgo.randomcalendar.ui.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object TimerManager {

    data class TimerState(
        val todoId: Long,
        val timerType: String,          // "NORMAL" or "SET"
        val isRunning: Boolean,
        val elapsedSeconds: Int,
        // 일반 타이머
        val goalSeconds: Int? = null,
        // 세트 타이머
        val currentSet: Int = 1,
        val totalSets: Int = 0,
        val isWorkPhase: Boolean = true,
        val workSeconds: Int = 0,
        val restSeconds: Int = 0,
        val phaseElapsedSeconds: Int = 0
    ) {
        val phaseTotal: Int get() = if (isWorkPhase) workSeconds else restSeconds
        val phaseRemaining: Int get() = (phaseTotal - phaseElapsedSeconds).coerceAtLeast(0)
        val phaseProgress: Int get() = if (phaseTotal > 0) (phaseElapsedSeconds * 100 / phaseTotal).coerceIn(0, 100) else 0
    }

    private val _state = MutableLiveData<TimerState?>()
    val state: LiveData<TimerState?> = _state

    var onAlarm: (() -> Unit)? = null
    var onPersist: ((Long, Int) -> Unit)? = null

    fun startNormal(
        todoId: Long,
        elapsedSeconds: Int,
        goalSeconds: Int?
    ) {
        _state.value = TimerState(
            todoId = todoId,
            timerType = "NORMAL",
            isRunning = true,
            elapsedSeconds = elapsedSeconds,
            goalSeconds = goalSeconds
        )
    }

    fun startSet(
        todoId: Long,
        elapsedSeconds: Int,
        workSeconds: Int,
        restSeconds: Int,
        totalSets: Int,
        currentSet: Int = 1,
        isWorkPhase: Boolean = true,
        phaseElapsedSeconds: Int = 0
    ) {
        _state.value = TimerState(
            todoId = todoId,
            timerType = "SET",
            isRunning = true,
            elapsedSeconds = elapsedSeconds,
            workSeconds = workSeconds,
            restSeconds = restSeconds,
            totalSets = totalSets,
            currentSet = currentSet,
            isWorkPhase = isWorkPhase,
            phaseElapsedSeconds = phaseElapsedSeconds
        )
    }

    fun pause() {
        _state.value = _state.value?.copy(isRunning = false)
    }

    fun resume() {
        _state.value = _state.value?.copy(isRunning = true)
    }

    fun reset(todoId: Long) {
        val current = _state.value ?: return
        if (current.todoId != todoId) return
        val reset = if (current.timerType == "SET") {
            current.copy(
                isRunning = false,
                elapsedSeconds = 0,
                currentSet = 1,
                isWorkPhase = true,
                phaseElapsedSeconds = 0
            )
        } else {
            current.copy(isRunning = false, elapsedSeconds = 0)
        }
        _state.value = reset
        onPersist?.invoke(todoId, 0)
    }

    fun clear() {
        _state.value = null
    }

    fun tick() {
        val current = _state.value ?: return
        if (!current.isRunning) return

        if (current.timerType == "NORMAL") {
            tickNormal(current)
        } else {
            tickSet(current)
        }
    }

    private fun tickNormal(state: TimerState) {
        val newElapsed = state.elapsedSeconds + 1
        val isGoalReached = state.goalSeconds != null && newElapsed >= state.goalSeconds
        _state.value = state.copy(
            elapsedSeconds = newElapsed,
            isRunning = !isGoalReached
        )
        onPersist?.invoke(state.todoId, newElapsed)
        if (isGoalReached) onAlarm?.invoke()
    }

    private fun tickSet(state: TimerState) {
        val newElapsed = state.elapsedSeconds + 1
        val newPhaseElapsed = state.phaseElapsedSeconds + 1
        val phaseTotal = if (state.isWorkPhase) state.workSeconds else state.restSeconds

        if (newPhaseElapsed >= phaseTotal) {
            // 페이즈 전환
            if (state.isWorkPhase) {
                // 운동 완료 → 휴식 시작
                onAlarm?.invoke()
                _state.value = state.copy(
                    elapsedSeconds = newElapsed,
                    isWorkPhase = false,
                    phaseElapsedSeconds = 0
                )
            } else {
                // 휴식 완료 → 다음 세트 or 전체 완료
                onAlarm?.invoke()
                val nextSet = state.currentSet + 1
                if (nextSet > state.totalSets) {
                    _state.value = state.copy(
                        elapsedSeconds = newElapsed,
                        isRunning = false,
                        phaseElapsedSeconds = phaseTotal
                    )
                } else {
                    _state.value = state.copy(
                        elapsedSeconds = newElapsed,
                        currentSet = nextSet,
                        isWorkPhase = true,
                        phaseElapsedSeconds = 0
                    )
                }
            }
        } else {
            _state.value = state.copy(
                elapsedSeconds = newElapsed,
                phaseElapsedSeconds = newPhaseElapsed
            )
        }
        onPersist?.invoke(state.todoId, newElapsed)
    }

    fun formatSeconds(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
