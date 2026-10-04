import clsx from 'clsx'
import type { InputHTMLAttributes, ReactNode } from 'react'

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: ReactNode
  description?: ReactNode
}

export function Checkbox({ label, description, className, ...rest }: CheckboxProps) {
  return (
    <label className={clsx('flex min-h-[44px] cursor-pointer items-start gap-3 py-2', className)}>
      <input type="checkbox" className="mt-0.5 size-5 shrink-0 accent-[rgb(var(--signal))]" {...rest} />
      <span>
        <span className="text-[15px] text-ink">{label}</span>
        {description && <span className="block text-sm text-muted">{description}</span>}
      </span>
    </label>
  )
}
