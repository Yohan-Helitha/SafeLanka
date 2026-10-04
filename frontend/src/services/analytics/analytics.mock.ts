import type {
  AlertTimelineSection,
  AnalyticsEvent,
  Channel,
  CitizensReachedSection,
  DisasterReport,
  ResourceDistributionSection,
  SectionKey,
  ShelterOccupancySection,
} from '@/types'
import { requireRole } from '../mocks/actor'
import { db, districtName, userById } from '../mocks/db'
import { buildCsv, buildPdf } from '../mocks/exporters'
import { newId } from '../mocks/ids'
import { fail, mockCall, notFound, validationFail } from '../mocks/mockCall'
import type { AnalyticsApi } from './analyticsApi'

const NO_DATA: Record<SectionKey, string> = {
  alertTimeline: 'No warnings were issued for this event in the selected period.',
  citizensReached: 'No notifications were sent for this event in the selected period.',
  shelterOccupancy: 'No shelter headcounts were recorded for this event in the selected period.',
  resourceDistribution: 'No relief was allocated for this event in the selected period.',
}

function eventHazardIds(eventId: string): string[] {
  return db.hazards.filter((h) => h.eventId === eventId).map((h) => h.id)
}

export const analyticsMock: AnalyticsApi = {
  events: () =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return db.events.map<AnalyticsEvent>((e) => {
        const hazardIds = eventHazardIds(e.id)
        return {
          ...e,
          warningCount: db.warnings.filter((w) => w.eventId === e.id).length,
          linkedReportCount: db.hazards.filter((h) => hazardIds.includes(h.id)).reduce((n, h) => n + h.evidence.length, 0),
        }
      })
    }),

  generate: (eventId, filters) =>
    mockCall(() => {
      const user = requireRole('DMC_OFFICER')
      const event = db.events.find((e) => e.id === eventId) ?? notFound('Event')
      if (filters.from && filters.to && filters.from > filters.to) {
        validationFail({ from: 'The start must be before the end.' })
      }
      const districtIds = filters.districtIds.length ? filters.districtIds : event.districtIds
      const from = filters.from ? new Date(filters.from).getTime() : -Infinity
      const to = filters.to ? new Date(filters.to).getTime() : Infinity
      const inWindow = (iso: string) => new Date(iso).getTime() >= from && new Date(iso).getTime() <= to

      const warnings = db.warnings
        .filter((w) => w.eventId === eventId && inWindow(w.issuedAt) && w.districtIds.some((d) => districtIds.includes(d)))
        .sort((a, b) => a.issuedAt.localeCompare(b.issuedAt))
      const unavailable: DisasterReport['unavailableSections'] = []

      // 1. Alert timeline
      let alertTimeline: AlertTimelineSection | null = null
      if (warnings.length) {
        const hazardIds = eventHazardIds(eventId)
        const verified = db.hazards
          .filter((h) => hazardIds.includes(h.id))
          .flatMap((h) => h.evidence)
          .map((e) => db.reports.find((r) => r.id === e.reportId)?.reviewedAt)
          .filter((x): x is string => Boolean(x))
          .sort()
        const first = warnings[0].issuedAt
        alertTimeline = {
          firstVerifiedReportAt: verified[0] ?? null,
          firstWarningAt: first,
          minutesReportToWarning: verified[0] ? Math.max(0, Math.round((new Date(first).getTime() - new Date(verified[0]).getTime()) / 60000)) : null,
          entries: warnings.map((w) => ({
            warningId: w.id,
            level: w.level,
            status: w.status,
            title: w.title,
            issuedAt: w.issuedAt,
            districtIds: w.districtIds.filter((d) => districtIds.includes(d)),
            isEscalation: w.supersedesId !== null,
          })),
        }
      } else unavailable.push({ key: 'alertTimeline', reason: NO_DATA.alertTimeline })

      // 2. Citizens reached
      let citizensReached: CitizensReachedSection | null = null
      const rows = db.deliveries.filter((d) => warnings.some((w) => w.id === d.warningId) && districtIds.includes(d.districtId))
      if (rows.length) {
        const targeted = new Set(rows.map((r) => r.citizenId))
        const reached = new Set(rows.filter((r) => r.status === 'DELIVERED').map((r) => r.citizenId))
        const byChannel = { PUSH: { delivered: 0, failed: 0 }, SMS: { delivered: 0, failed: 0 }, AUDIBLE: { delivered: 0, failed: 0 } } as Record<Channel, { delivered: number; failed: number }>
        rows.forEach((r) => {
          if (r.status === 'DELIVERED') byChannel[r.channel].delivered += 1
          else if (r.status === 'FAILED') byChannel[r.channel].failed += 1
        })
        citizensReached = {
          targeted: targeted.size,
          reached: reached.size,
          deliveryRate: reached.size / targeted.size,
          byChannel,
          byDistrict: districtIds
            .map((districtId) => {
              const mine = rows.filter((r) => r.districtId === districtId)
              return {
                districtId,
                targeted: new Set(mine.map((r) => r.citizenId)).size,
                reached: new Set(mine.filter((r) => r.status === 'DELIVERED').map((r) => r.citizenId)).size,
              }
            })
            .filter((d) => d.targeted > 0),
        }
      } else unavailable.push({ key: 'citizensReached', reason: NO_DATA.citizensReached })

      // 3. Shelter occupancy over time
      let shelterOccupancy: ShelterOccupancySection | null = null
      const logs = db.occupancyLogs
        .filter((l) => l.eventId === eventId && inWindow(l.recordedAt))
        .filter((l) => districtIds.includes(db.shelters.find((s) => s.id === l.shelterId)!.districtId))
        .sort((a, b) => a.recordedAt.localeCompare(b.recordedAt))
      if (logs.length) {
        const shelterIds = [...new Set(logs.map((l) => l.shelterId))]
        const shelters = shelterIds.map((id) => {
          const s = db.shelters.find((x) => x.id === id)!
          return { shelterId: id, name: s.name, capacity: s.capacity }
        })
        const last: Record<string, number> = {}
        const series = [...new Set(logs.map((l) => l.recordedAt))].map((recordedAt) => {
          logs.filter((l) => l.recordedAt === recordedAt).forEach((l) => (last[l.shelterId] = l.occupancy))
          return { recordedAt, values: { ...last } }
        })
        shelterOccupancy = {
          shelters,
          series,
          peaks: shelters.map((s) => {
            const mine = logs.filter((l) => l.shelterId === s.shelterId)
            const top = mine.reduce((a, b) => (b.occupancy > a.occupancy ? b : a))
            return { shelterId: s.shelterId, name: s.name, peak: top.occupancy, capacity: s.capacity, at: top.recordedAt }
          }),
        }
      } else unavailable.push({ key: 'shelterOccupancy', reason: NO_DATA.shelterOccupancy })

      // 4. Resource distribution
      let resourceDistribution: ResourceDistributionSection | null = null
      const allocations = db.allocations.filter(
        (a) =>
          a.eventId === eventId &&
          inWindow(a.allocatedAt) &&
          districtIds.includes(db.shelters.find((s) => s.id === a.shelterId)!.districtId),
      )
      if (allocations.length) {
        const byLabel = new Map<string, { allocated: number; distributed: number }>()
        const byOrg = new Map<string, number>()
        allocations.forEach((a) => {
          const stock = db.stocks.find((s) => s.id === a.stockId)!
          const item = db.reliefItems.find((i) => i.id === stock.itemId)!
          const shelter = db.shelters.find((s) => s.id === a.shelterId)!
          const label = `${districtName(shelter.districtId)} · ${item.name}`
          const cur = byLabel.get(label) ?? { allocated: 0, distributed: 0 }
          cur.allocated += a.quantity
          cur.distributed += a.distributed
          byLabel.set(label, cur)
          const type = db.organisations.find((o) => o.id === stock.organisationId)!.type
          byOrg.set(type, (byOrg.get(type) ?? 0) + a.distributed)
        })
        resourceDistribution = {
          byDistrictItem: [...byLabel].map(([label, v]) => ({ label, ...v })),
          byOrganisationType: [...byOrg].map(([type, distributed]) => ({ type, distributed })),
        }
      } else unavailable.push({ key: 'resourceDistribution', reason: NO_DATA.resourceDistribution })

      const report: DisasterReport = {
        id: newId(),
        eventId,
        eventName: event.name,
        filters: { districtIds, from: filters.from, to: filters.to },
        generatedBy: userById(user.id)!.fullName,
        generatedAt: new Date().toISOString(),
        alertTimeline,
        citizensReached,
        shelterOccupancy,
        resourceDistribution,
        unavailableSections: unavailable,
      }
      db.analyticsReports.push(report)
      return report
    }),

  list: () =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return [...db.analyticsReports]
        .sort((a, b) => b.generatedAt.localeCompare(a.generatedAt))
        .map((r) => ({
          id: r.id,
          eventName: r.eventName,
          generatedAt: r.generatedAt,
          generatedBy: r.generatedBy,
          unavailableCount: r.unavailableSections.length,
        }))
    }),

  get: (id) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      return db.analyticsReports.find((r) => r.id === id) ?? notFound('Report')
    }),

  download: (id, format) =>
    mockCall(() => {
      requireRole('DMC_OFFICER', 'DISTRICT_OFFICER')
      const report = db.analyticsReports.find((r) => r.id === id)
      if (!report) fail('NOT_FOUND', 'Report was not found.')
      return format === 'CSV' ? buildCsv(report) : buildPdf(report)
    }),
}
