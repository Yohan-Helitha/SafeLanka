import { LIMITS } from '@/constants/app'
import { LEVEL_ORDER } from '@/constants/labels'
import { SEVERITY } from '@/theme/tokens'
import type {
  CitizenAlert,
  Delivery,
  HazardDetail,
  HazardListItem,
  Sensor,
  Warning,
  WarningInput,
  WarningListItem,
  WarningUpdate,
} from '@/types'
import { requireRole, actor } from '../mocks/actor'
import { bus } from '../mocks/bus'
import { db, districtsOfBasins, makeDeliveries, recipientsFor, summariseDeliveries } from '../mocks/db'
import type { HazardRow } from '../mocks/db'
import { newId } from '../mocks/ids'
import { fail, mockCall, notFound, paginate, validationFail } from '../mocks/mockCall'
import type { SensorTick, WarningsApi } from './warningsApi'

// ---------- mapping ----------

function toListItem(row: HazardRow): HazardListItem {
  const sensor = row.sensorId ? db.sensors.find((s) => s.id === row.sensorId) : undefined
  return {
    id: row.id,
    hazardTypeId: row.hazardTypeId,
    severity: row.severity,
    districtId: row.districtId,
    riverBasinId: row.riverBasinId,
    description: row.description,
    source: row.source,
    status: row.status,
    detectedAt: row.detectedAt,
    verifiedReportCount: row.evidence.length,
    latestReading: sensor
      ? { sensorName: sensor.name, value: sensor.latest, unit: sensor.unit, aboveAlert: sensor.latest >= sensor.alertLevel }
      : null,
  }
}

function warningItem(w: Warning): WarningListItem {
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
    levelHistory: w.levelHistory,
    reached: w.reached,
  }
}

function hazardRow(id: string): HazardRow {
  return db.hazards.find((h) => h.id === id) ?? notFound('Hazard')
}

function warningRow(id: string): Warning {
  return db.warnings.find((w) => w.id === id) ?? notFound('Warning')
}

function toDetail(row: HazardRow): HazardDetail {
  return {
    ...toListItem(row),
    sensor: row.sensorId ? (db.sensors.find((s) => s.id === row.sensorId) ?? null) : null,
    evidence: row.evidence.map((e) => {
      const r = db.reports.find((x) => x.id === e.reportId)!
      return {
        reportId: r.id,
        referenceNo: r.referenceNo,
        description: r.description,
        capturedAt: r.capturedAt,
        verifiedAt: r.reviewedAt,
      }
    }),
    warnings: db.warnings
      .filter((w) => w.hazardId === row.id)
      .sort((a, b) => b.issuedAt.localeCompare(a.issuedAt))
      .map(warningItem),
  }
}

// ---------- validation ----------

function validateText(input: WarningUpdate) {
  const errors: Record<string, string> = {}
  const check = (key: keyof WarningUpdate, label: string, min: number, max: number) => {
    const n = input[key].trim().length
    if (n < min || n > max) errors[key] = `${label} must be ${min}–${max} characters.`
  }
  check('title', 'Title', LIMITS.warningTitle.min, LIMITS.warningTitle.max)
  check('message', 'Message', LIMITS.warningMessage.min, LIMITS.warningMessage.max)
  check('smsText', 'SMS text', LIMITS.sms.min, LIMITS.sms.max)
  check('instructions', 'Instructions', LIMITS.instructions.min, LIMITS.instructions.max)
  if (Object.keys(errors).length) validationFail(errors)
}

function expand(districtIds: string[], basinIds: string[]): string[] {
  return [...new Set([...districtIds, ...districtsOfBasins(basinIds)])]
}

function broadcast(w: Warning) {
  const recipients = recipientsFor(w.targetType === 'DISTRICT' ? w.districtIds : [], w.riverBasinIds)
  const rows = makeDeliveries(w, recipients, { seeded: false })
  db.deliveries.push(...rows)
  w.deliverySummary = summariseDeliveries(rows)
  w.reached = w.deliverySummary.targeted
}

// ---------- hazards from reports and sensors ----------

bus.subscribe((event) => {
  if (event.type !== 'ReportVerified') return
  const report = db.reports.find((r) => r.id === event.reportId)
  if (!report) return
  const basin = db.riverBasins.find((b) => b.districtIds.includes(report.districtId))
  const open = db.hazards.find(
    (h) =>
      h.status !== 'RESOLVED' &&
      h.hazardTypeId === report.hazardTypeId &&
      (h.districtId === report.districtId || (basin && h.riverBasinId === basin.id)),
  )
  const linkedAt = new Date().toISOString()
  if (open) {
    if (!open.evidence.some((e) => e.reportId === report.id)) open.evidence.push({ reportId: report.id, linkedAt })
    return
  }
  db.hazards.push({
    id: newId(),
    eventId: null,
    hazardTypeId: report.hazardTypeId,
    severity: 2,
    districtId: report.districtId,
    riverBasinId: null,
    description: report.description,
    source: 'REPORT',
    sensorId: null,
    status: 'UNDER_ASSESSMENT',
    detectedAt: linkedAt,
    evidence: [{ reportId: report.id, linkedAt }],
  })
})

function pushReading(sensor: Sensor, value: number): SensorTick {
  const previous = sensor.latest
  sensor.latest = +value.toFixed(2)
  sensor.readings.push({ recordedAt: new Date().toISOString(), value: sensor.latest })
  if (sensor.readings.length > 48) sensor.readings.shift()

  let newHazardId: string | null = null
  const crossed = previous < sensor.alertLevel && sensor.latest >= sensor.alertLevel
  const hasOpen = db.hazards.some((h) => h.sensorId === sensor.id && h.status !== 'RESOLVED')
  if (crossed && !hasOpen) {
    newHazardId = newId()
    db.hazards.push({
      id: newHazardId,
      eventId: db.events.find((e) => e.status === 'ACTIVE' && e.districtIds.includes(sensor.districtId))?.id ?? null,
      hazardTypeId: db.hazardTypes[0].id,
      severity: 3,
      districtId: sensor.districtId,
      riverBasinId: sensor.riverBasinId,
      description: `${sensor.name} passed its alert level of ${sensor.alertLevel} ${sensor.unit}.`,
      source: 'SENSOR',
      sensorId: sensor.id,
      status: 'UNDER_ASSESSMENT',
      detectedAt: new Date().toISOString(),
      evidence: [],
    })
  }
  return { sensor, newHazardId }
}

function sensorRow(id: string): Sensor {
  return db.sensors.find((s) => s.id === id) ?? notFound('Gauge')
}

// ---------- API ----------

export const warningsMock: WarningsApi = {
  hazards: (filter) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return db.hazards
        .filter((h) => filter?.includeResolved || h.status !== 'RESOLVED')
        .sort((a, b) => b.severity - a.severity || b.detectedAt.localeCompare(a.detectedAt))
        .map(toListItem)
    }),

  hazard: (id) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return toDetail(hazardRow(id))
    }),

  createHazard: (input) =>
    mockCall(() => {
      requireRole('DMC_OFFICER')
      const errors: Record<string, string> = {}
      if (input.description.trim().length < LIMITS.description.min || input.description.trim().length > LIMITS.description.max) {
        errors.description = `Description must be ${LIMITS.description.min}–${LIMITS.description.max} characters.`
      }
      if (input.severity < 1 || input.severity > 5) errors.severity = 'Severity is 1 to 5.'
      if (!input.districtId && !input.riverBasinId) errors.area = 'Choose a district or river basin.'
      if (Object.keys(errors).length) validationFail(errors)
      const row: HazardRow = {
        id: newId(),
        eventId: db.events.find((e) => e.status === 'ACTIVE')?.id ?? null,
        hazardTypeId: input.hazardTypeId,
        severity: input.severity,
        districtId: input.districtId,
        riverBasinId: input.riverBasinId,
        description: input.description.trim(),
        source: 'MANUAL',
        sensorId: null,
        status: 'UNDER_ASSESSMENT',
        detectedAt: new Date().toISOString(),
        evidence: [],
      }
      db.hazards.push(row)
      return toDetail(row)
    }),

  setHazardStatus: (id, status) =>
    mockCall(() => {
      requireRole('DMC_OFFICER')
      const row = hazardRow(id)
      row.status = status
      return toDetail(row)
    }),

  setHazardSeverity: (id, severity) =>
    mockCall(() => {
      requireRole('DMC_OFFICER')
      const row = hazardRow(id)
      if (row.status === 'RESOLVED') fail('INVALID_STATE_TRANSITION', 'A resolved hazard cannot be changed.')
      row.severity = severity
      return toDetail(row)
    }),

  audience: (districtIds, basinIds) =>
    mockCall(() => ({ recipients: recipientsFor(districtIds, basinIds).length })),

  publish: (input: WarningInput) =>
    mockCall(() => {
      const user = requireRole('DMC_OFFICER')
      if (!input.confirm) fail('BUSINESS_RULE', 'Confirm before broadcasting.')
      validateText(input)
      const targetDistricts = expand(input.districtIds, input.riverBasinIds)
      if (!targetDistricts.length) validationFail({ area: 'Choose at least one area to warn.' })
      const hazard = hazardRow(input.hazardId)
      if (hazard.status === 'RESOLVED') fail('INVALID_STATE_TRANSITION', 'This hazard is resolved.')

      const overlapping = db.warnings.find(
        (w) =>
          w.status === 'ACTIVE' &&
          db.hazards.find((h) => h.id === w.hazardId)?.hazardTypeId === hazard.hazardTypeId &&
          w.districtIds.some((d) => targetDistricts.includes(d)),
      )
      if (overlapping) {
        fail('CONFLICT', 'An active warning already covers this area. Escalate or update it instead.', {
          warningId: overlapping.id,
        })
      }

      const issuedAt = new Date().toISOString()
      const warning: Warning = {
        id: newId(),
        hazardId: hazard.id,
        eventId: hazard.eventId,
        level: input.level,
        status: 'ACTIVE',
        targetType: input.targetType,
        districtIds: targetDistricts,
        riverBasinIds: input.targetType === 'RIVER_BASIN' ? input.riverBasinIds : [],
        title: input.title.trim(),
        message: input.message.trim(),
        smsText: input.smsText.trim(),
        instructions: input.instructions.trim(),
        issuedBy: user.id,
        issuedAt,
        levelChangedAt: issuedAt,
        levelHistory: [{ from: null, to: input.level, changedBy: user.id, changedAt: issuedAt }],
        cancelledAt: null,
        cancelReason: null,
        reportIds: input.reportIds,
        reached: 0,
        deliverySummary: summariseDeliveries([]),
      }
      broadcast(warning)
      db.warnings.push(warning)
      hazard.status = 'WARNED'
      bus.emit({ type: 'WarningPublished', warningId: warning.id })
      return warning
    }),

  update: (id, input) =>
    mockCall(() => {
      requireRole('DMC_OFFICER')
      const w = warningRow(id)
      if (w.status !== 'ACTIVE') fail('INVALID_STATE_TRANSITION', 'Only an active warning can be edited.')
      validateText(input)
      Object.assign(w, {
        title: input.title.trim(),
        message: input.message.trim(),
        smsText: input.smsText.trim(),
        instructions: input.instructions.trim(),
      })
      return w
    }),

  escalate: (id, input) =>
    mockCall(() => {
      const user = requireRole('DMC_OFFICER')
      const warning = warningRow(id)
      if (warning.status !== 'ACTIVE') fail('INVALID_STATE_TRANSITION', 'Only an active warning can be escalated.')
      if (LEVEL_ORDER.indexOf(input.level) <= LEVEL_ORDER.indexOf(warning.level)) {
        fail('BUSINESS_RULE', 'Choose a level higher than the current one.')
      }
      validateText(input)
      // The warning stays the same warning: its level rises, the history records the step and it is sent again.
      const changedAt = new Date().toISOString()
      warning.levelHistory = [
        ...warning.levelHistory,
        { from: warning.level, to: input.level, changedBy: user.id, changedAt },
      ]
      warning.levelChangedAt = changedAt
      Object.assign(warning, {
        level: input.level,
        title: input.title.trim(),
        message: input.message.trim(),
        smsText: input.smsText.trim(),
        instructions: input.instructions.trim(),
      })
      for (let i = db.deliveries.length - 1; i >= 0; i -= 1) {
        if (db.deliveries[i].warningId === warning.id) db.deliveries.splice(i, 1)
      }
      broadcast(warning)
      bus.emit({ type: 'WarningEscalated', warningId: warning.id })
      return warning
    }),

  cancel: (id, reason) =>
    mockCall(() => {
      requireRole('DMC_OFFICER')
      const w = warningRow(id)
      if (w.status !== 'ACTIVE') fail('INVALID_STATE_TRANSITION', 'Only an active warning can be cancelled.')
      if (reason.trim().length < 5) validationFail({ reason: 'Give a reason of at least 5 characters.' })
      w.status = 'CANCELLED'
      w.cancelledAt = new Date().toISOString()
      w.cancelReason = reason.trim()
      bus.emit({ type: 'WarningCancelled', warningId: w.id })
      return w
    }),

  list: (status = 'ALL') =>
    mockCall(() => {
      actor()
      return db.warnings
        .filter((w) => status === 'ALL' || w.status === status)
        .sort((a, b) => b.issuedAt.localeCompare(a.issuedAt))
        .map(warningItem)
    }),

  get: (id) =>
    mockCall(() => {
      actor()
      return warningRow(id)
    }),

  deliveries: (id, filter) =>
    mockCall(() => {
      const w = warningRow(id)
      const all = db.deliveries.filter((d) => d.warningId === id)
      const rows = all
        .filter((d) => !filter.status || filter.status === 'ALL' || d.status === filter.status)
        .filter((d) => !filter.channel || filter.channel === 'ALL' || d.channel === filter.channel)
        .sort((a, b) => a.attemptedAt.localeCompare(b.attemptedAt))
        .map<Delivery>((d) => ({
          id: d.id,
          districtId: d.districtId,
          channel: d.channel,
          status: d.status,
          attemptedAt: d.attemptedAt,
          failureReason: d.failureReason,
        }))
      return { summary: w.deliverySummary, items: paginate(rows, filter.page, filter.size ?? 25) }
    }),

  myAlerts: () =>
    mockCall(() => {
      const user = actor()
      return db.warnings
        .filter(
          (w) =>
            w.status === 'ACTIVE' &&
            (w.districtIds.includes(user.districtId) || (user.riverBasinId !== null && w.riverBasinIds.includes(user.riverBasinId))),
        )
        .sort(
          (a, b) =>
            LEVEL_ORDER.indexOf(b.level) - LEVEL_ORDER.indexOf(a.level) || b.issuedAt.localeCompare(a.issuedAt),
        )
        .map<CitizenAlert>((w) => ({
          id: w.id,
          level: w.level,
          title: w.title,
          message: w.message,
          instructions: w.instructions,
          issuedAt: w.issuedAt,
          districtId: user.districtId,
          riverBasinId: user.riverBasinId,
          audible: SEVERITY[w.level].audible,
        }))
    }),

  simulation: {
    sensors: () => mockCall(() => db.sensors),
    tick: (id) =>
      mockCall(() => {
        requireRole('DMC_OFFICER')
        const s = sensorRow(id)
        return pushReading(s, Math.max(0, s.latest + 0.05 + Math.random() * 0.3))
      }),
    setReading: (id, value) =>
      mockCall(() => {
        requireRole('DMC_OFFICER')
        if (!Number.isFinite(value) || value < 0) validationFail({ value: 'Enter a reading of 0 or more.' })
        return pushReading(sensorRow(id), value)
      }),
    channels: () => mockCall(() => db.channelSettings),
    setChannel: (channel, patch) =>
      mockCall(() => {
        requireRole('DMC_OFFICER')
        const c = db.channelSettings.find((x) => x.channel === channel)!
        Object.assign(c, patch)
        return c
      }),
  },
}
