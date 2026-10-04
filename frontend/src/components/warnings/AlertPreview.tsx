import { SEVERITY } from '@/theme/tokens'
import type { WarningLevel } from '@/types'
import { SeverityIcon } from '../domain'

interface Props {
  level: WarningLevel
  title: string
  instructions: string
  smsText: string
}

/** What recipients will see: the app alert (always light) and the SMS bubble. */
export function AlertPreview({ level, title, instructions, smsText }: Props) {
  const colour = SEVERITY[level].hex
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div data-surface="field" className="rounded-card bg-canvas p-3">
        <p className="mb-2 text-xs font-medium uppercase tracking-wide text-faint">In the app</p>
        <div className="overflow-hidden rounded-card bg-panel" style={{ border: `2px solid ${colour}` }}>
          <div className="flex items-center gap-2 px-3 py-2 text-sm font-semibold uppercase tracking-wide text-white" style={{ backgroundColor: colour }}>
            <SeverityIcon level={level} />
            {SEVERITY[level].label}
          </div>
          <div className="space-y-1 p-3">
            <p className="font-display text-lg font-semibold text-ink">{title || 'Warning title'}</p>
            <p className="text-sm font-medium text-ink">{instructions || 'What people should do'}</p>
          </div>
        </div>
      </div>
      <div className="rounded-card bg-raised p-3">
        <p className="mb-2 text-xs font-medium uppercase tracking-wide text-faint">
          As SMS (<span className="tabular">{smsText.length}/160</span>)
        </p>
        <div className="rounded-2xl rounded-bl-sm bg-slate-200 px-3 py-2 text-sm text-slate-900">{smsText || 'SMS text'}</div>
      </div>
    </div>
  )
}
