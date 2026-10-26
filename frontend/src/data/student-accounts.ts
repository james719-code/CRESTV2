import { doc, runTransaction, type Firestore } from 'firebase/firestore'
import {
  studentDecisionUpdate,
  type StudentAccountStatus,
  type StudentDecision,
} from '../domain.ts'

export async function reviewStudentAccount(
  db: Firestore,
  teacherId: string,
  id: string,
  decision: StudentDecision,
  expected: StudentAccountStatus,
) {
  const roleRef = doc(db, 'users/user_details/teachers', teacherId)
  const accountRef = doc(db, 'users/user_details/students', id)
  await runTransaction(db, async (tx) => {
    const teacher = await tx.get(roleRef)
    const account = await tx.get(accountRef)
    if (teacher.data()?.access !== true) throw new Error('An approved teacher account is required.')
    if (!account.exists()) throw new Error('This student account no longer exists.')
    const data = account.data()
    const update = studentDecisionUpdate(
      { accepted: data.accepted === true, declined: data.declined === true },
      decision,
      expected,
    )
    tx.update(accountRef, update)
  })
}
