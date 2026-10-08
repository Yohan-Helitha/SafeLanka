import { useState } from 'react'
import { LEVEL_ORDER } from '@/constants/labels'
import { useToast } from '@/context/ToastContext'
import { useCancelWarning, useEscalateWarning, useUpdateWarning } from '@/hooks/warnings/useWarnings'
import { SEVERITY } from '@/theme/tokens'
import type { Warning, WarningLevel, WarningUpdate } from '@/types'
import { ApiErrorNotice, SeverityBadge } from '../domain'
import { Button, Dialog, Field, Segmented, TextArea } from '../ui'
import { textErrors, WarningTextFields } from './WarningTextFields'

const textOf = (w: Warning): WarningUpdate => ({
  title: w.title,
  message: w.message,
  smsText: w.smsText,
  instructions: w.instructions,
})

export function WarningActions({ warning }: { warning: Warning }) {
  const [dialog, setDialog] = useState<'escalate' | 'edit' | 'cancel' | null>(null)
  if (warning.status !== 'ACTIVE') return null
  const close = () => setDialog(null)
  const canEscalate = LEVEL_ORDER.indexOf(warning.level) < LEVEL_ORDER.length - 1

  return (
    <>
      <div className="mb-5 flex flex-wrap gap-2">
        {canEscalate && <Button onClick={() => setDialog('escalate')}>Escalate</Button>}
        <Button variant="secondary" onClick={() => setDialog('edit')}>
          Edit text
        </Button>
        <Button variant="ghost" onClick={() => setDialog('cancel')}>
          Cancel warning
        </Button>
      </div>
      {dialog === 'escalate' && <EscalateDialog warning={warning} onClose={close} />}
      {dialog === 'edit' && <EditDialog warning={warning} onClose={close} />}
      {dialog === 'cancel' && <CancelDialog warning={warning} onClose={close} />}
    </>
  )
}

function EscalateDialog({ warning, onClose }: { warning: Warning; onClose: () => void }) {
  const { toast } = useToast()
  const escalate = useEscalateWarning()
  const higher = LEVEL_ORDER.slice(LEVEL_ORDER.indexOf(warning.level) + 1)
  const [level, setLevel] = useState<WarningLevel>(higher[0])
  const [text, setText] = useState(textOf(warning))
  const [attempted, setAttempted] = useState(false)
  const errors = textErrors(text)
  const invalid = Object.values(errors).some(Boolean)

  return (
    <Dialog
      open
      onClose={onClose}
      title="Escalate warning"
      description="This warning is raised to the higher level and sent again to the same area. The change is kept in its level history."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="danger"
            loading={escalate.isPending}
            onClick={() => {
              setAttempted(true)
              if (invalid) return
              escalate.mutate(
                { id: warning.id, input: { ...text, level } },
                {
                  onSuccess: (raised) => {
                    toast(`Escalated to ${SEVERITY[raised.level].label}`)
                    onClose()
                  },
                },
              )
            }}
          >
            Confirm and re-send
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <p className="flex items-center gap-2 text-sm text-muted">
          Current level <SeverityBadge level={warning.level} size="sm" />
        </p>
        <Segmented
          legend="New level"
          columns={1}
          value={level}
          onChange={setLevel}
          options={higher.map((l) => ({ value: l, label: <SeverityBadge level={l} size="sm" />, description: SEVERITY[l].action }))}
        />
        <WarningTextFields value={text} onChange={setText} errors={attempted ? errors : {}} />
        <ApiErrorNotice error={escalate.error} />
      </div>
    </Dialog>
  )
}

function EditDialog({ warning, onClose }: { warning: Warning; onClose: () => void }) {
  const { toast } = useToast()
  const update = useUpdateWarning()
  const [text, setText] = useState(textOf(warning))
  const [attempted, setAttempted] = useState(false)
  const errors = textErrors(text)

  return (
    <Dialog
      open
      onClose={onClose}
      title="Edit text"
      description="Corrections are shown in the app. They are not sent again by SMS."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            loading={update.isPending}
            onClick={() => {
              setAttempted(true)
              if (Object.values(errors).some(Boolean)) return
              update.mutate(
                { id: warning.id, input: text },
                {
                  onSuccess: () => {
                    toast('Text saved')
                    onClose()
                  },
                },
              )
            }}
          >
            Save text
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <WarningTextFields value={text} onChange={setText} errors={attempted ? errors : {}} />
        <ApiErrorNotice error={update.error} />
      </div>
    </Dialog>
  )
}

function CancelDialog({ warning, onClose }: { warning: Warning; onClose: () => void }) {
  const { toast } = useToast()
  const cancel = useCancelWarning()
  const [reason, setReason] = useState('')
  const short = reason.trim().length < 5

  return (
    <Dialog
      open
      onClose={onClose}
      title="Cancel warning"
      description="People are told the warning no longer applies."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Keep warning
          </Button>
          <Button
            variant="danger"
            loading={cancel.isPending}
            disabled={short}
            onClick={() =>
              cancel.mutate(
                { id: warning.id, reason: reason.trim() },
                {
                  onSuccess: () => {
                    toast('Warning cancelled')
                    onClose()
                  },
                },
              )
            }
          >
            Cancel warning
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Reason" required hint="At least 5 characters.">
          {(p) => <TextArea {...p} rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="River level has fallen below the alert level." />}
        </Field>
        <ApiErrorNotice error={cancel.error} />
      </div>
    </Dialog>
  )
}
