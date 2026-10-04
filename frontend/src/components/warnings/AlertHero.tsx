import { Volume2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { paths } from '@/constants/routes'
import { SEVERITY } from '@/theme/tokens'
import type { CitizenAlert } from '@/types'
import { relativeTime } from '@/utils/format'
import { playAlertTone } from '@/utils/audio'
import { SeverityIcon } from '../domain'
import { Button } from '../ui'

interface Props {
  alert: CitizenAlert
  area: string
}

/** Top alert for a citizen: severity-coloured border and header, instructions first. */
export function AlertHero({ alert, area }: Props) {
  const colour = SEVERITY[alert.level].hex
  return (
    <article className="overflow-hidden rounded-card bg-panel" style={{ border: `2px solid ${colour}` }}>
      <header className="px-4 py-3 text-white" style={{ backgroundColor: colour }}>
        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-base font-semibold uppercase tracking-wide">
            <SeverityIcon level={alert.level} className="size-5" />
            {SEVERITY[alert.level].label}
          </span>
          <span className="text-sm opacity-90">{relativeTime(alert.issuedAt)}</span>
        </div>
        <h2 className="mt-2 font-display text-2xl font-semibold leading-tight">{alert.title}</h2>
        <p className="text-sm opacity-90">{area}</p>
      </header>
      <div className="space-y-3 p-4">
        <p className="text-base font-semibold text-ink">{alert.instructions}</p>
        <p className="line-clamp-3 text-[15px] text-muted">{alert.message}</p>
        <div className="flex gap-2">
          <Link to={paths.citizen.alert(alert.id)} className="flex-1">
            <Button size="lg" block>
              Read full warning
            </Button>
          </Link>
          {alert.audible && (
            <Button
              variant="secondary"
              size="lg"
              aria-label="Play alert sound"
              onClick={() => void playAlertTone()}
              icon={<Volume2 className="size-5" aria-hidden />}
            />
          )}
        </div>
      </div>
    </article>
  )
}
