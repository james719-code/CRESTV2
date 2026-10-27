package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningRepository @Inject constructor(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private fun roleRef(uid: String, teacher: Boolean) = db.collection(if (teacher) FirestorePaths.TEACHERS else FirestorePaths.STUDENTS).document(uid)
    private fun inbox(uid: String, teacher: Boolean) = db.collection(if (teacher) "teacher_inboxes" else "student_inboxes").document(uid).collection("items")
    private fun requireSession(uid: String) { check(auth.currentUser?.uid == uid) { "Your account has changed. Reopen this page." } }

    fun observe(uid: String, teacher: Boolean) = callbackFlow {
        val listeners = mutableListOf<ListenerRegistration>()
        var studentStops = mutableListOf<ListenerRegistration>()
        var records = PlanningRecords()
        val received = mutableSetOf<String>()
        val required = if (teacher) setOf("groups", "milestones", "inbox", "students", "teachers") else setOf("groups", "milestones", "inbox")
        var active = true
        var watching = false
        var ownGroupId: String? = null
        var groupGeneration = 0
        var studentPending = emptyList<Pair<String, String>>()
        var teacherPending = emptyList<Pair<String, String>>()
        fun fail(error: Exception) { if (active) close(error) }
        fun publish(key: String) {
            if (!active) return
            received.add(key)
            records = records.copy(loading = !received.containsAll(required), pendingAccounts = studentPending + teacherPending)
            trySend(records)
        }
        fun listen(query: Query, key: String, update: (List<DocumentSnapshot>) -> Unit): ListenerRegistration =
            query.addSnapshotListener { snapshot, error ->
                if (!active) return@addSnapshotListener
                if (error != null) fail(error) else { update(snapshot?.documents.orEmpty()); publish(key) }
            }
        val roleListener = roleRef(uid, teacher).addSnapshotListener { role, error ->
            if (!active) return@addSnapshotListener
            if (error != null) { fail(error); return@addSnapshotListener }
            if (role?.getBoolean(if (teacher) "access" else "accepted") != true) {
                fail(IllegalStateException("Your school access is no longer approved.")); return@addSnapshotListener
            }
            if (!watching) {
                watching = true
                listeners += listen(inbox(uid, teacher), "inbox") { rows -> records = records.copy(notifications = rows.mapNotNull(::mapUpdate).sortedByDescending { it.observedAt }) }
                if (teacher) {
                    listeners += listen(db.collection("groups"), "groups") { rows -> records = records.copy(groups = rows.map(::mapGroup)) }
                    listeners += listen(db.collection("group_milestones"), "milestones") { rows -> records = records.copy(milestones = rows.mapNotNull(::mapMilestone)) }
                    listeners += listen(db.collection(FirestorePaths.STUDENTS).whereEqualTo("accepted", false), "students") { rows ->
                        studentPending = rows.filter { it.getBoolean("declined") != true }.map { "student:${it.id}" to it.getString("name").orEmpty() }
                    }
                    listeners += listen(db.collection(FirestorePaths.TEACHERS).whereEqualTo("access", false), "teachers") { rows ->
                        teacherPending = rows.map { "teacher:${it.id}" to it.getString("name").orEmpty() }
                    }
                }
            }
            if (!teacher) {
                val groupId = role.getString("groupId").orEmpty().ifBlank { role.getString("group_id").orEmpty() }
                if (groupId != ownGroupId) {
                    ownGroupId = groupId
                    val currentGroup = ++groupGeneration
                    studentStops.forEach { it.remove() }; studentStops.clear()
                    received.removeAll(setOf("groups", "milestones"))
                    records = records.copy(groups = emptyList(), milestones = emptyList())
                    if (groupId.isBlank()) { publish("groups"); publish("milestones") }
                    else {
                        studentStops += db.collection("groups").document(groupId).addSnapshotListener { group, groupError ->
                            if (!active || currentGroup != groupGeneration) return@addSnapshotListener
                            if (groupError != null) { fail(groupError); return@addSnapshotListener }
                            val member = (group?.get("group_member") as? List<*>)?.contains(uid) == true
                            records = records.copy(groups = if (group?.exists() == true && member) listOf(mapGroup(group)) else emptyList())
                            publish("groups")
                        }
                        studentStops += db.collection("group_milestones").whereEqualTo("groupId", groupId).addSnapshotListener { snapshot, milestoneError ->
                            if (!active || currentGroup != groupGeneration) return@addSnapshotListener
                            if (milestoneError != null) fail(milestoneError)
                            else { records = records.copy(milestones = snapshot?.documents.orEmpty().mapNotNull(::mapMilestone)); publish("milestones") }
                        }
                    }
                }
            }
        }
        awaitClose { active = false; roleListener.remove(); listeners.forEach { it.remove() }; studentStops.forEach { it.remove() } }
    }

    suspend fun save(uid: String, input: MilestoneInput, existing: ResearchMilestone?) {
        requireSession(uid)
        val value = PlanningDates.validate(input)
        val ref = existing?.let { db.collection("group_milestones").document(it.id) } ?: db.collection("group_milestones").document()
        db.runTransaction { tx ->
            check(tx.get(roleRef(uid, true)).getBoolean("access") == true) { "An approved teacher account is required." }
            check(tx.get(db.collection("groups").document(value.groupId)).exists()) { "This group no longer exists." }
            val record = tx.get(ref)
            PlanningDates.checkVersion(if (record.exists()) record.getLong("version") else null, existing?.version)
            if (existing != null) check(record.getString("groupId") == value.groupId) { "A milestone cannot be moved to another group." }
            val data = mutableMapOf<String, Any>("groupId" to value.groupId, "title" to value.title, "stage" to value.stage.value,
                "dueDate" to value.dueDate, "version" to (existing?.version?.plus(1) ?: 1L), "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to uid)
            if (existing == null) {
                data.putAll(mapOf("completedAt" to 0L, "createdAt" to FieldValue.serverTimestamp(), "createdBy" to uid)); tx.set(ref, data)
            } else tx.update(ref, data)
        }.await()
    }
    suspend fun change(uid: String, milestone: ResearchMilestone, action: String) {
        requireSession(uid)
        require(action in listOf("complete", "reopen", "delete"))
        db.runTransaction { tx ->
            check(tx.get(roleRef(uid, true)).getBoolean("access") == true) { "An approved teacher account is required." }
            val ref = db.collection("group_milestones").document(milestone.id)
            val record = tx.get(ref)
            PlanningDates.checkVersion(if (record.exists()) record.getLong("version") else null, milestone.version)
            if (action == "delete") tx.delete(ref) else tx.update(ref, mapOf("completedAt" to if (action == "complete") System.currentTimeMillis() else 0L,
                "version" to milestone.version + 1, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to uid))
        }.await()
    }
    suspend fun capture(uid: String, teacher: Boolean, events: List<SchoolUpdate>) {
        requireSession(uid)
        events.chunked(100).forEach { portion ->
            db.runTransaction { tx ->
                check(tx.get(roleRef(uid, teacher)).getBoolean(if (teacher) "access" else "accepted") == true) { "An approved school account is required." }
                val refs = portion.map { inbox(uid, teacher).document(it.id) }
                val previous = refs.map { tx.get(it) }
                portion.forEachIndexed { index, event -> if (!previous[index].exists()) tx.set(refs[index], mapOf(
                    "kind" to event.kind, "groupId" to event.groupId, "title" to event.title, "message" to event.message,
                    "observedAt" to FieldValue.serverTimestamp(), "read" to false)) }
            }.await()
        }
    }
    suspend fun markRead(uid: String, teacher: Boolean, ids: List<String>, read: Boolean) {
        requireSession(uid)
        ids.chunked(100).forEach { portion ->
            db.runTransaction { tx ->
                check(tx.get(roleRef(uid, teacher)).getBoolean(if (teacher) "access" else "accepted") == true) { "An approved school account is required." }
                portion.forEach { tx.update(inbox(uid, teacher).document(it), "read", read) }
            }.await()
        }
    }
    private fun mapGroup(row: DocumentSnapshot) = PlanningGroup(row.id, row.getString("group_name").orEmpty(), row.getString("research_title").orEmpty(),
        row.getString("file_link").orEmpty(), row.getBoolean("uploaded") == true, row.getBoolean("accepted_research") == true,
        (row.get("comments") as? List<*>)?.filterIsInstance<String>().orEmpty())
    private fun mapMilestone(row: DocumentSnapshot): ResearchMilestone? = runCatching {
        val input = PlanningDates.validate(MilestoneInput(row.getString("groupId").orEmpty(), row.getString("title").orEmpty(),
            MilestoneStage.entries.first { it.value == row.getString("stage") }, row.getString("dueDate").orEmpty()))
        ResearchMilestone(row.id, input.groupId, input.title, input.stage, input.dueDate, row.timestampMillis("completedAt"), row.getLong("version") ?: 1)
    }.getOrNull()
    private fun mapUpdate(row: DocumentSnapshot): SchoolUpdate? {
        val kind = row.getString("kind") ?: return null
        if (kind !in listOf("submission", "approval", "revision", "account")) return null
        return SchoolUpdate(row.id, kind, row.getString("groupId").orEmpty(), row.getString("title").orEmpty(),
            row.getString("message").orEmpty(), row.timestampMillis("observedAt"), row.getBoolean("read") == true)
    }
}
