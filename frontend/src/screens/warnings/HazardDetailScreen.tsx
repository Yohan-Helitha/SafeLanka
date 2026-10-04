import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiErrorNotice, SeverityBadge, SeverityMeter, StatusChip } from '@/components/domain'
import { Button, Card, EmptyState, ErrorState, Loading, PageHeader } from '@/components/ui'
import { GaugeChart } from '@/components/warnings/GaugeChart'
import { HAZARD_SOURCE_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useToast } from '@/context/ToastContext'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useHazard, useSetHazardStatus } from '@/hooks/warnings/useWarnings'
import { formatDateTime, relativeTime } from '@/utils/format'

export function HazardDetailScreen() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { hazardTypeName, areaName } = useReferenceData()
  const hazard = useHazard(id)
  const setStatus = useSetHazardStatus()
  useDocumentTitle('Hazard')

  if (hazard.isLoading) return <Loading />
  if (hazard.isError || !hazard.data) return <ErrorState error={hazard.error} onRetry={() => void hazard.refetch()} />
  const h = hazard.data
  const resolved = h.status === 'RESOLVED'
  const area = areaName(h.districtId, h.riverBasinId)

  return (
    <div>
      <PageHeader
        backTo={paths.dmc.hazards}
        backLabel="Hazards"
        title={`${hazardTypeName(h.hazardTypeId)} · ${area}`}
        subtitle={h.description}
        actions={
          !resolved && (
            <>
              {h.status !== 'MONITORING' && (
                <Button
                  variant="secondary"
                  loading={setStatus.isPending}
                  onClick={() => setStatus.mutate({ id: h.id, status: 'MONITORING' }, { onSuccess: () => toast('Hazard is being monitored') })}
                >
                  Keep monitoring
                </Button>
              )}
              <Button variant="ghost" onClick={() => setStatus.mutate({ id: h.id, status: 'RESOLVED' }, { onSuccess: () => toast('Hazard resolved') })}>
                Resolve
              </Button>
              <Button onClick={() => navigate(`${paths.dmc.newWarning}?hazardId=${h.id}`)}>Issue warning</Button>
            </>
          )
        }
      />
      <ApiErrorNotice error={setStatus.error} />

      <dl className="mb-5 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
        <div className="flex items-center gap-2">
          <dt className="sr-only">Status</dt>
          <dd>
            <StatusChip status={h.status} />
          </dd>
        </div>
        <div className="flex items-center gap-2">
          <dt className="text-faint">Severity</dt>
          <dd>
            <SeverityMeter value={h.severity} />
          </dd>
        </div>
        <div className="flex gap-2">
          <dt className="text-faint">Source</dt>
          <dd className="text-ink">{HAZARD_SOURCE_LABEL[h.source]}</dd>
        </div>
        <div className="flex gap-2">
          <dt className="text-faint">Detected</dt>
          <dd className="text-ink">{formatDateTime(h.detectedAt)}</dd>
        </div>
      </dl>

      <div className="grid gap-4 lg:grid-cols-[1fr_340px]">
        <div className="space-y-4">
          {h.sensor && (
            <Card
              title={h.sensor.name}
              description={`Simulated gauge ${h.sensor.code} · alert ${h.sensor.alertLevel} ${h.sensor.unit} · major flood ${h.sensor.majorFloodLevel} ${h.sensor.unit}`}
            >
              <GaugeChart sensor={h.sensor} />
            </Card>
          )}
          <Card title="Verified ground reports">
            {h.evidence.length === 0 ? (
              <EmptyState title="No verified reports yet" description="Reports appear here once an officer verifies them." />
            ) : (
              <ul className="divide-y divide-line">
                {h.evidence.map((e) => (
                  <li key={e.reportId} className="py-3 first:pt-0 last:pb-0">
                    <Link to={paths.dmc.report(e.reportId)} className="tabular font-medium text-signal hover:underline">
                      {e.referenceNo}
                    </Link>
                    <p className="text-[15px] text-ink">{e.description}</p>
                    <p className="text-sm text-muted">
                      Seen {relativeTime(e.capturedAt)} · verified {relativeTime(e.verifiedAt)}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>

        <Card title="Warnings for this hazard">
          {h.warnings.length === 0 ? (
            <p className="text-sm text-muted">No warning has been issued for this hazard.</p>
          ) : (
            <ul className="space-y-3">
              {h.warnings.map((w) => (
                <li key={w.id}>
                  <Link to={paths.dmc.warning(w.id)} className="flex items-center justify-between gap-2 rounded-control border border-line bg-raised p-3 hover:border-signal/60">
                    <span className="min-w-0">
                      <SeverityBadge level={w.level} size="sm" />
                      <span className="mt-1 block truncate text-sm text-ink">{w.title}</span>
                      <span className="text-xs text-muted">{relativeTime(w.issuedAt)}</span>
                    </span>
                    <StatusChip status={w.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  )
}
