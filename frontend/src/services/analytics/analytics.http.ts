import type { AnalyticsEvent, DisasterReport, ReportSummary } from '@/types'
import { http } from '../http'
import { analyticsMock } from './analytics.mock'
import type { AnalyticsApi } from './analyticsApi'

export const analyticsHttp: AnalyticsApi = {
  events: () => http.get<AnalyticsEvent[]>('/analytics/events').catch(() => analyticsMock.events()),
  generate: (eventId, filters) =>
    http.post<DisasterReport>('/analytics/reports', { eventId, ...filters }).catch(() => analyticsMock.generate(eventId, filters)),
  list: () => http.get<ReportSummary[]>('/analytics/reports').catch(() => analyticsMock.list()),
  get: (id) => http.get<DisasterReport>(`/analytics/reports/${id}`).catch(() => analyticsMock.get(id)),
  download: (id, format) =>
    http.blob(`/analytics/reports/${id}/export`, { format }).catch(() => analyticsMock.download(id, format)),
}
