import type {
  Channel,
  ChannelSetting,
  CitizenAlert,
  Delivery,
  DeliverySummary,
  HazardDetail,
  HazardListItem,
  HazardSource,
  HazardStatus,
  Page,
  Sensor,
  TargetType,
  Warning,
  WarningLevel,
  WarningListItem,
  WarningStatus,
} from '@/types'

/**
 * The shapes the Spring Boot warnings API sends, and the functions that turn them into the types
 * the screens already use. Keeping the translation here means the screens and the mock service
 * stay exactly as they are.
 */

const CHANNELS: Channel[] = ['PUSH', 'SMS', 'AUDIBLE']

export interface WirePage<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface WireLatestReading {
  sensorName: string
  value: number
  unit: string
  aboveAlert: boolean
  recordedAt: string
}

export interface WireHazardListItem {
  id: string
  hazardTypeId: string
  hazardTypeCode: string | null
  severity: number
  districtId: string | null
  riverBasinId: string | null
  description: string
  source: HazardSource
  status: HazardStatus
  detectedAt: string
  verifiedReportCount: number
  latestReading: WireLatestReading | null
}

export interface WireGauge {
  id: string
  code: string
  name: string
  riverBasinId: string
  districtId: string
  alertLevel: number
  majorFloodLevel: number
  unit: string
  readings: { value: number; recordedAt: string }[]
}

export interface WireEvidence {
  reportId: string
  referenceNo: string
  category: string
  description: string
  districtId: string
  capturedAt: string
}

export interface WireWarningRef {
  id: string
  hazardId: string
  level: WarningLevel
  status: WarningStatus
  title: string
  districtIds: string[]
  riverBasinIds: string[]
  issuedAt: string
  levelChangedAt: string
  levelHistory: WireLevelChange[]
  reached: number
}

export interface WireLevelChange {
  fromLevel: WarningLevel | null
  toLevel: WarningLevel
  changedBy: string
  changedAt: string
}

export interface WireHazardDetail extends WireHazardListItem {
  evidence: WireEvidence[]
  sensor: WireGauge | null
  warnings: WireWarningRef[]
}

export interface WireDeliverySummary {
  targeted: number
  delivered: number
  failed: number
  byChannel: { channel: Channel; delivered: number; failed: number }[]
}

export interface WireWarning {
  id: string
  hazardId: string
  eventId: string | null
  level: WarningLevel
  status: WarningStatus
  targetType: TargetType
  districtIds: string[]
  riverBasinIds: string[]
  title: string
  message: string
  smsText: string
  instructions: string
  issuedAt: string
  issuedBy: string
  levelChangedAt: string
  levelHistory: WireLevelChange[]
  cancelledAt: string | null
  cancelReason: string | null
  evidenceReportIds: string[]
  deliverySummary: WireDeliverySummary
}

export interface WireDelivery {
  citizenId: string
  districtId: string | null
  channel: Channel
  status: 'QUEUED' | 'DELIVERED' | 'FAILED'
  attemptedAt: string
  failureReason: string | null
}

export interface WireAlert {
  warningId: string
  level: WarningLevel
  title: string
  message: string
  instructions: string
  issuedAt: string
  districtId: string
  riverBasinId: string | null
  audible: boolean
}

export interface WireAudience {
  citizenCount: number
  resolvedDistrictIds: string[]
  channels: ChannelSetting[]
}

export interface WireSensor {
  id: string
  code: string
  name: string
  riverBasinId: string
  districtId: string
  alertLevel: number
  majorFloodLevel: number
  unit: string
  latest: { value: number; recordedAt: string } | null
}

export interface WireTick {
  reading: { value: number; recordedAt: string }
  thresholdCrossed: boolean
  hazardId: string | null
}

export function toHazardListItem(w: WireHazardListItem): HazardListItem {
  return {
    id: w.id,
    hazardTypeId: w.hazardTypeId,
    severity: w.severity,
    districtId: w.districtId,
    riverBasinId: w.riverBasinId,
    description: w.description,
    source: w.source,
    status: w.status,
    detectedAt: w.detectedAt,
    verifiedReportCount: w.verifiedReportCount,
    latestReading: w.latestReading && {
      sensorName: w.latestReading.sensorName,
      value: w.latestReading.value,
      unit: w.latestReading.unit,
      aboveAlert: w.latestReading.aboveAlert,
    },
  }
}

export function toHazardDetail(w: WireHazardDetail): HazardDetail {
  return {
    ...toHazardListItem(w),
    sensor: w.sensor && toChartSensor(w.sensor),
    evidence: w.evidence.map((e) => ({
      reportId: e.reportId,
      referenceNo: e.referenceNo,
      description: e.description,
      capturedAt: e.capturedAt,
      verifiedAt: null,
    })),
    warnings: w.warnings.map(toWarningListItem),
  }
}

/** A gauge with its readings, newest reading last. The latest value is the last reading. */
function toChartSensor(g: WireGauge): Sensor {
  const last = g.readings[g.readings.length - 1]
  return {
    id: g.id,
    code: g.code,
    name: g.name,
    riverBasinId: g.riverBasinId,
    districtId: g.districtId,
    alertLevel: g.alertLevel,
    majorFloodLevel: g.majorFloodLevel,
    unit: g.unit,
    latest: last?.value ?? 0,
    readings: g.readings.map((r) => ({ recordedAt: r.recordedAt, value: r.value })),
  }
}

/** A gauge from the simulation list, which carries only its newest reading. */
export function toSimulationSensor(s: WireSensor): Sensor {
  return {
    id: s.id,
    code: s.code,
    name: s.name,
    riverBasinId: s.riverBasinId,
    districtId: s.districtId,
    alertLevel: s.alertLevel,
    majorFloodLevel: s.majorFloodLevel,
    unit: s.unit,
    latest: s.latest?.value ?? 0,
    readings: s.latest ? [{ recordedAt: s.latest.recordedAt, value: s.latest.value }] : [],
  }
}

export function toWarningListItem(w: WireWarningRef | WireWarning): WarningListItem {
  return {
    id: w.id,
    hazardId: w.hazardId,
    level: w.level,
    status: w.status,
    title: w.title,
    districtIds: w.districtIds,
    riverBasinIds: w.riverBasinIds,
    issuedAt: w.issuedAt,
    levelChangedAt: w.levelChangedAt,
    levelHistory: w.levelHistory.map((c) => ({
      from: c.fromLevel,
      to: c.toLevel,
      changedBy: c.changedBy,
      changedAt: c.changedAt,
    })),
    reached: 'reached' in w ? w.reached : w.deliverySummary.targeted,
  }
}

export function toWarning(w: WireWarning): Warning {
  return {
    ...toWarningListItem(w),
    eventId: w.eventId,
    targetType: w.targetType,
    message: w.message,
    smsText: w.smsText,
    instructions: w.instructions,
    issuedBy: w.issuedBy,
    cancelledAt: w.cancelledAt,
    cancelReason: w.cancelReason,
    reportIds: w.evidenceReportIds,
    deliverySummary: toDeliverySummary(w.deliverySummary),
  }
}

/** The API lists the channels that were used; the screens expect every channel, with zeros. */
export function toDeliverySummary(s: WireDeliverySummary): DeliverySummary {
  const byChannel = Object.fromEntries(
    CHANNELS.map((c) => [c, { delivered: 0, failed: 0 }]),
  ) as DeliverySummary['byChannel']
  s.byChannel.forEach((c) => {
    byChannel[c.channel] = { delivered: c.delivered, failed: c.failed }
  })
  return { targeted: s.targeted, delivered: s.delivered, failed: s.failed, byChannel }
}

/** A delivery has no id of its own: one person and one channel are unique within a warning. */
export function toDelivery(d: WireDelivery): Delivery {
  return {
    id: `${d.citizenId}:${d.channel}`,
    districtId: d.districtId ?? '',
    channel: d.channel,
    status: d.status,
    attemptedAt: d.attemptedAt,
    failureReason: d.failureReason,
  }
}

export function toPage<S, T>(page: WirePage<S>, map: (item: S) => T): Page<T> {
  return {
    items: page.content.map(map),
    page: page.page,
    size: page.size,
    totalElements: page.totalElements,
    totalPages: page.totalPages,
  }
}

export function toCitizenAlert(a: WireAlert): CitizenAlert {
  return {
    id: a.warningId,
    level: a.level,
    title: a.title,
    message: a.message,
    instructions: a.instructions,
    issuedAt: a.issuedAt,
    districtId: a.districtId,
    riverBasinId: a.riverBasinId,
    audible: a.audible,
  }
}
