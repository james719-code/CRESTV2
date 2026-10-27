import {
  collection,
  doc,
  getDocsFromServer,
  runTransaction,
  type Firestore,
} from 'firebase/firestore'

const students = 'users/user_details/students'
const teachers = 'users/user_details/teachers'
const strings = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((id): id is string => typeof id === 'string' && !!id) : []

export async function deleteDeclinedStudent(db: Firestore, teacherId: string, id: string) {
  // group_member is deliberately unindexed. Teachers already have collection access.
  const memberships = await getDocsFromServer(collection(db, 'groups'))
  await runTransaction(db, async (tx) => {
    const teacher = await tx.get(doc(db, teachers, teacherId))
    const accountRef = doc(db, students, id)
    const account = await tx.get(accountRef)
    if (teacher.data()?.access !== true) throw new Error('An approved teacher account is required.')
    if (!account.exists()) throw new Error('This student account no longer exists.')
    const data = account.data()
    if (data.accepted === true || data.declined !== true)
      throw new Error('Only declined student accounts can be deleted. Check the current status.')
    const groupIds = new Set([
      ...memberships.docs
        .filter((group) => strings(group.data().group_member).includes(id))
        .map((group) => group.id),
      ...[data.groupId, data.group_id].filter(
        (value): value is string => typeof value === 'string' && !!value,
      ),
    ])
    const groups = await Promise.all(
      [...groupIds].map((groupId) => tx.get(doc(db, 'groups', groupId))),
    )
    for (const group of groups) {
      if (group.exists() && strings(group.data().group_member).includes(id))
        tx.update(group.ref, {
          group_member: strings(group.data().group_member).filter((member) => member !== id),
        })
    }
    tx.delete(accountRef)
  })
}

export async function deleteResearchGroup(db: Firestore, teacherId: string, id: string) {
  // Legacy group_id is deliberately unindexed. Server data is required for cleanup.
  const studentsSnapshot = await getDocsFromServer(collection(db, students))
  const linkedIds = studentsSnapshot.docs
    .filter((student) => student.data().groupId === id || student.data().group_id === id)
    .map((student) => student.id)
  await runTransaction(db, async (tx) => {
    const teacher = await tx.get(doc(db, teachers, teacherId))
    const groupRef = doc(db, 'groups', id)
    const group = await tx.get(groupRef)
    if (teacher.data()?.access !== true) throw new Error('An approved teacher account is required.')
    if (!group.exists()) throw new Error('This group no longer exists.')
    const memberIds = new Set([...strings(group.data().group_member), ...linkedIds])
    const members = await Promise.all([...memberIds].map((uid) => tx.get(doc(db, students, uid))))
    for (const member of members) {
      if (!member.exists()) continue
      const data = member.data()
      const update: Record<string, string> = {}
      if (data.groupId === id) update.groupId = ''
      if (data.group_id === id) update.group_id = ''
      if (Object.keys(update).length) tx.update(member.ref, update)
    }
    tx.delete(groupRef)
    // Published research and its PDF may be shared with the library; retain them.
  })
}
