import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CHANNEL_LABEL } from '@/constants/labels'
import { CHART } from '@/theme/tokens'
import type { Channel, CitizensReachedSection as Data } from '@/types'
import { formatNumber, formatPercent } from '@/utils/format'
import { Card, DataTable, Stat } from '../ui'

export function CitizensReachedSection({ data, districtName }: { data: Data; districtName: (id: string) => string }) {
  const chart = (Object.keys(data?.byChannel ?? {}) as Channel[])
    .filter((c) => (data.byChannel[c]?.delivered ?? 0) + (data.byChannel[c]?.failed ?? 0) > 0)
    .map((c) => ({ channel: CHANNEL_LABEL[c], Delivered: data.byChannel[c]?.delivered ?? 0, Failed: data.byChannel[c]?.failed ?? 0 }))
  return (
    <Card title="2. Citizens reached">
      <div className="mb-4 grid gap-3 sm:grid-cols-3">
        <Stat label="Targeted" value={formatNumber(data?.targeted ?? 0)} />
        <Stat label="Reached" value={formatNumber(data?.reached ?? 0)} tone="signal" />
        <Stat label="Delivery rate" value={formatPercent(data?.deliveryRate ?? 0)} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div role="img" aria-label="Delivered and failed messages by channel">
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={chart} margin={{ top: 8, right: 8, bottom: 0, left: -12 }}>
              <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="channel" stroke={CHART.axis} tick={{ fontSize: 12 }} />
              <YAxis stroke={CHART.axis} tick={{ fontSize: 12 }} allowDecimals={false} />
              <Tooltip contentStyle={{ background: '#0B1324', border: '1px solid #26324A', borderRadius: 8, color: '#E2E8F0' }} cursor={{ fill: 'rgba(148,163,184,0.08)' }} />
              <Legend />
              <Bar dataKey="Delivered" stackId="a" fill={CHART.signal} isAnimationActive={false} />
              <Bar dataKey="Failed" stackId="a" fill={CHART.danger} isAnimationActive={false} />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <DataTable
          caption="Citizens targeted and reached by district"
          minWidth={0}
          rows={data?.byDistrict ?? []}
          rowKey={(d) => d.districtId}
          columns={[
            { key: 'd', header: 'District', cell: (d) => districtName(d.districtId) },
            { key: 't', header: 'Targeted', align: 'right', cell: (d) => <span className="tabular">{d.targeted}</span> },
            { key: 'r', header: 'Reached', align: 'right', cell: (d) => <span className="tabular">{d.reached}</span> },
          ]}
        />
      </div>
    </Card>
  )
}
