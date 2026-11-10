package com.preet.detoxlauncher.data.model

/**
 * Valid reasons selected during Intentional App Opening reflection.
 */
enum class IntentionalReason(val displayName: String) {
    STUDY("Study"),
    WORK("Work"),
    COMMUNICATION("Communication"),
    ENTERTAINMENT("Entertainment"),
    JUST_BROWSING("Just browsing")
}
