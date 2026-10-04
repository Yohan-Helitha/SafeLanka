import { http } from '../http'
import type { AnalyticsApi } from './analyticsApi'

export const analyticsHttp: AnalyticsApi = {
  events: () => http.get('/analytics/events'),
  generate: (eventId, filters) => http.post('/analytics/reports', { eventId, ...filters }),
  list: () => http.get('/analytics/reports'),
  get: (id) => http.get(`/analytics/reports/${id}`),
  download: (id, format) => http.blob(`/analytics/reports/${id}/export`, { format }),
}
