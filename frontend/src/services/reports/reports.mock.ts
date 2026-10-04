import { LIMITS } from '@/constants/app'
import { THRESHOLDS } from '@/theme/tokens'
import type { RejectionReason, ReportDetail, ReportListItem } from '@/types'
import { haversineMetres } from '@/utils/geo'
import { actor, requireRole } from '../mocks/actor'
import { bus } from '../mocks/bus'
import { db, userById } from '../mocks/db'
import type { ReportRow } from '../mocks/db'
import { newId } from '../mocks/ids'
import { fail, mockCall, notFound, paginate, validationFail } from '../mocks/mockCall'
import type { ReportsApi } from './reportsApi'

function duplicatesOf(row: ReportRow) {
  if (row.latitude === null || row.longitude === null) return []
  const here = { latitude: row.latitude, longitude: row.longitude }
  const windowMs = THRESHOLDS.duplicateHours * 3_600_000
  return db.reports
    .filter((r) => r.id !== row.id && r.hazardTypeId === row.hazardTypeId && r.latitude !== null && r.longitude !== null)
    .filter((r) => Math.abs(new Date(r.capturedAt).getTime() - new Date(row.capturedAt).getTime()) <= windowMs)
    .map((r) => ({
      id: r.id,
      referenceNo: r.referenceNo,
      distanceMetres: Math.round(haversineMetres(here, { latitude: r.latitude!, longitude: r.longitude! })),
    }))
    .filter((d) => d.distanceMetres <= THRESHOLDS.duplicateMetres)
}

function toListItem(row: ReportRow): ReportListItem {
  return {
    id: row.id,
    referenceNo: row.referenceNo,
    hazardTypeId: row.hazardTypeId,
    category: row.category,
    description: row.description,
    districtId: row.districtId,
    status: row.status,
    capturedAt: row.capturedAt,
    syncedAt: row.syncedAt,
    hasPhoto: row.photoUrl !== null,
    hasDuplicates: duplicatesOf(row).length > 0,
    rejectionReason: row.rejectionReason,
    reviewComment: row.reviewComment,
  }
}

function toDetail(row: ReportRow): ReportDetail {
  const reporter = userById(row.reporterId)
  return {
    ...toListItem(row),
    latitude: row.latitude ?? undefined,
    longitude: row.longitude ?? undefined,
    isManualLocation: row.isManualLocation,
    manualLocationText: row.manualLocationText,
    photoUrl: row.photoUrl,
    reporter: { id: row.reporterId, fullName: reporter?.fullName ?? 'Unknown', role: reporter?.role ?? 'CITIZEN' },
    reviewedBy: row.reviewedBy ? (userById(row.reviewedBy)?.fullName ?? null) : null,
    reviewedAt: row.reviewedAt,
    possibleDuplicates: duplicatesOf(row),
  }
}

function find(id: string): ReportRow {
  return db.reports.find((r) => r.id === id) ?? notFound('Report')
}

/** Reviewer rules: DMC officer only, never on their own report, only from an open state. */
function reviewable(id: string): ReportRow {
  const user = requireRole('DMC_OFFICER')
  const row = find(id)
  if (row.reporterId === user.id) fail('BUSINESS_RULE', 'You cannot review your own report.')
  if (row.status !== 'PENDING' && row.status !== 'NEEDS_MORE_INFO') {
    fail('INVALID_STATE_TRANSITION', `This report is already ${row.status.toLowerCase().replace(/_/g, ' ')}.`)
  }
  return row
}

function stamp(row: ReportRow, status: ReportRow['status']) {
  row.status = status
  row.reviewedBy = actor().id
  row.reviewedAt = new Date().toISOString()
}

export const reportsMock: ReportsApi = {
  submit: (input) =>
    mockCall(() => {
      const user = actor()
      const existing = db.reports.find((r) => r.clientRef === input.clientRef)
      if (existing) return toListItem(existing) // idempotent retry

      const type = db.hazardTypes.find((t) => t.id === input.hazardTypeId)
      const errors: Record<string, string> = {}
      if (!type || !type.active) errors.hazardTypeId = 'Choose a hazard type that is open for reporting.'
      else if (!type.reportCategories.includes(input.category)) errors.category = 'This does not match the hazard type.'
      const d = input.description.trim().length
      if (d < LIMITS.description.min || d > LIMITS.description.max) {
        errors.description = `Description must be ${LIMITS.description.min}–${LIMITS.description.max} characters.`
      }
      const hasGps = input.latitude !== null && input.longitude !== null
      if (!hasGps && !(input.isManualLocation && (input.manualLocationText?.trim().length ?? 0) >= 5)) {
        errors.location = 'Add a location: use GPS or describe the place.'
      }
      if (input.photo && input.photo.size > LIMITS.photoBytes) errors.photo = 'The photo is larger than 5 MB.'
      if (Object.keys(errors).length) validationFail(errors)

      const row: ReportRow = {
        id: newId(),
        referenceNo: `RPT-2026-${String(db.nextReportNo++).padStart(4, '0')}`,
        reporterId: user.id,
        hazardTypeId: input.hazardTypeId,
        category: input.category,
        description: input.description.trim(),
        latitude: input.latitude,
        longitude: input.longitude,
        isManualLocation: input.isManualLocation,
        manualLocationText: input.manualLocationText,
        districtId: input.districtId,
        status: 'PENDING',
        clientRef: input.clientRef,
        capturedAt: input.capturedAt,
        syncedAt: new Date().toISOString(),
        photoUrl: input.photo ? URL.createObjectURL(input.photo) : null,
        reviewedBy: null,
        reviewedAt: null,
        rejectionReason: null,
        reviewComment: null,
      }
      db.reports.push(row)
      return toListItem(row)
    }),

  mine: () =>
    mockCall(() => {
      const user = actor()
      return db.reports
        .filter((r) => r.reporterId === user.id)
        .sort((a, b) => b.capturedAt.localeCompare(a.capturedAt))
        .map(toListItem)
    }),

  search: (filter) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      const status = filter.status ?? 'PENDING'
      const rows = db.reports
        .filter((r) => status === 'ALL' || r.status === status)
        .filter((r) => !filter.hazardTypeId || r.hazardTypeId === filter.hazardTypeId)
        .filter((r) => !filter.districtId || r.districtId === filter.districtId)
        .sort((a, b) => a.capturedAt.localeCompare(b.capturedAt))
        .map(toListItem)
      return paginate(rows, filter.page, filter.size ?? 50)
    }),

  get: (id) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return toDetail(find(id))
    }),

  verify: (id) =>
    mockCall(() => {
      const row = reviewable(id)
      stamp(row, 'VERIFIED')
      row.rejectionReason = null
      bus.emit({ type: 'ReportVerified', reportId: row.id })
      return toDetail(row)
    }),

  reject: (id, reason: RejectionReason, comment) =>
    mockCall(() => {
      const row = reviewable(id)
      if (reason === 'OTHER' && comment.trim().length < LIMITS.comment.min) {
        validationFail({ comment: 'Explain why the report is rejected.' })
      }
      stamp(row, 'REJECTED')
      row.rejectionReason = reason
      row.reviewComment = comment.trim() || null
      return toDetail(row)
    }),

  requestInfo: (id, comment) =>
    mockCall(() => {
      const row = reviewable(id)
      if (row.status !== 'PENDING') fail('INVALID_STATE_TRANSITION', 'More information was already requested.')
      const n = comment.trim().length
      if (n < LIMITS.comment.min || n > LIMITS.comment.max) {
        validationFail({ comment: `Comment must be ${LIMITS.comment.min}–${LIMITS.comment.max} characters.` })
      }
      stamp(row, 'NEEDS_MORE_INFO')
      row.reviewComment = comment.trim()
      return toDetail(row)
    }),
}
