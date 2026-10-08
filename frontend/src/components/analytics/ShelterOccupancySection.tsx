import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CHART } from '@/theme/tokens'
import type { ShelterOccupancySection as Data } from '@/types'
import { formatDateTime, formatShortDay } from '@/utils/format'
import { Card, DataTable } from '../ui'

export function ShelterOccupancySection({ data }: { data: Data }) {
  const rows = (data?.series ?? []).map((p) => ({ at: p.recordedAt, ...p.values }))
  return (
    <Card title="3. Shelter occupancy over time">
      <div role="img" aria-label="Occupancy of each shelter over time">
        <ResponsiveContainer width="100%" height={260}>
          <LineChart data={rows} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
            <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" />
            <XAxis dataKey="at" stroke={CHART.axis} tick={{ fontSize: 11 }} tickFormatter={formatShortDay} minTickGap={32} />
            <YAxis stroke={CHART.axis} tick={{ fontSize: 11 }} allowDecimals={false} />
            <Tooltip
              contentStyle={{ background: '#0B1324', border: '1px solid #26324A', borderRadius: 8, color: '#E2E8F0' }}
              labelFormatter={(v) => formatDateTime(String(v))}
            />
            <Legend />
            {(data?.shelters ?? []).map((s, i) => (
              <Line key={s.shelterId} type="stepAfter" name={s.name} dataKey={s.shelterId} stroke={CHART.series[i % CHART.series.length]} strokeWidth={2.5} dot={false} isAnimationActive={false} />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div className="mt-4">
        <DataTable
          caption="Peak occupancy by shelter"
          minWidth={0}
          rows={data?.peaks ?? []}
          rowKey={(p) => p.shelterId}
          columns={[
            { key: 's', header: 'Shelter', cell: (p) => p.name },
            { key: 'p', header: 'Peak', align: 'right', cell: (p) => <span className="tabular">{p.peak}/{p.capacity}</span> },
            {
              key: 'c',
              header: 'Of capacity',
              align: 'right',
              cell: (p) => <span className={`tabular ${p.peak / p.capacity >= 0.9 ? 'font-medium text-caution' : ''}`}>{Math.round((p.peak / p.capacity) * 100)}%</span>,
            },
            { key: 'w', header: 'When', cell: (p) => <span className="text-muted">{formatDateTime(p.at)}</span> },
          ]}
        />
      </div>
    </Card>
  )
}
