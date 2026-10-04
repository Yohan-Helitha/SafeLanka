import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CHART } from '@/theme/tokens'
import type { Sensor } from '@/types'
import { formatTime } from '@/utils/format'

export function GaugeChart({ sensor }: { sensor: Sensor }) {
  const data = sensor.readings.map((r) => ({ time: formatTime(r.recordedAt), value: r.value }))
  const top = Math.max(sensor.majorFloodLevel, ...sensor.readings.map((r) => r.value)) * 1.05
  return (
    <div role="img" aria-label={`Water level at ${sensor.name}, last 24 hours. Latest ${sensor.latest} ${sensor.unit}.`}>
      <ResponsiveContainer width="100%" height={240}>
        <LineChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
          <CartesianGrid stroke={CHART.grid} strokeDasharray="3 3" />
          <XAxis dataKey="time" stroke={CHART.axis} tick={{ fontSize: 11 }} interval="preserveStartEnd" minTickGap={32} />
          <YAxis stroke={CHART.axis} tick={{ fontSize: 11 }} domain={[0, +top.toFixed(1)]} unit={` ${sensor.unit}`} width={56} />
          <Tooltip
            contentStyle={{ background: '#0B1324', border: '1px solid #26324A', borderRadius: 8, color: '#E2E8F0' }}
            formatter={(v) => [`${v} ${sensor.unit}`, 'Level']}
          />
          <ReferenceLine y={sensor.alertLevel} stroke={CHART.alert} strokeDasharray="5 4" label={{ value: 'Alert', fill: CHART.alert, fontSize: 11, position: 'insideTopLeft' }} />
          <ReferenceLine y={sensor.majorFloodLevel} stroke={CHART.danger} strokeDasharray="5 4" label={{ value: 'Major flood', fill: CHART.danger, fontSize: 11, position: 'insideTopLeft' }} />
          <Line type="monotone" dataKey="value" stroke={CHART.signal} strokeWidth={2.5} dot={false} isAnimationActive={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
