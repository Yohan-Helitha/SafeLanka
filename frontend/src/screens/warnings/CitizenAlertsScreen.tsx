import { ChevronRight, Plus, ShieldCheck } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { SeverityBadge } from '@/components/domain'
import { ErrorState, Loading } from '@/components/ui'
import { AlertHero } from '@/components/warnings/AlertHero'
import { paths } from '@/constants/routes'
import { useCurrentUser } from '@/context/ActingUserContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useMyAlerts } from '@/hooks/warnings/useWarnings'
import { playAlertTone } from '@/utils/audio'

export function CitizenAlertsScreen() {
  useDocumentTitle('Alerts')
  const user = useCurrentUser()
  const { districtName, basinName } = useReferenceData()
  const alerts = useMyAlerts()
  const played = useRef<string | null>(null)

  const top = alerts.data?.[0]
  const area = [districtName(user.districtId), user.riverBasinId ? `${basinName(user.riverBasinId)} basin` : null]
    .filter(Boolean)
    .join(' · ')

  // Sound the siren once for a new audible alert; browsers may block it, which is fine.
  useEffect(() => {
    if (top?.audible && played.current !== top.id) {
      played.current = top.id
      void playAlertTone()
    }
  }, [top])

  if (alerts.isLoading) return <Loading label="Checking for warnings" />
  if (alerts.isError) return <ErrorState error={alerts.error} onRetry={() => void alerts.refetch()} />

  return (
    <div className="space-y-5">
      {top ? (
        <AlertHero alert={top} area={area} />
      ) : (
        <div className="rounded-card border border-line bg-panel p-5 text-center">
          <ShieldCheck className="mx-auto size-12 text-ok" aria-hidden />
          <h2 className="mt-2 font-display text-2xl font-semibold text-ink">No warnings for your area</h2>
          <p className="mt-1 text-muted">{area}</p>
          <p className="mt-3 text-sm text-muted">We'll alert you by app notification and SMS if that changes.</p>
        </div>
      )}

      {alerts.data && alerts.data.length > 1 && (
        <section aria-labelledby="other-alerts">
          <h2 id="other-alerts" className="mb-2 text-sm font-medium text-muted">
            Other active warnings
          </h2>
          <ul className="divide-y divide-line rounded-card border border-line bg-panel">
            {alerts.data.slice(1).map((a) => (
              <li key={a.id}>
                <Link to={paths.citizen.alert(a.id)} className="flex min-h-[56px] items-center gap-3 px-4 py-2">
                  <SeverityBadge level={a.level} size="sm" />
                  <span className="flex-1 font-medium text-ink">{a.title}</span>
                  <ChevronRight className="size-5 text-faint" aria-hidden />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <Link
        to={paths.citizen.report}
        className="flex min-h-[72px] items-center gap-4 rounded-card border border-line bg-panel px-4 py-3 transition-colors hover:border-signal/60"
      >
        <span className="grid size-12 shrink-0 place-items-center rounded-full bg-action text-white">
          <Plus className="size-6" aria-hidden />
        </span>
        <span>
          <span className="block text-lg font-semibold text-ink">Report a hazard</span>
          <span className="block text-sm text-muted">Rising water, a blocked road or a landslide crack</span>
        </span>
      </Link>
    </div>
  )
}
