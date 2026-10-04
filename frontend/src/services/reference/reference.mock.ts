import { db } from '../mocks/db'
import { mockCall } from '../mocks/mockCall'
import type { ReferenceApi } from './referenceApi'

export const referenceMock: ReferenceApi = {
  districts: () => mockCall(() => db.districts),
  riverBasins: () => mockCall(() => db.riverBasins),
  hazardTypes: (activeOnly = false) =>
    mockCall(() => db.hazardTypes.filter((h) => !activeOnly || h.active)),
  organisations: (type) => mockCall(() => db.organisations.filter((o) => !type || o.type === type)),
  reliefItems: () => mockCall(() => db.reliefItems),
  events: (status) => mockCall(() => db.events.filter((e) => !status || e.status === status)),
  users: (role) => mockCall(() => db.users.filter((u) => !role || u.role === role)),
}
