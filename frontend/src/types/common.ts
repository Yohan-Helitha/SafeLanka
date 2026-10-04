export type Id = string

export type Role =
  | 'CITIZEN'
  | 'VOLUNTEER'
  | 'DMC_OFFICER'
  | 'DISTRICT_OFFICER'
  | 'RESCUE_MEMBER'
  | 'SHELTER_COORDINATOR'

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface LatLng {
  latitude: number
  longitude: number
}
