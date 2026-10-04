import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { ApiErrorNotice, StatusChip } from '@/components/domain'
import { Button, Card, Checkbox, EmptyState, ErrorState, Field, Input, Loading, PageHeader } from '@/components/ui'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAnalyticsEvents, useGenerateReport, useSavedReports } from '@/hooks/analytics/useAnalytics'
import { formatDate, formatDateTime, fromLocalInput, plural } from '@/utils/format'
import { BarChart3 } from 'lucide-react'

interface Props {
  /** Builds the report page path, so DMC and district officers each stay in their own area. */
  reportPath: (id: string) => string
  /** DMC officers generate reports; district officers read the saved ones. */
  canGenerate: boolean
}

export function AnalyticsHomeScreen({ reportPath, canGenerate }: Props) {
  useDocumentTitle('Analysis')
  const navigate = useNavigate()
  const { districtName } = useReferenceData()
  const events = useAnalyticsEvents()
  const saved = useSavedReports()
  const generate = useGenerateReport()

  const [eventId, setEventId] = useState<string | null>(null)
  const [districtIds, setDistrictIds] = useState<string[] | null>(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const event = events.data?.find((e) => e.id === eventId)
  const chosen = districtIds ?? event?.districtIds ?? []
  const rangeError = from && to && from > to ? 'The start must be before the end.' : null

  const run = () => {
    if (!event || rangeError) return
    generate.mutate(
      {
        eventId: event.id,
        filters: { districtIds: chosen, from: from ? fromLocalInput(from) : null, to: to ? fromLocalInput(to) : null },
      },
      { onSuccess: (r) => navigate(reportPath(r.id)) },
    )
  }

  return (
    <div>
      <PageHeader
        title="Disaster response analysis"
        subtitle="See how warnings, shelters and relief performed during an event. Sections without data say so instead of showing zeros."
      />

      {events.isLoading && <Loading />}
      {events.isError && <ErrorState error={events.error} onRetry={() => void events.refetch()} />}
      <div className="grid gap-3 md:grid-cols-2">
        {events.data?.map((e) => {
          const selected = e.id === eventId
          const body = (
            <>
              <span className="flex items-start justify-between gap-2">
                <span className="font-display text-lg font-semibold text-ink">{e.name}</span>
                <StatusChip status={e.status} />
              </span>
              <span className="mt-1 block text-sm text-muted">
                {formatDate(e.startedAt)} {e.endedAt ? `– ${formatDate(e.endedAt)}` : '– ongoing'}
              </span>
              <span className="block text-sm text-muted">{e.districtIds.map(districtName).join(', ')}</span>
              <span className="tabular mt-1 block text-sm text-ink">
                {plural(e.warningCount, 'warning')} · {plural(e.linkedReportCount, 'linked ground report')}
              </span>
            </>
          )
          return canGenerate ? (
            <button
              key={e.id}
              type="button"
              aria-pressed={selected}
              onClick={() => {
                setEventId(e.id)
                setDistrictIds(null)
              }}
              className={`rounded-card border bg-panel p-4 text-left transition-colors hover:border-signal/60 ${selected ? 'border-signal' : 'border-line'}`}
            >
              {body}
            </button>
          ) : (
            <div key={e.id} className="rounded-card border border-line bg-panel p-4">
              {body}
            </div>
          )
        })}
      </div>

      {canGenerate && event && (
        <Card title="Filters" className="mt-4" description="Narrow the report to some districts or a time window. Times are Sri Lanka time.">
          <div className="space-y-4">
            <fieldset>
              <legend className="mb-1 text-sm font-medium text-ink">Districts</legend>
              <div className="flex flex-wrap gap-x-5">
                {event.districtIds.map((d) => (
                  <Checkbox
                    key={d}
                    label={districtName(d)}
                    checked={chosen.includes(d)}
                    onChange={() => setDistrictIds(chosen.includes(d) ? chosen.filter((x) => x !== d) : [...chosen, d])}
                  />
                ))}
              </div>
            </fieldset>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="From">{(p) => <Input {...p} type="datetime-local" value={from} onChange={(e) => setFrom(e.target.value)} />}</Field>
              <Field label="To" error={rangeError}>
                {(p) => <Input {...p} type="datetime-local" value={to} onChange={(e) => setTo(e.target.value)} />}
              </Field>
            </div>
            <ApiErrorNotice error={generate.error} />
            <Button size="lg" loading={generate.isPending} disabled={chosen.length === 0 || Boolean(rangeError)} onClick={run}>
              Generate report
            </Button>
          </div>
        </Card>
      )}

      {!canGenerate && <p className="mt-4 text-sm text-muted">Reports are generated by the Disaster Management Centre. Open a saved report below to read it.</p>}

      <section className="mt-6" aria-labelledby="saved">
        <h2 id="saved" className="mb-2 font-display text-xl font-semibold text-ink">
          Saved reports
        </h2>
        {saved.isLoading && <Loading />}
        {saved.data?.length === 0 && (
          <EmptyState
            icon={BarChart3}
            title="No reports yet"
            description={canGenerate ? 'Pick the closed Kalu Flood May 2026 event above to see a full report.' : 'Reports appear here once the DMC generates them.'}
          />
        )}
        <ul className="space-y-2">
          {saved.data?.map((r) => (
            <li key={r.id}>
              <Link to={reportPath(r.id)} className="flex flex-wrap items-center justify-between gap-2 rounded-card border border-line bg-panel px-4 py-3 hover:border-signal/60">
                <span>
                  <span className="block font-medium text-ink">{r.eventName}</span>
                  <span className="text-sm text-muted">
                    {formatDateTime(r.generatedAt)} · {r.generatedBy}
                  </span>
                </span>
                {r.unavailableCount > 0 && <span className="text-sm text-caution">{plural(r.unavailableCount, 'section')} without data</span>}
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
