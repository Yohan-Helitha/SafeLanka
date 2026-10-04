import type { Role } from '@/types'
import { paths } from './routes'

export const ROLE_LABEL: Record<Role, string> = {
  CITIZEN: 'Citizen',
  VOLUNTEER: 'Volunteer',
  DMC_OFFICER: 'DMC duty officer',
  DISTRICT_OFFICER: 'District officer',
  RESCUE_MEMBER: 'Rescue team member',
  SHELTER_COORDINATOR: 'Shelter coordinator',
}

/** Order in which roles are grouped on the landing screen and in the switcher. */
export const ROLE_ORDER: Role[] = [
  'DMC_OFFICER',
  'DISTRICT_OFFICER',
  'CITIZEN',
  'VOLUNTEER',
  'RESCUE_MEMBER',
  'SHELTER_COORDINATOR',
]

export type Surface = 'portal' | 'field'

export const ROLE_SURFACE: Record<Role, Surface> = {
  CITIZEN: 'field',
  VOLUNTEER: 'field',
  RESCUE_MEMBER: 'field',
  SHELTER_COORDINATOR: 'field',
  DMC_OFFICER: 'portal',
  DISTRICT_OFFICER: 'portal',
}

const HOME: Record<Role, string> = {
  CITIZEN: paths.citizen.home,
  VOLUNTEER: paths.citizen.home,
  RESCUE_MEMBER: paths.team,
  SHELTER_COORDINATOR: paths.coordinator,
  DMC_OFFICER: paths.dmc.hazards,
  DISTRICT_OFFICER: paths.district.home,
}

export function homeFor(role: Role): string {
  return HOME[role]
}
