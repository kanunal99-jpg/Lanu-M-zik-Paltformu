package com.example.service

/** Pure queue rules kept separate from Media3 so behavior can be tested without a device. */
object PlaybackQueuePolicy {
    fun nextAction(currentIndex: Int, queueSize: Int, repeatMode: RepeatMode): NextAction {
        if (queueSize <= 0) return NextAction.Stop
        return when {
            currentIndex < queueSize - 1 -> NextAction.MoveTo(currentIndex + 1)
            repeatMode == RepeatMode.ALL -> NextAction.MoveTo(0)
            else -> NextAction.Stop
        }
    }

    fun previousAction(currentIndex: Int, currentPositionMs: Long, queueSize: Int, repeatMode: RepeatMode): PreviousAction {
        if (queueSize <= 0) return PreviousAction.Stop
        if (currentPositionMs > 3_000L) return PreviousAction.RestartCurrent
        return when {
            currentIndex > 0 -> PreviousAction.MoveTo(currentIndex - 1)
            repeatMode == RepeatMode.ALL -> PreviousAction.MoveTo(queueSize - 1)
            else -> PreviousAction.RestartCurrent
        }
    }

    sealed interface NextAction {
        data class MoveTo(val index: Int) : NextAction
        data object Stop : NextAction
    }

    sealed interface PreviousAction {
        data class MoveTo(val index: Int) : PreviousAction
        data object RestartCurrent : PreviousAction
        data object Stop : PreviousAction
    }
}
