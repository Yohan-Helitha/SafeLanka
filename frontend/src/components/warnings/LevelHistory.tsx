import type { LevelChange, WarningListItem } from '@/types'
import { formatDateTime } from '@/utils/format'
import { SeverityBadge } from '../domain'
import { Button, Dialog } from '../ui'

/** The levels a warning has had, oldest first, each with the time it changed: Advisory -> Warning -> Evacuate. */
export function LevelHistory({ history }: { history: LevelChange[] }) {
  return (
    <ol className="space-y-3">
      {history.map((step) => (
        <li key={step.changedAt} className="flex flex-wrap items-center gap-2 text-sm">
          {step.from && (
            <>
              <SeverityBadge level={step.from} size="sm" />
              <span aria-label="changed to" className="text-faint">
                →
              </span>
            </>
          )}
          <SeverityBadge level={step.to} size="sm" />
          <span className="text-muted">{step.from ? 'raised' : 'issued'} {formatDateTime(step.changedAt)}</span>
        </li>
      ))}
    </ol>
  )
}

/** A short chain of the levels, for a table cell or a heading. */
export function LevelPath({ history }: { history: LevelChange[] }) {
  return (
    <span className="flex flex-wrap items-center gap-1">
      {history.map((step, i) => (
        <span key={step.changedAt} className="flex items-center gap-1">
          {i > 0 && (
            <span aria-hidden className="text-faint">
              →
            </span>
          )}
          <SeverityBadge level={step.to} size="sm" />
        </span>
      ))}
    </span>
  )
}

export function LevelHistoryDialog({ warning, onClose }: { warning: WarningListItem; onClose: () => void }) {
  return (
    <Dialog
      open
      onClose={onClose}
      title={warning.title}
      description="How the level of this warning has changed."
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <LevelHistory history={warning.levelHistory} />
    </Dialog>
  )
}
