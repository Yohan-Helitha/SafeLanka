import { http } from '../http'
import type { ReportsApi } from './reportsApi'

export const reportsHttp: ReportsApi = {
  submit(input) {
    const { photo, ...report } = input
    const form = new FormData()
    form.append('report', new Blob([JSON.stringify(report)], { type: 'application/json' }))
    if (photo) form.append('photo', photo)
    return http.postForm('/reports', form)
  },
  mine: () => http.get('/reports/mine'),
  search: (filter) =>
    http.page('/reports', {
      status: filter.status === 'ALL' ? undefined : filter.status,
      hazardTypeId: filter.hazardTypeId,
      districtId: filter.districtId,
      page: filter.page,
      size: filter.size,
    }),
  get: (id) => http.get(`/reports/${id}`),
  verify: (id) => http.patch(`/reports/${id}/verify`),
  reject: (id, reason, comment) => http.patch(`/reports/${id}/reject`, { reason, comment }),
  requestInfo: (id, comment) => http.patch(`/reports/${id}/request-info`, { comment }),
}
