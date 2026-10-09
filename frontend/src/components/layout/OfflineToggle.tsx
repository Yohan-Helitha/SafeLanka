import clsx from 'clsx'
import { Wifi, WifiOff } from 'lucide-react'
import { useOnlineStatus } from '@/hooks/shared'
import { connectivity } from '@/services/offline/connectivity'

/** Demo switch that forces the app offline so the offline flows can be shown. */
export function OfflineToggle({ className, fullWidth = false }: { className?: string; fullWidth?: boolean }) {
  const online = useOnlineStatus()
  return (
    <button
      type="button"
      role="switch"
      aria-checked={!online}
      aria-label="Work offline"
      onClick={() => connectivity.setForcedOffline(online)}
      className={clsx(
        'inline-flex min-h-9 items-center gap-2 rounded-xl border px-3 text-sm font-medium transition-all active:scale-[0.98]',
        fullWidth && 'w-full justify-center py-2',
        online
          ? 'border-line/70 bg-[#162033]/60 text-slate-300 hover:border-cyan-500/40 hover:bg-[#1a273e] hover:text-white'
          : 'border-amber-500/50 bg-amber-500/10 text-amber-300 hover:bg-amber-500/20 shadow-sm shadow-amber-500/10',
        className,
      )}
    >
      <span className="relative flex h-2.5 w-2.5 shrink-0">
        <span
          className={clsx(
            'absolute inline-flex h-full w-full rounded-full opacity-75',
            online ? 'bg-emerald-400 animate-ping' : 'bg-amber-400',
          )}
        />
        <span
          className={clsx(
            'relative inline-flex h-2.5 w-2.5 rounded-full',
            online ? 'bg-emerald-500' : 'bg-amber-500',
          )}
        />
      </span>
      {online ? <Wifi className="size-4 text-emerald-400" aria-hidden /> : <WifiOff className="size-4 text-amber-400" aria-hidden />}
      <span>{online ? 'Online' : 'Offline'}</span>
    </button>
  )
}
