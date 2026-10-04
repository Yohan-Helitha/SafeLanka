import { http } from '../http'
import type { ReferenceApi } from './referenceApi'

export const referenceHttp: ReferenceApi = {
  districts: () => http.get('/reference/districts'),
  riverBasins: () => http.get('/reference/river-basins'),
  hazardTypes: (activeOnly = false) => http.get('/reference/hazard-types', { activeOnly }),
  organisations: (type) => http.get('/reference/organisations', { type }),
  reliefItems: () => http.get('/reference/relief-items'),
  events: (status) => http.get('/reference/events', { status }),
  users: (role) => http.get('/reference/users', { role }),
}
