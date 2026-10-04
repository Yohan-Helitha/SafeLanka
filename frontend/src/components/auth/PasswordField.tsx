import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import type { ReactNode } from 'react'
import { Field, Input } from '../ui'

interface Props {
  label?: string
  value: string
  onChange: (value: string) => void
  error?: string | null
  hint?: ReactNode
  /** "current-password" at login, "new-password" at sign-up, so password managers behave. */
  autoComplete: 'current-password' | 'new-password'
}

/** Password input with a show/hide toggle. */
export function PasswordField({ label = 'Password', value, onChange, error, hint, autoComplete }: Props) {
  const [visible, setVisible] = useState(false)
  return (
    <Field label={label} required error={error} hint={hint}>
      {(p) => (
        <div className="relative">
          <Input
            {...p}
            type={visible ? 'text' : 'password'}
            autoComplete={autoComplete}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="pr-12"
          />
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Hide password' : 'Show password'}
            aria-pressed={visible}
            className="absolute inset-y-0 right-0 grid w-11 place-items-center text-muted hover:text-ink"
          >
            {visible ? <EyeOff className="size-5" aria-hidden /> : <Eye className="size-5" aria-hidden />}
          </button>
        </div>
      )}
    </Field>
  )
}
