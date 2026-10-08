import type { ReportDetail } from '@/types'
import { http } from '../http'
import type { ReportsApi } from './reportsApi'

/**
 * The API serves photos only to the reporter and DMC officers, so an <img src> pointing at it would
 * arrive without the login token. Fetch it with the token and hand the screen a local object URL.
 */
async function withLocalPhoto(detail: ReportDetail): Promise<ReportDetail> {
  if (!detail.photoUrl) return detail
  const blob = await http.blob(detail.photoUrl.replace(/^\/api/, ''))
  return { ...detail, photoUrl: URL.createObjectURL(blob) }
}

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
  get: async (id) => withLocalPhoto(await http.get<ReportDetail>(`/reports/${id}`)),
  verify: async (id, severity) =>
    withLocalPhoto(await http.patch<ReportDetail>(`/reports/${id}/verify`, severity ? { severity } : undefined)),
  reject: async (id, reason, comment) =>
    withLocalPhoto(await http.patch<ReportDetail>(`/reports/${id}/reject`, { reason, comment })),
  requestInfo: async (id, comment) =>
    withLocalPhoto(await http.patch<ReportDetail>(`/reports/${id}/request-info`, { comment })),
}
