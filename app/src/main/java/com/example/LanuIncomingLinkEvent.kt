package com.example

/**
 * Distinguishes two successive external intents carrying the same URI.
 * A new event must be observable in Compose without recreating the Activity
 * or its media-controller-owning ViewModel.
 */
internal data class LanuIncomingLinkEvent(
    val rawUri: String?,
    val sequence: Long
) {
    companion object {
        fun next(current: LanuIncomingLinkEvent, rawUri: String?): LanuIncomingLinkEvent =
            LanuIncomingLinkEvent(rawUri, current.sequence + 1L)
    }
}
