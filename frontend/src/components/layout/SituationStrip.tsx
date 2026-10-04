import { ShieldCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paths } from '@/constants/routes'
import { LEVEL_ORDER } from '@/constants/labels'
import { useAuth } from '@/context/AuthContext'
import { useActiveWarnings } from '@/hooks/warnings/useWarnings'
import { SEVERITY } from '@/theme/tokens'
import { formatTime } from '@/utils/format'
import { SeverityIcon } from '../domain'

/** The one bold element in the portal: the highest active warning, always in view. */
export function SituationStrip() {
  const { user } = useAuth()
  const { data } = useActiveWarnings()

  const active = (data ?? []).filter((w) => user?.role !== 'DISTRICT_OFFICER' || w.districtIds.includes(user.districtId))
  if (!active.length) {
    return (
      <div className="flex items-center gap-2 border-b border-line bg-panel px-4 py-2.5 text-sm text-muted">
        <ShieldCheck className="size-4 text-ok" aria-hidden />
        No active warnings
      </div>
    )
  }

  const top = [...active].sort((a, b) => LEVEL_ORDER.indexOf(b.level) - LEVEL_ORDER.indexOf(a.level))[0]
  const latest = [...active].sort((a, b) => b.issuedAt.localeCompare(a.issuedAt))[0]
  const body = (
    <>
      <SeverityIcon level={top.level} className="size-5 shrink-0" />
      <span className="font-display text-lg font-semibold uppercase tracking-wide">{SEVERITY[top.level].label}</span>
      <span className="min-w-0 flex-1 truncate font-medium">{top.title}</span>
      <span className="tabular hidden shrink-0 text-sm opacity-90 sm:inline">
        {active.length} active · issued {formatTime(latest.issuedAt)}
      </span>
    </>
  )
  const cls = 'flex items-center gap-3 px-4 py-2.5 text-white'
  const style = { backgroundColor: SEVERITY[top.level].hex }

  return user?.role === 'DMC_OFFICER' ? (
    <Link to={paths.dmc.warnings} className={cls} style={style} aria-label={`Highest active warning: ${SEVERITY[top.level].label}. Open warnings`}>
      {body}
    </Link>
  ) : (
    <div className={cls} style={style} role="status">
      {body}
    </div>
  )
}
