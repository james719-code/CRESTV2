package com.bdbshs.crest.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ResearchPlanningTest {
    @Test fun notificationIdentitiesMatchTheWebsiteIncludingUnicodeAndSlashes() {
        assertEquals("adb85b9ddb71c55489c95adebfc2dd05dcd4b58828023684f1b9415635a0e8fe", PlanningInbox.eventId("submission", "g1", "sb_123"))
        assertEquals("48bb3544b5bdbf88fa4b8e95d4b12b35740670b3ef170e469e021dfbd172767f", PlanningInbox.eventId("revision", "g1", 0, "Clarify / samples \"ñ\" 🧪"))
    }
    @Test fun deadlineIncludesTheEntireManilaDay() {
        val item = ResearchMilestone("m", "g", "Proposal", MilestoneStage.PROPOSAL, "2026-10-27")
        assertEquals(MilestoneStatus.TODAY, item.status(PlanningDates.today(Instant.parse("2026-10-27T15:59:59Z").toEpochMilli())))
        assertEquals(MilestoneStatus.OVERDUE, item.status(PlanningDates.today(Instant.parse("2026-10-27T16:00:00Z").toEpochMilli())))
        assertEquals(MilestoneStatus.UPCOMING, item.status(LocalDate.parse("2026-10-26")))
        assertEquals(MilestoneStatus.COMPLETED, item.copy(completedAt = 1).status(LocalDate.parse("2026-10-28")))
    }
    @Test fun invalidAndIncompletePlansAreRejected() {
        val input = MilestoneInput("g", "  Chapter 1  ", MilestoneStage.CHAPTER, "2028-02-29")
        assertEquals("Chapter 1", PlanningDates.validate(input).title)
        for (date in listOf("2026-02-29", "2026-04-31", "2026-13-01", "2026-1-01", "1999-12-31", "2101-01-01"))
            assertThrows(IllegalArgumentException::class.java) { PlanningDates.validate(input.copy(dueDate = date)) }
        assertThrows(IllegalArgumentException::class.java) { PlanningDates.validate(input.copy(groupId = "")) }
        assertThrows(IllegalArgumentException::class.java) { PlanningDates.validate(input.copy(title = " ")) }
        assertThrows(IllegalArgumentException::class.java) { PlanningDates.validate(input.copy(title = "x".repeat(201))) }
    }
    @Test fun staleEditsAndDeletionsAreRejected() {
        PlanningDates.checkVersion(null, null)
        PlanningDates.checkVersion(2, 2)
        assertThrows(IllegalStateException::class.java) { PlanningDates.checkVersion(3, 2) }
        assertThrows(IllegalStateException::class.java) { PlanningDates.checkVersion(null, 2) }
    }
    @Test fun inboxIncludesSubmissionsApprovalsRetainedFeedbackAndPendingAccounts() {
        val records = PlanningRecords(groups = listOf(
            PlanningGroup("g", "Science", fileId = "one", uploaded = true, comments = listOf("Clarify sources")),
            PlanningGroup("approved", "Literature", accepted = true)
        ), pendingAccounts = listOf("student:s" to "Student"))
        val events = PlanningInbox.candidates(records)
        assertEquals(setOf("submission", "revision", "approval", "account"), events.map { it.kind }.toSet())
        assertEquals(events.size, events.map { it.id }.toSet().size)
        val next = PlanningInbox.candidates(records.copy(groups = records.groups.map { if (it.id == "g") it.copy(fileId = "replacement") else it }))
        assertNotEquals(events.first { it.kind == "submission" }.id, next.first { it.kind == "submission" }.id)
        assertEquals(events.first { it.kind == "revision" }.id, next.first { it.kind == "revision" }.id)
    }
}
