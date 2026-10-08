import { Link, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { ApiErrorNotice, StatusChip } from '@/components/domain'
import { Button, Card, Checkbox, ErrorState, Field, Input, Loading } from '@/components/ui'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import { useAnalyticsEvents, useGenerateReport, useSavedReports } from '@/hooks/analytics/useAnalytics'
import { formatDate, formatDateTime, fromLocalInput, toLocalInput, plural } from '@/utils/format'
import {
  BarChart3,
  Calendar,
  ChevronRight,
  FileText,
  History,
  Info,
  LineChart,
  MapPin,
} from 'lucide-react'

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
    <div className="flex flex-col w-full gap-6">
      {/* Split Master-Detail Operational Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start w-full">
        {/* Left Column: Event Explorer (5/12) */}
        <div className="lg:col-span-5 flex flex-col gap-5">
          {/* Section Header */}
          <div className="flex flex-col gap-1.5">
            
            <h1 className="font-display text-2xl font-semibold leading-tight text-ink sm:text-[28px]">
              Disaster response analysis
            </h1>
            <p className="text-sm text-muted">
              See how warnings, shelters and relief performed during an event. Sections without data say so instead of showing zeros.
            </p>
          </div>

          {events.isLoading && <Loading />}
          {events.isError && <ErrorState error={events.error} onRetry={() => void events.refetch()} />}

          {/* Incident Cards List */}
          <div className="flex flex-col gap-3">
            {events.data?.map((e) => {
              const selected = e.id === eventId
              const isActive = e.status === 'ACTIVE'

              const cardContent = (
                <div
                  className={`group relative overflow-hidden rounded-xl border p-4 text-left transition-all duration-200 ${
                    canGenerate ? 'cursor-pointer' : ''
                  } ${
                    selected
                      ? 'border-signal bg-cyan-950/40 ring-1 ring-signal/50 shadow-md'
                      : isActive
                      ? 'border-cyan-500/40 bg-[#0c1e2e] hover:bg-[#0f2538] hover:border-cyan-400/60 shadow-sm'
                      : 'border-slate-800 bg-[#0d121c]/90 opacity-80 hover:opacity-100 hover:bg-[#121824] hover:border-slate-700'
                  }`}
                >
                  {/* Subtle decorative glow for selected or active */}
                  {(selected || isActive) && (
                    <div className="absolute -right-12 -top-12 h-28 w-28 rounded-full bg-cyan-500/10 blur-2xl pointer-events-none" />
                  )}

                  <div className="flex flex-col gap-3">
                    <div className="flex items-start justify-between gap-2">
                      <h2
                        className={`font-display text-base font-semibold transition-colors ${
                          selected ? 'text-signal' : 'text-ink group-hover:text-signal'
                        }`}
                      >
                        {e.name}
                      </h2>
                      <StatusChip status={e.status} />
                    </div>

                    <div className="flex flex-col gap-1.5 text-xs">
                      <div className="flex items-center gap-2 text-muted">
                        <Calendar className="h-3.5 w-3.5 shrink-0 text-signal/80" />
                        <span className="text-ink/90">
                          {formatDate(e.startedAt)} {e.endedAt ? `– ${formatDate(e.endedAt)}` : '– ongoing'}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-muted">
                        <MapPin className="h-3.5 w-3.5 shrink-0 text-signal/80" />
                        <span className="text-ink/90">
                          {e.districtIds.map(districtName).join(', ')}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-3 pt-2.5 border-t border-line/60 -mx-4 -mb-4 px-4 pb-3 flex items-center justify-between bg-canvas/40">
                    <div className="flex items-center gap-1.5 text-xs text-muted">
                      {isActive ? (
                        <LineChart className="h-3.5 w-3.5 text-signal" />
                      ) : (
                        <History className="h-3.5 w-3.5 text-muted" />
                      )}
                      <span>
                        {plural(e.warningCount, 'warning')} · {plural(e.linkedReportCount, 'linked ground report')}
                      </span>
                    </div>
                    {canGenerate && (
                      <ChevronRight
                        className={`h-4 w-4 text-muted transition-transform group-hover:translate-x-0.5 ${
                          selected ? 'text-signal translate-x-0.5' : 'group-hover:text-signal'
                        }`}
                      />
                    )}
                  </div>
                </div>
              )

              return canGenerate ? (
                <button
                  key={e.id}
                  type="button"
                  aria-pressed={selected}
                  onClick={() => {
                    setEventId(e.id)
                    setDistrictIds(null)
                    setFrom(e.startedAt ? toLocalInput(e.startedAt) : '')
                    setTo(e.endedAt ? toLocalInput(e.endedAt) : '')
                  }}
                  className="w-full text-left"
                >
                  {cardContent}
                </button>
              ) : (
                <div key={e.id}>{cardContent}</div>
              )
            })}
          </div>
        </div>

        {/* Right Column: Inspection Workspace & Saved Reports (7/12) */}
        <div className="lg:col-span-7 flex flex-col gap-5">
          {/* Information Banner */}
          <div className="flex items-center gap-3 px-4 py-3 rounded-lg bg-panel border border-line/70 shadow-sm">
            <div className="w-8 h-8 rounded-lg bg-signal/15 text-signal flex items-center justify-center shrink-0">
              <Info className="h-4 w-4" />
            </div>
            <p className="text-xs text-ink/90 leading-relaxed">
              Reports are generated by the Disaster Management Centre. Open a saved report below to inspect the findings and analytics.
            </p>
          </div>

          {/* DMC Generator Filters (when canGenerate is true and an event is selected) */}
          {canGenerate && event && (
            <Card
              title={`Generate report: ${event.name}`}
              description="Narrow the report to specific districts or a time window. Times are Sri Lanka time."
            >
              <div className="space-y-4">
                <fieldset>
                  <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted">
                    Filter Districts
                  </legend>
                  <div className="flex flex-wrap gap-x-5 gap-y-2">
                    {event.districtIds.map((d) => (
                      <Checkbox
                        key={d}
                        label={districtName(d)}
                        checked={chosen.includes(d)}
                        onChange={() =>
                          setDistrictIds(
                            chosen.includes(d) ? chosen.filter((x) => x !== d) : [...chosen, d],
                          )
                        }
                      />
                    ))}
                  </div>
                </fieldset>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="From">
                    {(p) => (
                      <Input
                        {...p}
                        type="datetime-local"
                        value={from}
                        onChange={(e) => setFrom(e.target.value)}
                      />
                    )}
                  </Field>
                  <Field label="To" error={rangeError}>
                    {(p) => (
                      <Input
                        {...p}
                        type="datetime-local"
                        value={to}
                        onChange={(e) => setTo(e.target.value)}
                      />
                    )}
                  </Field>
                </div>
                <ApiErrorNotice error={generate.error} />
                <Button
                  size="lg"
                  loading={generate.isPending}
                  disabled={chosen.length === 0 || Boolean(rangeError)}
                  onClick={run}
                >
                  Generate report
                </Button>
              </div>
            </Card>
          )}

          {/* Saved Reports Section */}
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg font-semibold text-ink">Saved reports</h2>
              {saved.data && saved.data.length > 0 && (
                <span className="text-xs text-muted">{plural(saved.data.length, 'report')}</span>
              )}
            </div>

            {saved.isLoading && <Loading />}

            {/* Empty State Panel */}
            {saved.data?.length === 0 && (
              <div className="relative overflow-hidden w-full rounded-xl border border-line bg-panel py-16 px-4 flex flex-col items-center justify-center text-center shadow-sm min-h-[340px]">
                <div className="w-16 h-16 rounded-full bg-raised flex items-center justify-center mb-4 border border-line/60">
                  <BarChart3 className="w-8 h-8 text-signal/80" />
                </div>
                <h3 className="font-display text-base font-semibold text-ink mb-1">
                  No reports yet
                </h3>
                <p className="text-xs text-muted max-w-sm">
                  {canGenerate
                    ? 'Pick an event on the left and click "Generate report" to create one.'
                    : 'Reports appear here once the DMC generates them.'}
                </p>
              </div>
            )}

            {/* List of Saved Reports */}
            {saved.data && saved.data.length > 0 && (
              <ul className="flex flex-col gap-2">
                {saved.data.map((r) => (
                  <li key={r.id}>
                    <Link
                      to={reportPath(r.id)}
                      className="group flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-panel px-4 py-3.5 transition-all hover:bg-raised hover:border-signal/60"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-raised text-signal flex items-center justify-center shrink-0 border border-line/60 group-hover:border-signal/40">
                          <FileText className="h-4 w-4" />
                        </div>
                        <div>
                          <span className="block font-medium text-ink group-hover:text-signal transition-colors text-sm">
                            {r.eventName}
                          </span>
                          <span className="text-xs text-muted">
                            {formatDateTime(r.generatedAt)} · {r.generatedBy}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        {r.unavailableCount > 0 && (
                          <span className="text-xs text-caution">
                            {plural(r.unavailableCount, 'section')} without data
                          </span>
                        )}
                        <ChevronRight className="h-4 w-4 text-muted group-hover:text-signal group-hover:translate-x-0.5 transition-transform" />
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
