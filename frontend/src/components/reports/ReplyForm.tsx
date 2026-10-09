import { useState } from 'react'
import { ApiErrorNotice } from '@/components/domain'
import { PhotoPicker } from '@/components/reports/PhotoPicker'
import { Button, CharCount, Field, TextArea } from '@/components/ui'
import { LIMITS } from '@/constants/app'
import { useToast } from '@/context/ToastContext'
import { useReplyToReport } from '@/hooks/reports/useReports'
import { lengthError } from '@/utils/validation'

/** The citizen's answer to the officer's question; sending it puts the report back in the queue. */
export function ReplyForm({ reportId }: { reportId: string }) {
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [photo, setPhoto] = useState<File | null>(null)
  const [attempted, setAttempted] = useState(false)
  const reply = useReplyToReport()
  const { toast } = useToast()
  const error = lengthError('Reply', message, LIMITS.comment.min, LIMITS.comment.max)
  const photoError = photo && photo.size > LIMITS.photoBytes ? 'This photo is larger than 5 MB.' : null

  if (!open) {
    return (
      <div className="mt-3">
        <Button size="sm" onClick={() => setOpen(true)}>
          Reply
        </Button>
      </div>
    )
  }

  const send = () => {
    setAttempted(true)
    if (error || photoError) return
    reply.mutate(
      { id: reportId, message: message.trim(), photo },
      { onSuccess: () => toast('Your answer was sent to the officer') },
    )
  }

  return (
    <div className="mt-3 space-y-3">
      <Field label="Your answer" required error={attempted ? error : null} aside={<CharCount value={message} max={LIMITS.comment.max} />}>
        {(p) => (
          <TextArea
            {...p}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Answer what the officer asked."
          />
        )}
      </Field>
      <div>
        <p className="mb-1.5 text-sm font-medium text-ink">New photo (optional)</p>
        <PhotoPicker file={photo} onChange={setPhoto} error={attempted ? photoError : null} />
        {photo && <p className="mt-1 text-sm text-muted">It replaces the photo you sent before.</p>}
      </div>
      <ApiErrorNotice error={reply.error} />
      <div className="flex gap-2">
        <Button size="sm" loading={reply.isPending} onClick={send}>
          Send answer
        </Button>
        <Button variant="ghost" size="sm" disabled={reply.isPending} onClick={() => setOpen(false)}>
          Cancel
        </Button>
      </div>
    </div>
  )
}
