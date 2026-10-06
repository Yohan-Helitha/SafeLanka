import clsx from 'clsx'
import type { ReactNode } from 'react'

interface StatProps {
  label: string
  value: ReactNode
  tone?: 'default' | 'signal' | 'caution' | 'danger'
  hint?: ReactNode
}

const TONE = {
  default: 'text-ink',
  signal: 'text-signal',
  caution: 'text-caution',
  danger: 'text-danger',
}

export function Stat({ label, value, tone = 'default', hint }: StatProps) {
  return (
    <div className="rounded-card border border-line bg-panel p-3 sm:px-4 sm:py-3">
      <p className="text-xs sm:text-sm text-muted">{label}</p>
      <p className={clsx('tabular font-display text-2xl sm:text-[32px] font-semibold leading-tight my-0.5', TONE[tone])}>{value}</p>
      {hint && <p className="text-[11px] sm:text-xs text-faint">{hint}</p>}
    </div>
  )
}
