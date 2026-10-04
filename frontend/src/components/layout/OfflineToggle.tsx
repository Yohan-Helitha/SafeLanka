import clsx from 'clsx'
import { Wifi, WifiOff } from 'lucide-react'
import { useOnlineStatus } from '@/hooks/shared'
import { connectivity } from '@/services/offline/connectivity'

/** Demo switch that forces the app offline so the offline flows can be shown. */
export function OfflineToggle({ className }: { className?: string }) {
  const online = useOnlineStatus()
  return (
    <button
      type="button"
      role="switch"
      aria-checked={!online}
      aria-label="Work offline"
      onClick={() => connectivity.setForcedOffline(online)}
      className={clsx(
        'inline-flex min-h-9 items-center gap-1.5 rounded-full border px-3 text-sm font-medium transition-colors',
        online ? 'border-line text-muted hover:text-ink' : 'border-caution/60 bg-caution/10 text-caution',
        className,
      )}
    >
      {online ? <Wifi className="size-4" aria-hidden /> : <WifiOff className="size-4" aria-hidden />}
      {online ? 'Online' : 'Offline'}
    </button>
  )
}
