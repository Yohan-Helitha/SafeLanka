import { http } from '../http'
import type { SectionKey } from '@/types'
import type { AnalyticsApi } from './analyticsApi'

const normalizeInstant = (v: any) => {
  if (!v) return null
  if (typeof v === 'number') return new Date(v > 1e11 ? v : v * 1000).toISOString()
  if (typeof v === 'string' && /^\d+(\.\d+)?$/.test(v)) return new Date(parseFloat(v) > 1e11 ? parseFloat(v) : parseFloat(v) * 1000).toISOString()
  return String(v)
}

const normalizeAlertTimeline = (raw: any) => {
  if (!raw) return null
  return {
    ...raw,
    firstVerifiedReportAt: normalizeInstant(raw.firstVerifiedReportAt),
    firstWarningAt: normalizeInstant(raw.firstWarningAt),
    minutesReportToWarning: raw.reportToWarningMinutes ?? raw.minutesReportToWarning ?? null,
    entries: (raw.entries || []).map((e: any) => ({
      ...e,
      issuedAt: normalizeInstant(e.issuedAt),
      districtIds: e.resolvedDistrictIds || e.districtIds || [],
      isEscalation: e.isEscalation ?? !!e.supersedesId,
      title: e.title || `${e.level} Warning`,
    })),
  }
}

const normalizeCitizensReached = (raw: any) => {
  if (!raw) return null
  return {
    ...raw,
    targeted: raw.uniqueCitizensTargeted ?? raw.targeted ?? 0,
    reached: raw.uniqueCitizensReached ?? raw.reached ?? 0,
    deliveryRate: raw.deliveryRate ?? 0,
    byChannel: Array.isArray(raw.byChannel)
      ? raw.byChannel.reduce((acc: any, curr: any) => ({ ...acc, [curr.channel]: { delivered: curr.delivered, failed: curr.failed } }), {})
      : (raw.byChannel || {}),
    byDistrict: raw.byDistrict || [],
  }
}

const normalizeShelterOccupancy = (raw: any) => {
  if (!raw) return null
  const rawSeries = raw.series || []
  const shelters = rawSeries.map((s: any) => ({
    shelterId: s.shelterId,
    name: s.shelterName,
    capacity: s.capacity,
  }))

  const timeMap = new Map<string, Record<string, number>>()
  rawSeries.forEach((s: any) => {
    (s.points || []).forEach((pt: any) => {
      const at = normalizeInstant(pt.recordedAt)
      if (at) {
        if (!timeMap.has(at)) timeMap.set(at, {})
        timeMap.get(at)![s.shelterId] = pt.occupancy
      }
    })
  })

  const series = Array.from(timeMap.entries())
    .map(([recordedAt, values]) => ({ recordedAt, values }))
    .sort((a, b) => a.recordedAt.localeCompare(b.recordedAt))

  const peaks = (raw.peaks || []).map((p: any) => {
    const s = shelters.find((sh: any) => sh.shelterId === p.shelterId)
    return {
      shelterId: p.shelterId,
      name: p.shelterName || s?.name || p.shelterId,
      peak: p.peakOccupancy ?? p.peak ?? 0,
      capacity: p.capacity ?? s?.capacity ?? 0,
      at: normalizeInstant(p.peakAt),
    }
  })

  return { shelters, series, peaks }
}

const normalizeResourceDistribution = (raw: any) => {
  if (!raw) return null
  return {
    byOrganisationType: raw.byOrganisationType || [],
    byDistrictItem: (raw.byDistrict || []).map((d: any) => ({
      label: `${d.districtName} - ${d.itemCode}`,
      allocated: d.allocated || 0,
      distributed: d.distributed || 0,
    })),
  }
}

/** The API names an unavailable section by its enum name; the screens use the camelCase section keys. */
const SECTION_KEY: Record<string, SectionKey> = {
  ALERT_TIMELINE: 'alertTimeline',
  CITIZENS_REACHED: 'citizensReached',
  SHELTER_OCCUPANCY: 'shelterOccupancy',
  RESOURCE_DISTRIBUTION: 'resourceDistribution',
}

/** One saved report from the API, in the shape the analysis screens expect. */
const toReport = (res: any) => ({
  ...res,
  // the API sends the author as { id, fullName }; the screens show the name
  generatedBy: typeof res.generatedBy === 'string' ? res.generatedBy : (res.generatedBy?.fullName ?? ''),
  alertTimeline: normalizeAlertTimeline(res.sections?.alertTimeline || res.sections?.ALERT_TIMELINE),
  citizensReached: normalizeCitizensReached(res.sections?.citizensReached || res.sections?.CITIZENS_REACHED),
  shelterOccupancy: normalizeShelterOccupancy(res.sections?.shelterOccupancy || res.sections?.SHELTER_OCCUPANCY),
  resourceDistribution: normalizeResourceDistribution(res.sections?.resourceDistribution || res.sections?.RESOURCE_DISTRIBUTION),
  unavailableSections: (res.unavailableSections ?? []).map((u: any) => ({ ...u, key: SECTION_KEY[u.key] ?? u.key })),
})

export const analyticsHttp: AnalyticsApi = {
  // Not /analytics/events: browser ad blockers block that exact path as if it were tracking.
  events: () => http.get<any[]>('/analytics/disaster-events')
    .then(res => res.map(e => ({ ...e, linkedReportCount: e.reportCount }))),

  generate: (eventId, filters) =>
    http.post<any>('/analytics/reports', { eventId, ...filters }).then(toReport),

  list: () => http.page<any>('/analytics/reports')
    .then(p => p.items.map(r => ({ ...r, generatedBy: r.generatedByName }))),

  get: (id) => http.get<any>(`/analytics/reports/${id}`).then(toReport),

  download: (id, format) =>
    http.blob(`/analytics/reports/${id}/export`, { format }),
}
