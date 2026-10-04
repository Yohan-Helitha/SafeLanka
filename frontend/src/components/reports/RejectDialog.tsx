import { useState } from 'react'
import { REJECTION_LABEL } from '@/constants/labels'
import { LIMITS } from '@/constants/app'
import type { RejectionReason } from '@/types'
import { ApiErrorNotice } from '../domain'
import { Button, CharCount, Dialog, Field, Segmented, TextArea } from '../ui'

interface Props {
  open: boolean
  onClose: () => void
  onConfirm: (reason: RejectionReason, comment: string) => void
  loading: boolean
  error: unknown
}

const REASONS = Object.keys(REJECTION_LABEL) as RejectionReason[]

export function RejectDialog({ open, onClose, onConfirm, loading, error }: Props) {
  const [reason, setReason] = useState<RejectionReason>('INSUFFICIENT_EVIDENCE')
  const [comment, setComment] = useState('')
  const [touched, setTouched] = useState(false)

  const commentError =
    reason === 'OTHER' && comment.trim().length < LIMITS.comment.min ? 'Explain why you are rejecting this report.' : null

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Reject report"
      description="The reporter sees the reason you pick and any comment."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="danger"
            loading={loading}
            onClick={() => {
              setTouched(true)
              if (!commentError) onConfirm(reason, comment)
            }}
          >
            Reject report
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Segmented
          legend="Reason"
          columns={1}
          value={reason}
          onChange={setReason}
          options={REASONS.map((r) => ({ value: r, label: REJECTION_LABEL[r] }))}
        />
        <Field
          label="Comment"
          required={reason === 'OTHER'}
          error={touched ? commentError : null}
          aside={<CharCount value={comment} max={LIMITS.comment.max} />}
        >
          {(p) => <TextArea {...p} rows={3} maxLength={LIMITS.comment.max} value={comment} onChange={(e) => setComment(e.target.value)} />}
        </Field>
        <ApiErrorNotice error={error} />
      </div>
    </Dialog>
  )
}
