package com.bdbshs.crest.branding

/** Presentation labels, independent of the stored workflow fields. */
enum class CrestSubmissionStatus(val label: String) {
    APPROVED("Approved"),
    PENDING("Pending review"),
    CHANGES("Changes requested"),
    NOT_SUBMITTED("Not submitted");

    companion object {
        fun from(approved: Boolean, uploaded: Boolean, hasFeedback: Boolean) = when {
            approved -> APPROVED
            uploaded -> PENDING
            hasFeedback -> CHANGES
            else -> NOT_SUBMITTED
        }
    }
}
