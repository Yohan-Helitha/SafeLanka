import type { Id, Role } from './common'

export interface District {
  id: Id
  code: string
  name: string
  province: string
}

export interface RiverBasin {
  id: Id
  code: string
  name: string
  districtIds: Id[]
}

export interface HazardType {
  id: Id
  code: string
  name: string
  onsetSpeed: 'RAPID' | 'SLOW'
  reportCategories: string[]
  active: boolean
}

export type OrganisationType = 'GOVERNMENT' | 'ARMED_FORCES' | 'POLICE' | 'NGO' | 'PRIVATE_DONOR'

export interface Organisation {
  id: Id
  name: string
  type: OrganisationType
}

export interface ReliefItem {
  id: Id
  code: string
  name: string
  unit: string
  category: 'FOOD' | 'WATER' | 'MEDICINE' | 'HYGIENE' | 'SHELTER_KIT'
}

export interface DisasterEvent {
  id: Id
  name: string
  hazardTypeId: Id
  status: 'ACTIVE' | 'CLOSED'
  startedAt: string
  endedAt: string | null
  districtIds: Id[]
}

export interface AppUser {
  id: Id
  role: Role
  fullName: string
  districtId: Id
  riverBasinId: Id | null
  organisationId: Id | null
  rescueTeamId: Id | null
  preferredLanguage: 'en' | 'si' | 'ta'
  /** Named seed users appear on the landing screen; the 40 demo residents do not. */
  named: boolean
}
