import type {
  AppUser,
  DisasterEvent,
  District,
  HazardType,
  Organisation,
  OrganisationType,
  ReliefItem,
  RiverBasin,
  Role,
} from '@/types'

export interface ReferenceApi {
  districts(): Promise<District[]>
  riverBasins(): Promise<RiverBasin[]>
  hazardTypes(activeOnly?: boolean): Promise<HazardType[]>
  organisations(type?: OrganisationType): Promise<Organisation[]>
  reliefItems(): Promise<ReliefItem[]>
  events(status?: 'ACTIVE' | 'CLOSED'): Promise<DisasterEvent[]>
  users(role?: Role): Promise<AppUser[]>
}
