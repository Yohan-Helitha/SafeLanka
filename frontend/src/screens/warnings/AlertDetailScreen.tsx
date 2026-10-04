import { BellOff, Phone, Volume2 } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { SeverityIcon } from '@/components/domain'
import { Button, Card, EmptyState, ErrorState, Loading, PageHeader } from '@/components/ui'
import { EMERGENCY_NUMBER } from '@/constants/app'
import { paths } from '@/constants/routes'
import { useDocumentTitle } from '@/hooks/shared'
import { useMyAlerts } from '@/hooks/warnings/useWarnings'
import { SEVERITY } from '@/theme/tokens'
import { formatDateTime } from '@/utils/format'
import { playAlertTone } from '@/utils/audio'

export function AlertDetailScreen() {
  const { id } = useParams()
  const alerts = useMyAlerts()
  const alert = alerts.data?.find((a) => a.id === id)
  useDocumentTitle(alert?.title ?? 'Warning')

  if (alerts.isLoading) return <Loading />
  if (alerts.isError) return <ErrorState error={alerts.error} onRetry={() => void alerts.refetch()} />
  if (!alert) {
    return (
      <div>
        <PageHeader backTo={paths.citizen.home} backLabel="Alerts" title="Warning" />
        <EmptyState icon={BellOff} title="This warning is no longer active" description="Check the alerts page for current warnings." />
      </div>
    )
  }

  return (
    <div className="-mx-4 -mt-4">
      <header className="px-4 pb-5 pt-4 text-white" style={{ backgroundColor: SEVERITY[alert.level].hex }}>
        <Link to={paths.citizen.home} className="mb-2 inline-block text-sm opacity-90">
          ← Alerts
        </Link>
        <p className="inline-flex items-center gap-2 rounded-full bg-white/20 px-3 py-1 text-base font-semibold uppercase tracking-wide">
          <SeverityIcon level={alert.level} className="size-5" />
          {SEVERITY[alert.level].label}
        </p>
        <h1 className="mt-2 font-display text-[28px] font-semibold leading-tight">{alert.title}</h1>
        <p className="mt-1 text-sm opacity-90">Issued {formatDateTime(alert.issuedAt)} by the Disaster Management Centre</p>
      </header>
      <div className="space-y-4 px-4 pt-4">
        <Card title="What to do">
          <p className="text-lg font-semibold text-ink">{alert.instructions}</p>
        </Card>
        <Card title="Details">
          <p className="text-[15px] text-ink">{alert.message}</p>
        </Card>
        <div className="grid gap-3">
          <a href={`tel:${EMERGENCY_NUMBER}`}>
            <Button size="lg" block icon={<Phone className="size-5" aria-hidden />}>
              Call {EMERGENCY_NUMBER}
            </Button>
          </a>
          {alert.audible && (
            <Button variant="secondary" size="lg" block icon={<Volume2 className="size-5" aria-hidden />} onClick={() => void playAlertTone()}>
              Play alarm
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
