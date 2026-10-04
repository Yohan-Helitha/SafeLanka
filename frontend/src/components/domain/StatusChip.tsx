import clsx from 'clsx'
import { STATUS_TONE } from '@/theme/tokens'
import type { Tone } from '@/theme/tokens'
import { humanise } from '@/utils/format'

const TONE: Record<Tone, string> = {
  neutral: 'border-line text-muted',
  live: 'border-signal/60 text-signal',
  good: 'border-ok/60 text-ok',
  caution: 'border-caution/60 text-caution',
  bad: 'border-danger/60 text-danger',
}

interface Props {
  status: string
  label?: string
  tone?: Tone
}

export function StatusChip({ status, label, tone }: Props) {
  const resolved = tone ?? STATUS_TONE[status] ?? 'neutral'
  return (
    <span className={clsx('inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border px-2.5 py-0.5 text-xs font-medium', TONE[resolved])}>
      <span className="size-1.5 rounded-full bg-current" aria-hidden />
      {label ?? humanise(status)}
    </span>
  )
}
