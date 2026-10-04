import clsx from 'clsx'
import { useId } from 'react'
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

export interface FieldControlProps {
  id: string
  'aria-invalid'?: true
  'aria-describedby'?: string
}

interface FieldProps {
  label: ReactNode
  hint?: ReactNode
  error?: string | null
  required?: boolean
  /** Right-aligned slot beside the label, for example a CharCount. */
  aside?: ReactNode
  className?: string
  children: (props: FieldControlProps) => ReactNode
}

export function Field({ label, hint, error, required, aside, className, children }: FieldProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined
  return (
    <div className={className}>
      <div className="mb-1.5 flex items-baseline justify-between gap-2">
        <label htmlFor={id} className="text-sm font-medium text-ink">
          {label}
          {required && <span className="ml-0.5 text-danger" aria-hidden> *</span>}
        </label>
        {aside}
      </div>
      {children({ id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })}
      {hint && (
        <p id={hintId} className="mt-1 text-xs text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="mt-1 text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}

const CONTROL =
  'block w-full rounded-control border border-line bg-raised px-3 text-[15px] text-ink placeholder:text-faint ' +
  'focus:border-signal aria-[invalid=true]:border-danger disabled:opacity-60'

export function Input({ className, ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={clsx(CONTROL, 'min-h-[44px]', className)} {...rest} />
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={clsx(CONTROL, 'min-h-[44px]', className)} {...rest}>
      {children}
    </select>
  )
}

export function TextArea({ className, rows = 4, ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea rows={rows} className={clsx(CONTROL, 'py-2.5', className)} {...rest} />
}

export function CharCount({ value, max }: { value: string; max: number }) {
  const n = value.length
  return (
    <span className={clsx('tabular text-xs', n > max ? 'font-medium text-danger' : 'text-faint')} aria-live="off">
      {n}/{max}
    </span>
  )
}
