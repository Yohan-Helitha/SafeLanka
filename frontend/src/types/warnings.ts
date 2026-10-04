import type { Id } from './common'

export type WarningLevel = 'ADVISORY' | 'WATCH' | 'WARNING' | 'EVACUATE'
export type WarningStatus = 'ACTIVE' | 'ESCALATED' | 'CANCELLED' | 'EXPIRED'
export type HazardStatus = 'UNDER_ASSESSMENT' | 'WARNED' | 'MONITORING' | 'RESOLVED'
export type HazardSource = 'MANUAL' | 'SENSOR' | 'REPORT'
export type Channel = 'PUSH' | 'SMS' | 'AUDIBLE'
export type TargetType = 'DISTRICT' | 'RIVER_BASIN'

export interface HazardListItem {
  id: Id
  hazardTypeId: Id
  severity: number
  districtId: Id | null
  riverBasinId: Id | null
  description: string
  source: HazardSource
  status: HazardStatus
  detectedAt: string
  verifiedReportCount: number
  latestReading: { sensorName: string; value: number; unit: string; aboveAlert: boolean } | null
}

export interface SensorReading {
  recordedAt: string
  value: number
}

export interface Sensor {
  id: Id
  code: string
  name: string
  riverBasinId: Id
  districtId: Id
  alertLevel: number
  majorFloodLevel: number
  unit: string
  latest: number
  readings: SensorReading[]
}

export interface HazardEvidence {
  reportId: Id
  referenceNo: string
  description: string
  capturedAt: string
  verifiedAt: string | null
}

export interface HazardDetail extends HazardListItem {
  sensor: Sensor | null
  evidence: HazardEvidence[]
  warnings: WarningListItem[]
}

export interface HazardInput {
  hazardTypeId: Id
  severity: number
  districtId: Id | null
  riverBasinId: Id | null
  description: string
}

export interface DeliverySummary {
  targeted: number
  delivered: number
  failed: number
  byChannel: Record<Channel, { delivered: number; failed: number }>
}

export interface WarningListItem {
  id: Id
  hazardId: Id
  level: WarningLevel
  status: WarningStatus
  title: string
  districtIds: Id[]
  riverBasinIds: Id[]
  issuedAt: string
  reached: number
}

export interface Warning extends WarningListItem {
  eventId: Id | null
  targetType: TargetType
  message: string
  smsText: string
  instructions: string
  issuedBy: Id
  supersedesId: Id | null
  cancelledAt: string | null
  cancelReason: string | null
  reportIds: Id[]
  deliverySummary: DeliverySummary
}

export interface WarningInput {
  hazardId: Id
  level: WarningLevel
  targetType: TargetType
  districtIds: Id[]
  riverBasinIds: Id[]
  title: string
  message: string
  smsText: string
  instructions: string
  reportIds: Id[]
  confirm: boolean
}

export interface WarningUpdate {
  title: string
  message: string
  smsText: string
  instructions: string
}

export interface EscalateInput extends WarningUpdate {
  level: WarningLevel
}

export interface Delivery {
  id: Id
  districtId: Id
  channel: Channel
  status: 'QUEUED' | 'DELIVERED' | 'FAILED'
  attemptedAt: string
  failureReason: string | null
}

export interface DeliveryFilter {
  status?: 'DELIVERED' | 'FAILED' | 'ALL'
  channel?: Channel | 'ALL'
  page?: number
  size?: number
}

export interface CitizenAlert {
  id: Id
  level: WarningLevel
  title: string
  message: string
  instructions: string
  issuedAt: string
  districtId: Id
  riverBasinId: Id | null
  audible: boolean
}

export interface ChannelSetting {
  channel: Channel
  enabled: boolean
  simulateFailure: boolean
}

export interface Audience {
  recipients: number
}
