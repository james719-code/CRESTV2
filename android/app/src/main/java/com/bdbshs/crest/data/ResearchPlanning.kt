package com.bdbshs.crest.data

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class MilestoneStage(val value: String, val label: String) {
    PROPOSAL("proposal", "Proposal"), CHAPTER("chapter", "Chapter"),
    DEFENSE("defense", "Defense"), FINAL("final", "Final manuscript")
}
enum class MilestoneStatus(val label: String) {
    OVERDUE("Overdue"), TODAY("Due today"), UPCOMING("Upcoming"), COMPLETED("Completed")
}
data class MilestoneInput(val groupId: String, val title: String, val stage: MilestoneStage, val dueDate: String)
data class ResearchMilestone(
    val id: String, val groupId: String, val title: String, val stage: MilestoneStage,
    val dueDate: String, val completedAt: Long = 0, val version: Long = 1
) {
    fun input() = MilestoneInput(groupId, title, stage, dueDate)
    fun status(today: LocalDate = PlanningDates.today()): MilestoneStatus = when {
        completedAt > 0 -> MilestoneStatus.COMPLETED
        LocalDate.parse(dueDate) < today -> MilestoneStatus.OVERDUE
        LocalDate.parse(dueDate) == today -> MilestoneStatus.TODAY
        else -> MilestoneStatus.UPCOMING
    }
}
object PlanningDates {
    val zone: ZoneId = ZoneId.of("Asia/Manila")
    fun today(now: Long = System.currentTimeMillis()): LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    fun validate(input: MilestoneInput): MilestoneInput {
        require(input.groupId.isNotBlank()) { "Choose a research group." }
        require(input.title.trim().length in 1..200) { "Enter a milestone title of up to 200 characters." }
        val date = runCatching { LocalDate.parse(input.dueDate) }.getOrNull()
        require(date != null && date.toString() == input.dueDate && date.year in 2000..2100) { "Choose a valid deadline between 2000 and 2100." }
        return input.copy(title = input.title.trim())
    }
    fun checkVersion(actual: Long?, expected: Long?) {
        check(actual == expected) { "This milestone has changed. Check the latest details and try again." }
    }
}
data class PlanningGroup(
    val id: String, val name: String, val title: String = "", val fileId: String = "",
    val uploaded: Boolean = false, val accepted: Boolean = false, val comments: List<String> = emptyList()
)
data class SchoolUpdate(
    val id: String, val kind: String, val groupId: String, val title: String,
    val message: String, val observedAt: Long = 0, val read: Boolean = false
)
data class PlanningRecords(
    val groups: List<PlanningGroup> = emptyList(), val milestones: List<ResearchMilestone> = emptyList(),
    val notifications: List<SchoolUpdate> = emptyList(), val pendingAccounts: List<Pair<String, String>> = emptyList(),
    val loading: Boolean = true
)
object PlanningInbox {
    // Match the website's JSON.stringify + SHA-256 identities, including Unicode and slashes.
    fun eventId(vararg parts: Any): String {
        fun quote(value: String) = buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '"' -> append("\\\""); '\\' -> append("\\\\"); '\b' -> append("\\b")
                    '\u000C' -> append("\\f"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
                    else -> if (char.code < 32) append("\\u%04x".format(char.code)) else append(char)
                }
            }
            append('"')
        }
        val key = parts.joinToString(",", "[", "]") { if (it is Number) it.toString() else quote(it.toString()) }
        return MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
    fun candidates(records: PlanningRecords): List<SchoolUpdate> = buildList {
        records.groups.forEach { group ->
            if (group.uploaded && !group.accepted) add(SchoolUpdate(eventId("submission", group.id, group.fileId),
                "submission", group.id, "${group.name} submitted research", group.title.ifBlank { group.name }))
            if (group.accepted) add(SchoolUpdate(eventId("approval", group.id, group.fileId),
                "approval", group.id, "${group.name}: research approved", group.title.ifBlank { group.name }))
            group.comments.forEachIndexed { index, comment -> add(SchoolUpdate(eventId("revision", group.id, index, comment),
                "revision", group.id, "${group.name}: revision feedback", comment)) }
        }
        records.pendingAccounts.forEach { (key, name) ->
            val role = key.substringBefore(':')
            add(SchoolUpdate(eventId(role, key.substringAfter(':')), "account", "",
                "${role.replaceFirstChar { it.titlecase() }} registration awaiting approval", name))
        }
    }.map { it.copy(title = it.title.take(240), message = it.message.take(4000)) }
}
