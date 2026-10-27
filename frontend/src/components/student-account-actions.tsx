import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  studentAccountStatus,
  type Student,
  type StudentAccountStatus,
  type StudentDecision,
} from '../domain'

export type ReviewStudent = (
  id: string,
  decision: StudentDecision,
  expected: StudentAccountStatus,
) => Promise<void>

export function StudentAccountActions({
  student,
  onReview,
  onDelete,
}: {
  student: Student
  onReview: ReviewStudent
  onDelete?: (id: string) => Promise<void>
}) {
  const status = studentAccountStatus(student)
  const [confirm, setConfirm] = useState<StudentAccountStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [deleting, setDeleting] = useState(false)
  async function remove() {
    if (!onDelete) return
    setBusy(true)
    try {
      await onDelete(student.id)
      toast.success('Declined student deleted. Group memberships have been cleared.')
    } catch (cause) {
      toast.error(
        cause instanceof Error ? cause.message : 'The account could not be deleted. Try again.',
      )
    } finally {
      setBusy(false)
    }
  }
  async function submit(decision: StudentDecision, expected: StudentAccountStatus) {
    setBusy(true)
    try {
      await onReview(student.id, decision, expected)
      setConfirm(null)
      toast.success(
        decision === 'approve'
          ? 'Student approved. They can now access CREST.'
          : expected === 'approved'
            ? 'Student access revoked.'
            : 'Student registration declined.',
      )
    } catch (cause) {
      toast.error(
        cause instanceof Error
          ? cause.message
          : 'The student account could not be updated. Try again.',
      )
    } finally {
      setBusy(false)
    }
  }
  return (
    <div
      className="student-account-actions"
      role="group"
      aria-label={`Account controls for ${student.name}`}
    >
      {deleting ? (
        <div className="account-decision-confirmation" role="alert">
          <p>
            Delete {student.name}’s declined registration? This permanently removes their student
            profile and group memberships. Published research stays in the library.
          </p>
          <div className="button-row">
            <Button variant="outline" disabled={busy} onClick={() => setDeleting(false)}>
              Cancel
            </Button>
            <Button variant="destructive" disabled={busy} onClick={remove}>
              {busy ? 'Deleting…' : 'Confirm delete'}
            </Button>
          </div>
        </div>
      ) : confirm ? (
        <div className="account-decision-confirmation">
          <p>
            {confirm === 'approved'
              ? `Revoke CREST access for ${student.name}?`
              : `Decline ${student.name}’s registration?`}{' '}
            Their profile and research records will be kept. You can approve them again from
            Students.
          </p>
          <div className="button-row">
            <Button variant="outline" disabled={busy} onClick={() => setConfirm(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={busy}
              onClick={() => submit('decline', confirm)}
            >
              {busy ? 'Saving…' : confirm === 'approved' ? 'Confirm revoke' : 'Confirm decline'}
            </Button>
          </div>
        </div>
      ) : (
        <div className="button-row">
          {status !== 'approved' && (
            <Button
              variant="outline"
              disabled={busy}
              onClick={() => submit('approve', status)}
              aria-label={
                status === 'declined'
                  ? `Restore access for ${student.name}`
                  : `Approve student account for ${student.name}`
              }
            >
              {busy ? 'Saving…' : status === 'declined' ? 'Restore access' : 'Approve account'}
            </Button>
          )}
          {status !== 'declined' && (
            <Button
              variant="outline"
              disabled={busy}
              onClick={() => setConfirm(status)}
              aria-label={`${status === 'approved' ? 'Revoke access' : 'Decline registration'} for ${student.name}`}
            >
              {status === 'approved' ? 'Revoke access' : 'Decline'}
            </Button>
          )}
          {status === 'declined' && onDelete && (
            <Button variant="destructive" disabled={busy} onClick={() => setDeleting(true)}>
              Delete student
            </Button>
          )}
        </div>
      )}
    </div>
  )
}
