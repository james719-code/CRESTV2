/**
 * `unregistered` — signed in, no profile yet (can register as a teacher).
 * `pending` — teacher profile exists and is waiting for approval.
 * `student` — the account belongs to a student, which this workspace does not serve.
 */
export type SessionState =
  | 'loading'
  | 'signed-out'
  | 'ready'
  | 'unregistered'
  | 'pending'
  | 'student'
  | 'error'

/** True for sessions where the person is signed in but has no teacher access yet. */
export const lacksTeacherAccess = (state: SessionState) =>
  state === 'unregistered' || state === 'pending' || state === 'student'

export type AccountInfo = { name: string; email: string }
