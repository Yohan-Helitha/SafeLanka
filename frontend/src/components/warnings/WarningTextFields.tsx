import { LIMITS } from '@/constants/app'
import type { WarningUpdate } from '@/types'
import { CharCount, Field, Input, TextArea } from '../ui'

interface Props {
  value: WarningUpdate
  onChange: (next: WarningUpdate) => void
  errors?: Partial<Record<keyof WarningUpdate, string | null>>
}

/** The four editable texts of a warning, shared by the new-warning, escalate and edit forms. */
export function WarningTextFields({ value, onChange, errors = {} }: Props) {
  const set = (key: keyof WarningUpdate) => (v: string) => onChange({ ...value, [key]: v })
  return (
    <div className="space-y-4">
      <Field label="Title" required error={errors.title} aside={<CharCount value={value.title} max={LIMITS.warningTitle.max} />}>
        {(p) => <Input {...p} value={value.title} onChange={(e) => set('title')(e.target.value)} placeholder="Kelani river flood warning" />}
      </Field>
      <Field label="Message for the app" required error={errors.message} aside={<CharCount value={value.message} max={LIMITS.warningMessage.max} />}>
        {(p) => <TextArea {...p} rows={4} value={value.message} onChange={(e) => set('message')(e.target.value)} />}
      </Field>
      <Field
        label="SMS text"
        required
        error={errors.smsText}
        hint="One SMS is 160 characters. Keep the key action first."
        aside={<CharCount value={value.smsText} max={LIMITS.sms.max} />}
      >
        {(p) => <TextArea {...p} rows={2} value={value.smsText} onChange={(e) => set('smsText')(e.target.value)} />}
      </Field>
      <Field label="What people should do" required error={errors.instructions} aside={<CharCount value={value.instructions} max={LIMITS.instructions.max} />}>
        {(p) => <TextArea {...p} rows={2} value={value.instructions} onChange={(e) => set('instructions')(e.target.value)} />}
      </Field>
    </div>
  )
}

export function textErrors(v: WarningUpdate): Partial<Record<keyof WarningUpdate, string | null>> {
  const len = (s: string, min: number, max: number, label: string) =>
    s.trim().length < min || s.trim().length > max ? `${label} must be ${min}–${max} characters.` : null
  return {
    title: len(v.title, LIMITS.warningTitle.min, LIMITS.warningTitle.max, 'Title'),
    message: len(v.message, LIMITS.warningMessage.min, LIMITS.warningMessage.max, 'Message'),
    smsText: len(v.smsText, LIMITS.sms.min, LIMITS.sms.max, 'SMS text'),
    instructions: len(v.instructions, LIMITS.instructions.min, LIMITS.instructions.max, 'Instructions'),
  }
}
