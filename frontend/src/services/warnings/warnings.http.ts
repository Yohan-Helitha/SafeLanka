import { http } from '../http'
import type { WarningsApi } from './warningsApi'

export const warningsHttp: WarningsApi = {
  hazards: (filter) => http.get('/hazards', { includeResolved: filter?.includeResolved }),
  hazard: (id) => http.get(`/hazards/${id}`),
  createHazard: (input) => http.post('/hazards', input),
  setHazardStatus: (id, status) => http.patch(`/hazards/${id}/status`, { status }),
  audience: (districtIds, basinIds) =>
    http.get('/warnings/audience', { districtIds, riverBasinIds: basinIds }),
  publish: (input) => http.post('/warnings', input),
  update: (id, input) => http.put(`/warnings/${id}`, input),
  escalate: (id, input) => http.post(`/warnings/${id}/escalate`, input),
  cancel: (id, reason) => http.post(`/warnings/${id}/cancel`, { reason }),
  list: (status) =>
    http.get<any[]>('/warnings', { status: status === 'ALL' ? undefined : status }).catch(() => []),
  get: (id) => http.get(`/warnings/${id}`),
  deliveries: (id, filter) =>
    http.get(`/warnings/${id}/deliveries`, {
      status: filter.status === 'ALL' ? undefined : filter.status,
      channel: filter.channel === 'ALL' ? undefined : filter.channel,
      page: filter.page,
      size: filter.size,
    }),
  myAlerts: () => http.get<any[]>('/warnings/active/mine').catch(() => []),
  simulation: {
    sensors: () => http.get('/simulation/sensors'),
    tick: (id) => http.post(`/simulation/sensors/${id}/tick`),
    setReading: (id, value) => http.post(`/simulation/sensors/${id}/readings`, { value }),
    channels: () => http.get('/simulation/channels'),
    setChannel: (channel, patch) => http.patch(`/simulation/channels/${channel}`, patch),
  },
}
