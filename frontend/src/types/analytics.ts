import type { Id } from './common'
import type { Channel, WarningLevel, WarningStatus } from './warnings'

export interface AnalyticsEvent {
  id: Id
  name: string
  status: 'ACTIVE' | 'CLOSED'
  startedAt: string
  endedAt: string | null
  districtIds: Id[]
  warningCount: number
  linkedReportCount: number
}

export interface ReportFilters {
  districtIds: Id[]
  from: string | null
  to: string | null
}

export interface TimelineEntry {
  warningId: Id
  level: WarningLevel
  status: WarningStatus
  title: string
  issuedAt: string
  districtIds: Id[]
  isEscalation: boolean
}

export interface AlertTimelineSection {
  firstVerifiedReportAt: string | null
  firstWarningAt: string | null
  minutesReportToWarning: number | null
  entries: TimelineEntry[]
}

export interface CitizensReachedSection {
  targeted: number
  reached: number
  deliveryRate: number
  byChannel: Record<Channel, { delivered: number; failed: number }>
  byDistrict: { districtId: Id; targeted: number; reached: number }[]
}

export interface ShelterOccupancySection {
  shelters: { shelterId: Id; name: string; capacity: number }[]
  series: { recordedAt: string; values: Record<Id, number> }[]
  peaks: { shelterId: Id; name: string; peak: number; capacity: number; at: string }[]
}

export interface ResourceDistributionSection {
  byDistrictItem: { label: string; allocated: number; distributed: number }[]
  byOrganisationType: { type: string; distributed: number }[]
}

export type SectionKey =
  | 'alertTimeline'
  | 'citizensReached'
  | 'shelterOccupancy'
  | 'resourceDistribution'

export interface DisasterReport {
  id: Id
  eventId: Id
  eventName: string
  filters: ReportFilters
  generatedBy: string
  generatedAt: string
  alertTimeline: AlertTimelineSection | null
  citizensReached: CitizensReachedSection | null
  shelterOccupancy: ShelterOccupancySection | null
  resourceDistribution: ResourceDistributionSection | null
  unavailableSections: { key: SectionKey; reason: string }[]
}

export interface ReportSummary {
  id: Id
  eventName: string
  generatedAt: string
  generatedBy: string
  unavailableCount: number
}
