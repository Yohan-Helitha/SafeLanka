import type { AppUser, DisasterEvent, District, HazardType, Organisation, ReliefItem, RiverBasin } from '@/types'
import { db } from '../mocks/db'
import { http } from '../http'
import type { ReferenceApi } from './referenceApi'

export const referenceHttp: ReferenceApi = {
  districts: () => http.get<District[]>('/reference/districts').catch(() => db.districts),
  riverBasins: () => http.get<RiverBasin[]>('/reference/river-basins').catch(() => db.riverBasins),
  hazardTypes: (activeOnly = false) =>
    http.get<HazardType[]>('/reference/hazard-types', { activeOnly }).catch(() => db.hazardTypes.filter((h) => !activeOnly || h.active)),
  organisations: (type) =>
    http.get<Organisation[]>('/reference/organisations', { type }).catch(() => db.organisations.filter((o) => !type || o.type === type)),
  reliefItems: () => http.get<ReliefItem[]>('/reference/relief-items').catch(() => db.reliefItems),
  events: (status) =>
    http.get<DisasterEvent[]>('/reference/events', { status }).catch(() => db.events.filter((e) => !status || e.status === status)),
  users: (role) => http.get<AppUser[]>('/reference/users', { role }).catch(() => db.users.filter((u) => !role || u.role === role)),
}
