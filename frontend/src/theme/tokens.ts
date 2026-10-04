import { BellRing, Eye, Info, Siren, TriangleAlert } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { WarningLevel } from '@/types'

/** Severity colours are reserved for hazard and warning meaning only. Always pair colour + icon + word. */
export const SEVERITY: Record<
  WarningLevel,
  { label: string; hex: string; action: string; icon: LucideIcon; audible: boolean }
> = {
  ADVISORY: { label: 'Advisory', hex: '#2563EB', action: 'Stay informed', icon: Info, audible: false },
  WATCH: { label: 'Watch', hex: '#CA8A04', action: 'Be ready to act', icon: Eye, audible: false },
  WARNING: {
    label: 'Warning',
    hex: '#EA580C',
    action: 'Prepare to move',
    icon: TriangleAlert,
    audible: true,
  },
  EVACUATE: { label: 'Evacuate', hex: '#DC2626', action: 'Leave now', icon: Siren, audible: true },
}

export const ALERT_ICON = BellRing

export type Tone = 'neutral' | 'live' | 'good' | 'caution' | 'bad'

const caution = [
  'PENDING',
  'PENDING_SYNC',
  'NEEDS_MORE_INFO',
  'UNDER_ASSESSMENT',
  'DISPATCHED',
  'PENDING_ACK',
  'PARTIALLY_DISTRIBUTED',
  'AMBER',
]
const good = ['VERIFIED', 'DELIVERED', 'AVAILABLE', 'COMPLETED', 'OPEN', 'DISTRIBUTED', 'OK']
const bad = ['REJECTED', 'FAILED', 'WARNED', 'FULL', 'OFFLINE_UNKNOWN', 'NEEDS_ATTENTION']
const live = ['ACTIVE', 'MONITORING', 'EN_ROUTE', 'ACCEPTED', 'ALLOCATED']

export const STATUS_TONE: Record<string, Tone> = Object.fromEntries([
  ...caution.map((s) => [s, 'caution']),
  ...good.map((s) => [s, 'good']),
  ...bad.map((s) => [s, 'bad']),
  ...live.map((s) => [s, 'live']),
])

export const THRESHOLDS = {
  /** Occupancy ratio at which a shelter turns amber. */
  shelterAmber: 0.9,
  duplicateMetres: 500,
  duplicateHours: 2,
} as const

export const CHART = {
  grid: '#26324A',
  axis: '#64748B',
  signal: '#22D3EE',
  danger: '#F87171',
  alert: '#EA580C',
  muted: '#475569',
  series: ['#22D3EE', '#A78BFA', '#34D399', '#FBBF24', '#F472B6', '#60A5FA'],
} as const
