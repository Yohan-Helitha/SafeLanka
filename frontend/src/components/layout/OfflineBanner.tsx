import clsx from 'clsx'
import { CloudOff } from 'lucide-react'
import { useOutbox } from '@/context/OutboxContext'
import { useOnlineStatus } from '@/hooks/shared'
import { plural } from '@/utils/format'
import { Button } from '../ui'

export function OfflineBanner() {
  const online = useOnlineStatus()
  const { pendingCount, attentionCount, flush, flushing } = useOutbox()
  const waiting = pendingCount + attentionCount
  if (online && waiting === 0) return null

  const bad = attentionCount > 0
  return (
    <div
      role="status"
      className={clsx(
        'flex flex-wrap items-center gap-x-3 gap-y-1 border-b px-4 py-2 text-sm',
        bad ? 'border-danger/40 bg-danger/10 text-danger' : 'border-caution/40 bg-caution/10 text-caution',
      )}
    >
      <CloudOff className="size-4 shrink-0" aria-hidden />
      <span className="flex-1">
        {!online && "You're offline. "}
        {waiting > 0 ? `${plural(waiting, 'saved item')} waiting to send.` : 'Changes are saved on this phone.'}
        {bad && ` ${plural(attentionCount, 'item')} could not be sent.`}
      </span>
      {online && waiting > 0 && (
        <Button size="sm" variant="secondary" onClick={() => void flush()} loading={flushing}>
          Send now
        </Button>
      )}
    </div>
  )
}
