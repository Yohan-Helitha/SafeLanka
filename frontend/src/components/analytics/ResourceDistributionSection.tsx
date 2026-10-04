import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { ORGANISATION_TYPE_LABEL } from '@/constants/labels'
import { CHART } from '@/theme/tokens'
import type { OrganisationType, ResourceDistributionSection as Data } from '@/types'
import { Card, Stat } from '../ui'

export function ResourceDistributionSection({ data }: { data: Data }) {
  return (
    <Card title="4. Resource distribution">
      <div role="img" aria-label="Allocated and distributed relief by district and item">
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data.byDistrictItem} margin={{ top: 8, right: 8, bottom: 0, left: -8 }}>
            <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" stroke={CHART.axis} tick={{ fontSize: 11 }} interval={0} />
            <YAxis stroke={CHART.axis} tick={{ fontSize: 11 }} allowDecimals={false} />
            <Tooltip contentStyle={{ background: '#0B1324', border: '1px solid #26324A', borderRadius: 8, color: '#E2E8F0' }} cursor={{ fill: 'rgba(148,163,184,0.08)' }} />
            <Legend />
            <Bar dataKey="allocated" name="Allocated" fill={CHART.muted} isAnimationActive={false} />
            <Bar dataKey="distributed" name="Distributed" fill={CHART.signal} isAnimationActive={false} />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {data.byOrganisationType.map((o) => (
          <Stat
            key={o.type}
            label={ORGANISATION_TYPE_LABEL[o.type as OrganisationType] ?? o.type}
            value={o.distributed}
            hint="units handed out"
          />
        ))}
      </div>
    </Card>
  )
}
