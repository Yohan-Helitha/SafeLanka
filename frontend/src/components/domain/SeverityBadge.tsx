import clsx from 'clsx'
import { SEVERITY } from '@/theme/tokens'
import type { WarningLevel } from '@/types'

interface Props {
  level: WarningLevel
  size?: 'sm' | 'md' | 'lg'
}

export function SeverityIcon({ level, className }: { level: WarningLevel; className?: string }) {
  const Icon = SEVERITY[level].icon
  return <Icon className={className ?? 'size-4'} aria-hidden />
}

const SIZE = {
  sm: 'gap-1 px-2 py-0.5 text-xs [&_svg]:size-3.5',
  md: 'gap-1.5 px-2.5 py-1 text-sm [&_svg]:size-4',
  lg: 'gap-2 px-3.5 py-1.5 text-base [&_svg]:size-5',
}

/** Colour + icon + word, so severity never relies on colour alone. */
export function SeverityBadge({ level, size = 'md' }: Props) {
  return (
    <span
      className={clsx('inline-flex items-center rounded-full font-semibold uppercase tracking-wide text-white', SIZE[size])}
      style={{ backgroundColor: SEVERITY[level].hex }}
    >
      <SeverityIcon level={level} />
      {SEVERITY[level].label}
    </span>
  )
}
