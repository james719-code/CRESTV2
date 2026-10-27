import { useState } from 'react'
import {
  ArrowRight,
  ArrowLeft,
  Check,
  CheckCircle,
  ChatText,
  FilePdf,
  Info,
  UploadSimple,
  UsersThree,
} from '@/icons'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  groupStatus,
  initials,
  studentGroup,
  studentAccountLabels,
  studentAccountStatus,
  type ResearchGroup,
  type ResearchType,
  type Student,
  type UploadInput,
  type Workspace,
} from '../domain'
import { StatusBadge, StrandBadge } from './workspace'
import { useDisplayPreferences } from '../display-context'
import { useReturnFocus } from './use-return-focus'
import { StudentAccountActions, type ReviewStudent } from './student-account-actions'

import type { ReaderTarget } from '../routing'
import { Milestones, type MilestoneActions } from './milestones'

export function GroupDetails({
  group,
  workspace,
  onClose,
  onReview,
  onFile,
  onDelete,
  onSaveMilestone,
  onChangeMilestone,
}: {
  group?: ResearchGroup
  workspace: Workspace
  onClose: () => void
  onReview: (id: string, decision: 'approve' | 'changes', feedback?: string) => Promise<void>
  onFile: (file: ReaderTarget) => void
  onDelete: (id: string) => void
  onSaveMilestone: MilestoneActions['onSave']
  onChangeMilestone: MilestoneActions['onChange']
}) {
  const { preferences } = useDisplayPreferences()
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [decision, setDecision] = useState<'approve' | 'changes' | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)
  function remove() {
    if (!group) return
    onDelete(group.id)
  }
  const status = group && groupStatus(group)
  async function submit(action: 'approve' | 'changes') {
    if (!group) return
    if (action === 'changes' && !feedback.trim()) {
      setError('Write a short note explaining what the group should revise.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await onReview(group.id, action, feedback)
      toast.success(
        action === 'approve'
          ? 'Research approved and added to the library.'
          : 'Feedback shared. The group can revise and submit again.',
      )
      setDecision(null)
      setFeedback('')
      onClose()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'The review could not be saved. Try again.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <article className="group-detail-page">
      <Button variant="outline" onClick={onClose} disabled={busy}>
        <ArrowLeft size={18} /> Back to groups
      </Button>
      {!group && (
        <p className="notice-box" role="status">
          This group is no longer available. Return to groups to choose another.
        </p>
      )}
      {group && (
        <div className="group-detail-body">
          <div className="flex flex-wrap items-center gap-2">
            <StrandBadge strand={group.strand} />
            <StatusBadge status={groupStatus(group)} />
          </div>
          <section className="detail-section">
            <h3>Current research</h3>
            <h2 className="detail-research-title">
              {group.title || 'The group is preparing their research'}
            </h2>
            {group.type && <p className="capitalize muted">{group.type} research</p>}
            {group.fileId && (
              <button
                className="pdf-attachment"
                onClick={() => onFile({ source: 'groups', id: group.id })}
              >
                <span className="pdf-icon">
                  <FilePdf size={26} />
                </span>
                <span>
                  <strong>Research manuscript</strong>
                  <small>Open the submitted PDF</small>
                </span>
                <ArrowRight size={20} />
              </button>
            )}
          </section>
          <section className="detail-section">
            <h3>
              <UsersThree size={18} />
              Group members <span className="muted">({group.memberIds.length})</span>
            </h3>
            <div className="member-list">
              {group.memberIds.map((id) => {
                const student = workspace.students.find((item) => item.id === id)
                return (
                  <div key={id}>
                    <span className="student-avatar">{initials(student?.name || 'Student')}</span>
                    <span>{student?.name || 'Student record unavailable'}</span>
                  </div>
                )
              })}
              {!group.memberIds.length && (
                <p className="muted">No members are recorded for this group.</p>
              )}
            </div>
          </section>
          <section className="detail-section">
            <h3>Submission progress</h3>
            <ol className="progress-steps">
              <li className="complete">
                <span>
                  <Check size={13} />
                </span>
                <div>
                  <strong>Group formed</strong>
                  <p>Students are working together.</p>
                </div>
              </li>
              <li className={status === 'preparing' ? 'current' : 'complete'}>
                <span>{status === 'preparing' ? '2' : <Check size={13} />}</span>
                <div>
                  <strong>{status === 'changes' ? 'Revising research' : 'Prepare & submit'}</strong>
                  <p>
                    {status === 'changes'
                      ? 'Your feedback is with the group.'
                      : 'The group prepares their manuscript.'}
                  </p>
                </div>
              </li>
              <li
                className={
                  status === 'pending' ? 'current' : status === 'approved' ? 'complete' : ''
                }
              >
                <span>{status === 'approved' ? <Check size={13} /> : '3'}</span>
                <div>
                  <strong>Teacher review</strong>
                  <p>
                    {status === 'pending'
                      ? 'This submission is ready for you.'
                      : 'Read the PDF and share feedback.'}
                  </p>
                </div>
              </li>
              <li className={status === 'approved' ? 'complete' : ''}>
                <span>{status === 'approved' ? <Check size={13} /> : '4'}</span>
                <div>
                  <strong>Approved for the library</strong>
                  <p>Preserved for the next researchers.</p>
                </div>
              </li>
            </ol>
            <p className="small-note">
              <Info size={15} />
              Based on group and submission records.
            </p>
          </section>
          <Milestones
            workspace={workspace}
            groupId={group.id}
            onSave={onSaveMilestone}
            onChange={onChangeMilestone}
          />
          <section className="detail-section">
            <h3>
              <ChatText size={18} />
              Teacher feedback
            </h3>
            {group.comments.length ? (
              <div className="feedback-list">
                {group.comments.map((comment, index) => (
                  <div key={index}>
                    <small>Feedback {index + 1}</small>
                    <p>{comment}</p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="muted">No feedback has been shared yet.</p>
            )}
          </section>
          {status === 'pending' && (
            <section className="review-actions">
              {preferences.guidance && (
                <div className="review-reading-guide">
                  <h3>Before you finish this review</h3>
                  <ol>
                    <li>Open and read the research PDF.</li>
                    <li>Add clear feedback if the group needs to make changes.</li>
                    <li>Choose Request changes or Approve research.</li>
                  </ol>
                </div>
              )}
              <h3>Your review</h3>
              <p className="muted">Check the manuscript before making your decision.</p>
              <Label htmlFor="review-feedback">
                Feedback for the group <span className="muted">(required for changes)</span>
              </Label>
              <Textarea
                id="review-feedback"
                value={feedback}
                maxLength={4000}
                onChange={(event) => {
                  setFeedback(event.target.value)
                  setError('')
                  setDecision(null)
                }}
                placeholder="Be specific about what is working and what needs attention…"
                rows={4}
                disabled={busy}
              />
              {decision === 'approve' ? (
                <div className="approval-confirmation">
                  <CheckCircle size={22} />
                  <p>Approve this manuscript? It will be added to the shared research library.</p>
                  <div>
                    <Button variant="outline" disabled={busy} onClick={() => setDecision(null)}>
                      Back
                    </Button>
                    <Button disabled={busy} onClick={() => submit('approve')}>
                      {busy ? 'Approving…' : 'Confirm approval'}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="button-row">
                  <Button variant="outline" disabled={busy} onClick={() => submit('changes')}>
                    <ChatText size={17} />
                    {busy ? 'Saving…' : 'Request changes'}
                  </Button>
                  <Button disabled={busy} onClick={() => setDecision('approve')}>
                    <Check size={17} />
                    Approve research
                  </Button>
                </div>
              )}
            </section>
          )}
          <section className="detail-section group-delete-section">
            <h3>Delete group</h3>
            <p className="muted">
              Remove this group, its submission record, and feedback. Members can create or join
              another group. Published research and PDFs remain in the library.
            </p>
            {confirmDelete ? (
              <div className="account-decision-confirmation" role="alert">
                <p>Permanently delete {group.name}? This cannot be undone.</p>
                <div className="button-row">
                  <Button variant="outline" disabled={busy} onClick={() => setConfirmDelete(false)}>
                    Cancel
                  </Button>
                  <Button variant="destructive" disabled={busy} onClick={remove}>
                    {busy ? 'Deleting…' : 'Confirm delete group'}
                  </Button>
                </div>
              </div>
            ) : (
              <Button variant="destructive" disabled={busy} onClick={() => setConfirmDelete(true)}>
                Delete group
              </Button>
            )}
          </section>
          {error && (
            <p role="alert" className="inline-error">
              {error}
            </p>
          )}
        </div>
      )}
    </article>
  )
}

export function StudentDetails({
  student,
  workspace,
  onClose,
  onGroup,
  onReviewStudent,
  onDeleteStudent,
}: {
  student: Student | null
  workspace: Workspace
  onClose: () => void
  onGroup: (id: string) => void
  onReviewStudent: ReviewStudent
  onDeleteStudent: (id: string) => Promise<void>
}) {
  const group = student && studentGroup(student, workspace.groups)
  const restoreFocus = useReturnFocus()
  return (
    <Dialog
      open={!!student}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent className="standard-dialog" onCloseAutoFocus={restoreFocus}>
        <DialogHeader>
          <DialogTitle>Student management</DialogTitle>
          <DialogDescription>
            Manage account access and check the student’s research group.
          </DialogDescription>
        </DialogHeader>
        {student && (
          <>
            <div className="student-profile">
              <span className="student-avatar large-avatar">{initials(student.name)}</span>
              <div>
                <h2>{student.name}</h2>
                <StrandBadge strand={student.strand} />
              </div>
            </div>
            <div className="student-detail-row">
              <span>Account status</span>
              <strong>{studentAccountLabels[studentAccountStatus(student)]}</strong>
            </div>
            <StudentAccountActions
              student={student}
              onReview={onReviewStudent}
              onDelete={async (id) => {
                await onDeleteStudent(id)
                onClose()
              }}
            />
            <div className="student-detail-row">
              <span>Research group</span>
              <strong>{group?.name || 'No group yet'}</strong>
            </div>
            <div className="student-detail-row">
              <span>Research status</span>
              {group ? (
                <StatusBadge status={groupStatus(group)} />
              ) : (
                <span className="muted">Not started</span>
              )}
            </div>
            {group ? (
              <>
                <p className="muted">
                  {group.title || 'This group has not submitted a research paper yet.'}
                </p>
                <Button
                  onClick={() => {
                    onClose()
                    onGroup(group.id)
                  }}
                >
                  View group progress
                  <ArrowRight size={16} />
                </Button>
              </>
            ) : (
              <p className="notice-box">
                Approved students can create or join a research group in the CREST app.
              </p>
            )}
          </>
        )}
      </DialogContent>
    </Dialog>
  )
}

export function UploadDialog({
  kind,
  onClose,
  onUpload,
}: {
  kind: 'research' | 'document' | null
  onClose: () => void
  onUpload: (input: UploadInput) => Promise<void>
}) {
  const restoreFocus = useReturnFocus()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [authors, setAuthors] = useState('')
  const [strand, setStrand] = useState('STEM')
  const [type, setType] = useState<ResearchType>('qualitative')
  const [file, setFile] = useState<File | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!kind) return
    if (!title.trim()) {
      setError('Give this resource a title.')
      return
    }
    if (kind === 'research' && !authors.split(',').some((name) => name.trim())) {
      setError('Add at least one author.')
      return
    }
    if (!file) {
      setError('Choose the PDF you want to share.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Choose a PDF smaller than 5 MB.')
      return
    }
    if (!file.size) {
      setError('This file is empty. Choose a different PDF.')
      return
    }
    setBusy(true)
    setError('')
    try {
      if (new TextDecoder().decode(await file.slice(0, 5).arrayBuffer()) !== '%PDF-')
        throw new Error('This file is not a valid PDF. Choose a PDF document.')
      await onUpload({
        kind,
        title: title.trim(),
        description: description.trim(),
        authors: authors
          .split(',')
          .map((name) => name.trim())
          .filter(Boolean),
        strand,
        type,
        file,
      })
      toast.success(
        kind === 'document' ? 'Document shared with the school.' : 'Research added to the library.',
      )
      onClose()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Your file could not be shared. Try again.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <Dialog
      open={!!kind}
      onOpenChange={(open) => {
        if (!open && !busy) onClose()
      }}
    >
      <DialogContent
        className="upload-dialog"
        onCloseAutoFocus={restoreFocus}
        onInteractOutside={(event) => {
          if (busy) event.preventDefault()
        }}
        onEscapeKeyDown={(event) => {
          if (busy) event.preventDefault()
        }}
        showCloseButton={!busy}
      >
        <DialogHeader>
          <span className="dialog-feature-icon">
            <UploadSimple size={25} />
          </span>
          <DialogTitle>
            {kind === 'document' ? 'Share a school document' : 'Add research to the library'}
          </DialogTitle>
          <DialogDescription>
            {kind === 'document'
              ? 'Give students a useful template, guide, or reference.'
              : 'Preserve a research paper for students and teachers to discover.'}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="resource-form">
          <div className="field">
            <Label htmlFor="resource-title">
              {kind === 'document' ? 'Document title' : 'Research title'}
            </Label>
            <Input
              id="resource-title"
              value={title}
              maxLength={300}
              onChange={(event) => setTitle(event.target.value)}
              placeholder={
                kind === 'document'
                  ? 'e.g. Research manuscript template'
                  : 'Enter the full research title'
              }
              required
              disabled={busy}
            />
          </div>
          {kind === 'document' ? (
            <div className="field">
              <Label htmlFor="resource-description">
                Description <span className="muted">(optional)</span>
              </Label>
              <Textarea
                id="resource-description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                maxLength={2000}
                placeholder="How should students use this resource?"
                disabled={busy}
              />
            </div>
          ) : (
            <>
              <div className="field">
                <Label htmlFor="research-authors">Authors</Label>
                <Input
                  id="research-authors"
                  value={authors}
                  onChange={(event) => setAuthors(event.target.value)}
                  placeholder="Separate author names with commas"
                  maxLength={2000}
                  required
                  disabled={busy}
                />
              </div>
              <div className="form-columns">
                <div className="field">
                  <Label htmlFor="research-strand">Strand</Label>
                  <Select value={strand} onValueChange={setStrand} disabled={busy}>
                    <SelectTrigger id="research-strand">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {['STEM', 'HUMSS', 'ABM', 'TVL', 'GAS'].map((item) => (
                        <SelectItem key={item} value={item}>
                          {item}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="field">
                  <Label htmlFor="research-type">Research type</Label>
                  <Select
                    value={type}
                    onValueChange={(value) => setType(value as ResearchType)}
                    disabled={busy}
                  >
                    <SelectTrigger id="research-type">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="qualitative">Qualitative</SelectItem>
                      <SelectItem value="quantitative">Quantitative</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </>
          )}
          <div className="field">
            <Label htmlFor="resource-file">PDF document</Label>
            <label className="upload-zone" htmlFor="resource-file">
              <UploadSimple size={28} />
              <strong>{file ? file.name : 'Choose a PDF to share'}</strong>
              <span>
                {file
                  ? `${(file.size / 1024 / 1024).toFixed(2)} MB · Click to choose another file`
                  : 'PDF only · Up to 5 MB'}
              </span>
              <Input
                id="resource-file"
                type="file"
                accept=".pdf,application/pdf"
                onChange={(event) => {
                  setFile(event.target.files?.[0] || null)
                  setError('')
                }}
                disabled={busy}
              />
            </label>
          </div>
          {error && (
            <p className="inline-error" role="alert">
              {error}
            </p>
          )}
          <div className="button-row">
            <Button type="button" variant="outline" onClick={onClose} disabled={busy}>
              Cancel
            </Button>
            <Button type="submit" disabled={busy}>
              <UploadSimple size={17} />
              {busy ? 'Sharing…' : kind === 'document' ? 'Share document' : 'Add research'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
