import { http } from '../http'
import type { ResponseApi } from './responseApi'

export const responseHttp: ResponseApi = {
  dashboard: (districtId) => http.get('/response/dashboard', { districtId }),
  teams: (filter) => http.get('/rescue-teams', { ...filter }),
  createAssignment: (input) => http.post('/assignments', input),
  assign: (id, teamId) => http.post(`/assignments/${id}/assign`, { teamId }),
  cancelAssignment: (id) => http.post(`/assignments/${id}/cancel`),
  assignments: (filter) => http.get('/assignments', { ...filter }),
  assignment: (id) => http.get(`/assignments/${id}`),
  myAssignment: () => http.get('/assignments/mine'),
  respond: (id, accept, declineReason) =>
    http.post(`/assignments/${id}/respond`, { accept, declineReason }),
  updateTeamStatus: (teamId, update) => http.post(`/rescue-teams/${teamId}/status`, update),
  shelters: (filter) => http.get('/shelters', { ...filter }),
  shelterSuggestions: (latitude, longitude) =>
    http.get('/shelters/suggestions', { latitude, longitude }),
  updateOccupancy: (id, occupancy) => http.patch(`/shelters/${id}/occupancy`, { occupancy }),
  headcountUpdates: (filter) => http.get('/shelters/headcount-updates', { ...filter }),
  applyHeadcountUpdate: (id, customOccupancy) =>
    http.post(`/shelters/headcount-updates/${id}/apply`, customOccupancy != null ? { customOccupancy } : {}),
  dismissHeadcountUpdate: (id) => http.post(`/shelters/headcount-updates/${id}/dismiss`),
  createHeadcountUpdate: (input) => http.post('/shelters/headcount-updates', input),
  stocks: (filter) => http.get('/relief-stocks', { ...filter }),
  allocate: (input) => http.post('/allocations', input),
  recordDistribution: (id, input) => http.post(`/allocations/${id}/distributions`, input),
  allocations: (filter) => http.get('/allocations', { ...filter }),
}

