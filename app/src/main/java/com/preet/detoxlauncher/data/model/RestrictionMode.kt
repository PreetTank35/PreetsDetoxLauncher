package com.preet.detoxlauncher.data.model

/**
 * Restriction modes for the Digital Detox Engine.
 */
enum class RestrictionMode {
    /**
     * Shows a gentle warning and break suggestion, but allows user to continue.
     */
    SOFT,

    /**
     * Shows a full-screen block requiring emergency override to bypass.
     */
    STRICT,

    /**
     * Restricts access during defined recurring time windows.
     */
    SCHEDULED,

    /**
     * No restrictions applied.
     */
    OFF
}
