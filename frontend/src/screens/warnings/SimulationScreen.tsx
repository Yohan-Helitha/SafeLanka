import { TriangleAlert } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiErrorNotice } from '@/components/domain'
import { Button, Card, Checkbox, DataTable, ErrorState, Input, Loading, PageHeader } from '@/components/ui'
import { CHANNEL_LABEL } from '@/constants/labels'
import { paths } from '@/constants/routes'
import { useDocumentTitle, useReferenceData } from '@/hooks/shared'
import {
  useChannelSettings,
  useSensors,
  useSensorTick,
  useSetChannel,
  useSetSensorReading,
} from '@/hooks/warnings/useWarnings'
import type { Sensor } from '@/types'

export function SimulationScreen() {
  useDocumentTitle('Simulation')
  const { districtName, basinName } = useReferenceData()
  const sensors = useSensors()
  const channels = useChannelSettings()
  const tick = useSensorTick()
  const setReading = useSetSensorReading()
  const setChannel = useSetChannel()
  const [values, setValues] = useState<Record<string, string>>({})
  const [newHazard, setNewHazard] = useState<{ id: string; sensor: string } | null>(null)

  const note = (r: { sensor: Sensor; newHazardId: string | null }) => setNewHazard(r.newHazardId ? { id: r.newHazardId, sensor: r.sensor.name } : null)

  return (
    <div>
      <PageHeader title="Simulation" subtitle="River gauges and notification gateways are simulated with demo data." />

      {newHazard && (
        <div role="status" className="mb-4 flex items-start gap-3 rounded-card border border-caution/50 bg-caution/10 p-3 text-sm text-ink">
          <TriangleAlert className="mt-0.5 size-4 shrink-0 text-caution" aria-hidden />
          <p>
            New hazard from a gauge: {newHazard.sensor} passed its alert level.{' '}
            <Link to={paths.dmc.hazard(newHazard.id)} className="font-medium text-signal hover:underline">
              Assess it
            </Link>
            . The simulator never issues a warning.
          </p>
        </div>
      )}

      <div className="space-y-4">
        <Card title="River gauges" padded={false}>
          <div className="p-4 pt-3">
            {sensors.isLoading && <Loading />}
            {sensors.isError && <ErrorState error={sensors.error} onRetry={() => void sensors.refetch()} />}
            {sensors.data && (
              <DataTable<Sensor>
                caption="Simulated river gauges"
                rows={sensors.data}
                rowKey={(s) => s.id}
                minWidth={760}
                columns={[
                  {
                    key: 'gauge',
                    header: 'Gauge',
                    cell: (s) => (
                      <span>
                        <span className="block font-medium">{s.name}</span>
                        <span className="text-sm text-muted">
                          {basinName(s.riverBasinId)} · {districtName(s.districtId)}
                        </span>
                      </span>
                    ),
                  },
                  {
                    key: 'latest',
                    header: 'Latest',
                    cell: (s) => (
                      <span className={`tabular font-medium ${s.latest >= s.alertLevel ? 'text-caution' : ''}`}>
                        {s.latest.toFixed(2)} {s.unit}
                      </span>
                    ),
                  },
                  { key: 'alert', header: 'Alert', cell: (s) => <span className="tabular text-muted">{s.alertLevel.toFixed(2)} m</span> },
                  { key: 'major', header: 'Major flood', cell: (s) => <span className="tabular text-muted">{s.majorFloodLevel.toFixed(2)} m</span> },
                  {
                    key: 'simulate',
                    header: 'Simulate',
                    cell: (s) => (
                      <span className="flex flex-wrap items-center gap-2">
                        <Button size="sm" variant="secondary" loading={tick.isPending && tick.variables === s.id} onClick={() => tick.mutate(s.id, { onSuccess: note })}>
                          Next reading
                        </Button>
                        <Input
                          aria-label={`Set reading for ${s.name}`}
                          type="number"
                          step="0.1"
                          min="0"
                          className="!min-h-8 w-20 py-1"
                          value={values[s.id] ?? ''}
                          onChange={(e) => setValues((v) => ({ ...v, [s.id]: e.target.value }))}
                        />
                        <Button
                          size="sm"
                          variant="ghost"
                          disabled={!values[s.id]}
                          onClick={() => setReading.mutate({ id: s.id, value: Number(values[s.id]) }, { onSuccess: (r) => { note(r); setValues((v) => ({ ...v, [s.id]: '' })) } })}
                        >
                          Set
                        </Button>
                      </span>
                    ),
                  },
                ]}
              />
            )}
            <div className="mt-3">
              <ApiErrorNotice error={tick.error ?? setReading.error} />
            </div>
          </div>
        </Card>

        <Card title="Notification gateways" description="Switch a channel off, or make it fail, to see delivery results change.">
          {channels.isLoading && <Loading />}
          <ul className="divide-y divide-line">
            {(channels.data ?? []).map((c) => (
              <li key={c.channel} className="flex flex-wrap items-center justify-between gap-x-6 py-1">
                <span className="font-medium text-ink">{CHANNEL_LABEL[c.channel]}</span>
                <span className="flex gap-4">
                  <Checkbox label="Enabled" checked={c.enabled} onChange={(e) => setChannel.mutate({ channel: c.channel, patch: { enabled: e.target.checked } })} />
                  <Checkbox label="Simulate failure" checked={c.simulateFailure} onChange={(e) => setChannel.mutate({ channel: c.channel, patch: { simulateFailure: e.target.checked } })} />
                </span>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  )
}
