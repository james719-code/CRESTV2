package com.bdbshs.crest.branding

import org.junit.Assert.assertEquals
import org.junit.Test

class CrestSubmissionStatusTest {
    @Test
    fun currentSubmissionTakesPrecedenceOverOlderFeedback() {
        assertEquals("Not submitted", CrestSubmissionStatus.from(false, false, false).label)
        assertEquals("Changes requested", CrestSubmissionStatus.from(false, false, true).label)
        assertEquals("Pending review", CrestSubmissionStatus.from(false, true, true).label)
        assertEquals("Approved", CrestSubmissionStatus.from(true, true, true).label)
    }
}
